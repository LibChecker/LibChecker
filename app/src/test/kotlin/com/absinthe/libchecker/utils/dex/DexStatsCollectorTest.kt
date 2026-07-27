package com.absinthe.libchecker.utils.dex

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile
import com.android.tools.smali.dexlib2.writer.pool.DexPool
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DexStatsCollectorTest {

  @get:Rule
  val temporaryFolder = TemporaryFolder()

  @Test
  fun collectsRootDexAndResourcesAcrossBaseAndSplits() {
    val baseDex = createDex("Lcom/example/Base;")
    val splitDex = createDex("Lcom/example/Feature;")
    val base = createArchive(
      "base.apk",
      mapOf(
        "classes.dex" to baseDex,
        "resources.arsc" to byteArrayOf(1, 2, 3, 4)
      )
    )
    val split = createArchive(
      "feature.apk",
      mapOf(
        "classes.dex" to splitDex,
        "resources.arsc" to byteArrayOf(1, 2, 3, 4, 5)
      )
    )

    val stats = DexStatsCollector.collect(
      listOf(
        DexStatsCollector.DexSource("base", base.path),
        DexStatsCollector.DexSource("split:feature", split.path)
      )
    )

    assertEquals(
      listOf("base/classes.dex", "split:feature/classes.dex"),
      stats.entries.map(DexEntryInfo::name)
    )
    assertEquals(listOf(baseDex.size.toLong(), splitDex.size.toLong()), stats.entries.map(DexEntryInfo::size))
    assertEquals(listOf(1, 1), stats.entries.map(DexEntryInfo::classCount))
    assertTrue(stats.isDexComplete)
    assertTrue(stats.isResourceComplete)
    assertEquals(9L, stats.resourcesSize)
  }

  @Test
  fun detectsSameSizeDexContentChanges() {
    val oldDex = createDex("Lcom/example/Old;")
    val newDex = createDex("Lcom/example/New;")
    assertEquals(oldDex.size, newDex.size)
    val oldArchive = createArchive(
      "old-dex.apk",
      mapOf("classes.dex" to oldDex)
    )
    val newArchive = createArchive(
      "new-dex.apk",
      mapOf("classes.dex" to newDex)
    )

    val oldStats = DexStatsCollector.collect(oldArchive.path)
    val newStats = DexStatsCollector.collect(newArchive.path)

    assertNotEquals(oldStats.entries, newStats.entries)
  }

  @Test
  fun detectsSameSizeResourceContentChanges() {
    val oldArchive = createArchive(
      "old-resources.apk",
      mapOf("resources.arsc" to byteArrayOf(1, 2, 3))
    )
    val newArchive = createArchive(
      "new-resources.apk",
      mapOf("resources.arsc" to byteArrayOf(3, 2, 1))
    )

    val oldStats = DexStatsCollector.collect(oldArchive.path)
    val newStats = DexStatsCollector.collect(newArchive.path)

    assertNotEquals(oldStats, newStats)
  }

  @Test
  fun malformedDexDoesNotDiscardResourceStats() {
    val archive = createArchive(
      "malformed-dex.apk",
      mapOf(
        "classes.dex" to byteArrayOf(1, 2, 3),
        "resources.arsc" to byteArrayOf(4, 5, 6)
      )
    )

    val stats = DexStatsCollector.collect(archive.path)

    assertTrue(stats.entries.isEmpty())
    assertTrue(stats.isDexComplete.not())
    assertTrue(stats.isResourceComplete)
    assertEquals(3L, stats.resourcesSize)
    assertEquals(listOf("base/resources.arsc"), stats.resourceEntries.map(ResourceEntryInfo::name))
  }

  @Test
  fun ignoresNestedDexEntries() {
    val archive = createArchive(
      "nested.apk",
      mapOf("assets/classes.dex" to byteArrayOf(1, 2, 3))
    )

    val stats = DexStatsCollector.collect(archive.path)

    assertTrue(stats.entries.isEmpty())
  }

  @Test
  fun rejectsArchivesWithTooManyDexEntries() {
    val entries = (1..101).associate { index ->
      "classes$index.dex" to byteArrayOf(1)
    }
    val archive = createArchive("many.apk", entries)

    val stats = DexStatsCollector.collect(archive.path)

    assertEquals(DexStatsCollector.DexStats(emptyList(), emptyList(), false, true), stats)
  }

  @Test
  fun rejectsTooManyDexEntriesAcrossSources() {
    val first = createArchive(
      "first.apk",
      (1..60).associate { index -> "classes$index.dex" to byteArrayOf(1) }
    )
    val second = createArchive(
      "second.apk",
      (61..101).associate { index -> "classes$index.dex" to byteArrayOf(1) }
    )

    val stats = DexStatsCollector.collect(
      listOf(
        DexStatsCollector.DexSource("base", first.path),
        DexStatsCollector.DexSource("split:second", second.path)
      )
    )

    assertEquals(DexStatsCollector.DexStats(emptyList(), emptyList(), false, true), stats)
  }

  @Test
  fun rejectsDexEntryNamesThatCannotBeRestored() {
    val archive = createArchive(
      "long-name.apk",
      mapOf(
        "classes${"1".repeat(300)}.dex" to createDex("Lcom/example/LongName;"),
        "resources.arsc" to byteArrayOf(1, 2, 3)
      )
    )

    val stats = DexStatsCollector.collect(archive.path)

    assertTrue(stats.entries.isEmpty())
    assertTrue(stats.isDexComplete.not())
    assertTrue(stats.isResourceComplete)
  }

  @Test
  fun usesDeclaredSplitNameInsteadOfArchiveFileName() {
    val base = createArchive("stable-base.apk", mapOf("classes.dex" to createDex("Lcom/example/Base;")))
    val split = createArchive(
      "renamed-physical-file.apk",
      mapOf("classes.dex" to createDex("Lcom/example/Feature;"))
    )
    val packageInfo = PackageInfo().apply {
      splitNames = arrayOf("feature")
      applicationInfo = ApplicationInfo().apply {
        sourceDir = base.path
        splitSourceDirs = arrayOf(split.path)
        enabled = true
      }
    }

    val stats = DexStatsCollector.collect(packageInfo)

    assertEquals(
      listOf("base/classes.dex", "split:feature/classes.dex"),
      stats.entries.map(DexEntryInfo::name)
    )
  }

  @Test
  fun collectsCompatibilityResolvedSplitsForFrozenPackage() {
    val base = createArchive("base.apk", mapOf("classes.dex" to createDex("Lcom/example/Base;")))
    createArchive(
      "split_feature.apk",
      mapOf("classes.dex" to createDex("Lcom/example/Feature;"))
    )
    val packageInfo = PackageInfo().apply {
      applicationInfo = ApplicationInfo().apply {
        sourceDir = base.path
        enabled = false
      }
    }

    val stats = DexStatsCollector.collect(packageInfo)

    assertEquals(
      listOf("base/classes.dex", "split:split_feature/classes.dex"),
      stats.entries.map(DexEntryInfo::name)
    )
  }

  private fun createArchive(
    name: String,
    entries: Map<String, ByteArray>
  ): File {
    val file = temporaryFolder.newFile(name)
    ZipOutputStream(file.outputStream()).use { output ->
      for ((entryName, content) in entries) {
        output.putNextEntry(ZipEntry(entryName))
        output.write(content)
        output.closeEntry()
      }
    }
    return file
  }

  private fun createDex(className: String): ByteArray {
    val dexFile = temporaryFolder.newFile("${className.hashCode()}.dex")
    val classDef = ImmutableClassDef(
      className,
      0x1,
      "Ljava/lang/Object;",
      null,
      null,
      null,
      null,
      null
    )
    DexPool.writeTo(
      dexFile.path,
      ImmutableDexFile(Opcodes.getDefault(), listOf(classDef))
    )
    return dexFile.readBytes()
  }
}
