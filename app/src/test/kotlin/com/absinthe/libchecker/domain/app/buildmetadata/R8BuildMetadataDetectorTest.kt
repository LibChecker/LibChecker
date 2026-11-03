package com.absinthe.libchecker.domain.app.buildmetadata

import com.absinthe.libchecker.compat.ZipFileCompat
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore
import com.android.tools.smali.dexlib2.writer.pool.DexPool
import com.android.tools.smali.util.Utf8Utils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class R8BuildMetadataDetectorTest {
  @get:Rule
  val temporaryFolder = TemporaryFolder()

  @Test
  fun malformedStringLengthDoesNotGrowDecoderThreadCache() {
    val data = ByteArray(128)
    ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).apply {
      put(byteArrayOf(100, 101, 120, 10, 48, 51, 53, 0))
      putInt(32, 128)
      putInt(36, 112)
      putInt(40, 0x12345678)
      putInt(52, 124)
      putInt(56, 1)
      putInt(60, 112)
      putInt(112, 116)
    }
    var length = 1_000_000
    var offset = 116
    do {
      data[offset++] = ((length and 127) or if (length > 127) 128 else 0).toByte()
      length = length ushr 7
    } while (length != 0)
    val task = FutureTask {
      assertEquals(emptyList<String>(), R8BuildMetadataDetector.detect(DexBackedDexFile(null, data)))
      val field = Utf8Utils::class.java.getDeclaredField("localBuffer").apply { isAccessible = true }
      ((field.get(null) as ThreadLocal<*>).get() as? CharArray)?.size ?: 0
    }
    Thread(task).start()
    assertTrue("Malformed lengths must not allocate a large decoder cache", task.get(5, TimeUnit.SECONDS) <= 16_384)
  }

  @Test
  fun longUnrelatedStringsAndOversizedMarkersDoNotHideValidMarkers() {
    val pool = DexPool(Opcodes.getDefault())
    listOf(
      "a".repeat(100_000),
      "~~R8{\"padding\":\"${"x".repeat(20_000)}\",\"version\":\"8.0.1\"}",
      "~~R8{\"version\":\"9.5.28\"}",
      "界".repeat(100_000)
    ).forEach(pool.stringSection::intern)
    val store = MemoryDataStore()
    pool.writeTo(store)
    assertEquals(listOf("9.5.28"), R8BuildMetadataDetector.detect(DexBackedDexFile(null, store.data)))
  }

  @Test
  fun readsUnreferencedStringsAcrossDexFilesAndDeduplicates() {
    val apk = temporaryFolder.newFile("markers.apk")
    ZipOutputStream(apk.outputStream()).use { zip ->
      listOf("full", "full", "compatibility").forEachIndexed { index, mode ->
        val pool = DexPool(Opcodes.getDefault())
        pool.stringSection.intern("~~R8{\"version\":\"9.5.28\",\"r8-mode\":\"$mode\"}")
        val store = MemoryDataStore()
        pool.writeTo(store)
        zip.putNextEntry(ZipEntry(if (index == 0) "classes.dex" else "classes${index + 1}.dex"))
        zip.write(store.data)
        zip.closeEntry()
      }
    }
    ZipFileCompat(apk).use { zip ->
      assertEquals(listOf("9.5.28 (Full)", "9.5.28 (Compatibility)"), R8BuildMetadataDetector.detect(apk, zip))
    }
  }

  @Test
  fun prefixSearchKeepsAllMarkersAndHandlesPoolBoundaries() {
    val first = "~~R8{\"version\":\"8.0.1\"}"
    val second = "~~R8{\"version\":\"9.5.28\",\"r8-mode\":\"full\"}"
    val surrounding = listOf("", "ordinary", "~~D8{}", "~~R8", "~~R8Partial{}", "~~R8z", "~~R8|", "~~~")
    for (strings in listOf(emptyList(), surrounding, listOf(first), listOf(second), surrounding + first + second)) {
      val pool = DexPool(Opcodes.getDefault())
      strings.forEach(pool.stringSection::intern)
      val store = MemoryDataStore()
      pool.writeTo(store)
      val dex = DexBackedDexFile(Opcodes.getDefault(), store.data)
      val expected = strings.sorted().mapNotNull(R8BuildMetadataDetector::parseMarker).distinct()
      assertEquals(expected, R8BuildMetadataDetector.detect(dex))
    }
  }

  @Test
  fun acceptsOlderMarkersWithoutModeButRejectsInvalidVersionsAndOtherTools() {
    assertEquals("8.0.1-dev", R8BuildMetadataDetector.parseMarker("~~R8{\"version\":\"8.0.1-dev\"}"))
    listOf(
      "~~D8{\"version\":\"9.5.28\"}",
      "~~R8{broken}",
      "~~R8{\"version\":123}",
      "~~R8{\"version\":\"<b>9.5.28</b>\"}",
      "~~R8{}"
    ).forEach { assertEquals(null, R8BuildMetadataDetector.parseMarker(it)) }
  }
}
