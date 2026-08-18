package com.absinthe.libchecker.utils

import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import com.android.tools.smali.dexlib2.immutable.value.ImmutableStringEncodedValue
import com.android.tools.smali.dexlib2.writer.pool.DexPool
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PackageUtilsKotlinDetectionTest {

  @get:Rule
  val temporaryFolder = TemporaryFolder()

  @Test
  fun detectsKotlinRuntimeMarkersAcrossMultipleDexEntries() {
    val apk = createApk(
      createDex(KOTLIN_RUNTIME_MARKERS[0]),
      createDex(KOTLIN_RUNTIME_MARKERS[1])
    )

    assertTrue(PackageUtils.hasKotlinRuntimeEvidenceInClassDex(apk))
  }

  @Test
  fun ignoresAStandaloneKotlinLookingString() {
    val apk = createApk(createDex(KOTLIN_RUNTIME_MARKERS[0]))

    assertFalse(PackageUtils.hasKotlinRuntimeEvidenceInClassDex(apk))
  }

  @Test
  fun ignoresMarkersBeyondTheRuntimeFallbackScanBudget() {
    val apk = createApk(createDexWithLateMarkers())

    assertFalse(PackageUtils.hasKotlinRuntimeEvidenceInClassDex(apk))
  }

  private fun createDex(marker: String): ByteArray {
    val dexFile = temporaryFolder.newFile()
    val field = ImmutableField(
      "La;",
      "a",
      "Ljava/lang/String;",
      STATIC_FINAL_ACCESS_FLAGS,
      ImmutableStringEncodedValue(marker),
      null,
      null
    )
    val classDef = ImmutableClassDef(
      "La;",
      0x1,
      "Ljava/lang/Object;",
      null,
      null,
      null,
      listOf(field),
      null
    )
    DexPool.writeTo(dexFile.path, ImmutableDexFile(Opcodes.getDefault(), listOf(classDef)))
    return dexFile.readBytes()
  }

  private fun createApk(vararg dexFiles: ByteArray): File {
    return temporaryFolder.newFile("fixture-${dexFiles.size}.apk").apply {
      outputStream().use { output ->
        ZipOutputStream(output).use { zip ->
          dexFiles.forEachIndexed { index, dex ->
            val name = if (index == 0) "classes.dex" else "classes${index + 1}.dex"
            zip.putNextEntry(ZipEntry(name))
            zip.write(dex)
            zip.closeEntry()
          }
        }
      }
    }
  }

  private fun createDexWithLateMarkers(): ByteArray {
    val firstMarkerOffset = MAX_RUNTIME_FALLBACK_SCAN_BYTES + 1
    val firstMarker = KOTLIN_RUNTIME_MARKERS[0].toByteArray()
    val secondMarker = KOTLIN_RUNTIME_MARKERS[1].toByteArray()
    val secondMarkerOffset = firstMarkerOffset + firstMarker.size + 2
    val dex = ByteArray(secondMarkerOffset + secondMarker.size + 2)
    "dex\n035\u0000".toByteArray().copyInto(dex)
    dex.writeIntLe(FILE_SIZE_OFFSET, dex.size)
    dex.writeIntLe(HEADER_SIZE_OFFSET, DEX_HEADER_SIZE)
    dex.writeIntLe(ENDIAN_TAG_OFFSET, ENDIAN_CONSTANT)
    dex.writeIntLe(STRING_IDS_SIZE_OFFSET, KOTLIN_RUNTIME_MARKERS.size)
    dex.writeIntLe(STRING_IDS_OFFSET_OFFSET, DEX_HEADER_SIZE)
    dex.writeIntLe(DEX_HEADER_SIZE, firstMarkerOffset)
    dex.writeIntLe(DEX_HEADER_SIZE + UINT_SIZE, secondMarkerOffset)
    dex[firstMarkerOffset] = firstMarker.size.toByte()
    firstMarker.copyInto(dex, firstMarkerOffset + 1)
    dex[secondMarkerOffset] = secondMarker.size.toByte()
    secondMarker.copyInto(dex, secondMarkerOffset + 1)
    return dex
  }

  private fun ByteArray.writeIntLe(offset: Int, value: Int) {
    this[offset] = value.toByte()
    this[offset + 1] = (value ushr 8).toByte()
    this[offset + 2] = (value ushr 16).toByte()
    this[offset + 3] = (value ushr 24).toByte()
  }

  private companion object {
    const val STATIC_FINAL_ACCESS_FLAGS = 0x18
    const val DEX_HEADER_SIZE = 0x70
    const val FILE_SIZE_OFFSET = 0x20
    const val HEADER_SIZE_OFFSET = 0x24
    const val ENDIAN_TAG_OFFSET = 0x28
    const val STRING_IDS_SIZE_OFFSET = 0x38
    const val STRING_IDS_OFFSET_OFFSET = 0x3c
    const val ENDIAN_CONSTANT = 0x12345678
    const val UINT_SIZE = 4
    const val MAX_RUNTIME_FALLBACK_SCAN_BYTES = 32 * 1024 * 1024
    val KOTLIN_RUNTIME_MARKERS = listOf(
      "kotlin.jvm.functions.Function1",
      "kotlin.coroutines.jvm.internal.BaseContinuationImpl"
    )
  }
}
