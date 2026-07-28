package com.absinthe.libchecker.domain.snapshot.detail.usecase

import com.absinthe.libchecker.utils.dex.DexEntryInfo
import com.absinthe.libchecker.utils.dex.ResourceEntryInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class SnapshotDetailSectionBuilderTest {

  @Test
  fun `changed dex metrics stay on their own lines`() {
    val extra = buildDexChangedExtra(
      oldEntry = DexEntryInfo(
        name = "base/classes.dex",
        size = 5_743_056,
        classCount = 5_834,
        crc32 = 1
      ),
      newEntry = DexEntryInfo(
        name = "base/classes.dex",
        size = 6_237_668,
        classCount = 6_108,
        crc32 = 2
      ),
      contentChangedText = "Content changed",
      formatSize = { "$it bytes" },
      formatClassCount = { "$it classes" },
      formatSizeDelta = { "$it bytes" }
    )

    assertEquals(
      "5743056 bytes → 6237668 bytes\n+494612 bytes, +8.6%\n" +
        "5834 classes → 6108 classes\n+274 classes, +4.7%",
      extra
    )
  }

  @Test
  fun `same size dex content change explains why it changed`() {
    val extra = buildDexChangedExtra(
      oldEntry = DexEntryInfo("base/classes.dex", 10, 1, 1),
      newEntry = DexEntryInfo("base/classes.dex", 10, 1, 2),
      contentChangedText = "Content changed",
      formatSize = { "$it bytes" },
      formatClassCount = { "$it classes" },
      formatSizeDelta = { "$it bytes" }
    )

    assertEquals("Content changed\n10 bytes\n1 classes", extra)
  }

  @Test
  fun `same size resource content change explains why it changed`() {
    val extra = buildResourceChangedExtra(
      oldEntry = ResourceEntryInfo("base/resources.arsc", 10, 1),
      newEntry = ResourceEntryInfo("base/resources.arsc", 10, 2),
      contentChangedText = "Content changed",
      formatSize = { "$it bytes" },
      formatSizeDelta = { "$it bytes" }
    )

    assertEquals("Content changed\n10 bytes", extra)
  }

  @Test
  fun `resources size includes signed delta and percentage`() {
    assertEquals(
      "853420 bytes → 1033396 bytes\n+179976 bytes, +21.1%",
      buildSizeChangedExtra(
        oldSize = 853_420,
        newSize = 1_033_396,
        formatSize = { "$it bytes" },
        formatSizeDelta = { "$it bytes" }
      )
    )
  }

  @Test
  fun `zero baseline omits percentage`() {
    assertEquals(
      "0 bytes → 10 bytes\n+10 bytes",
      buildSizeChangedExtra(
        oldSize = 0,
        newSize = 10,
        formatSize = { "$it bytes" },
        formatSizeDelta = { "$it bytes" }
      )
    )
  }

  @Test
  fun `decreased metric includes negative delta and percentage`() {
    assertEquals(
      "200 classes → 150 classes\n-50 classes, -25.0%",
      buildSizeChangedExtra(
        oldSize = 200,
        newSize = 150,
        formatSize = { "$it classes" },
        formatSizeDelta = { "$it classes" }
      )
    )
  }

  @Test
  fun `single apk dex names omit base prefix`() {
    assertEquals(
      "classes.dex",
      buildDexDisplayName(
        name = "base/classes.dex",
        allEntryNames = listOf("base/classes.dex", "base/classes2.dex")
      )
    )
  }

  @Test
  fun `split apk dex names keep source prefix`() {
    assertEquals(
      "base/classes.dex",
      buildDexDisplayName(
        name = "base/classes.dex",
        allEntryNames = listOf("base/classes.dex", "split:feature/classes.dex")
      )
    )
  }

  @Test
  fun `all changed dex metric lines are parsed for highlighting`() {
    val changedLines = parseSnapshotDetailChangedLines(
      "5743056 bytes → 6237668 bytes\n+494612 bytes, +8.6%\n5834 classes → 6108 classes"
    ).filter { it.oldValue != null }

    assertEquals(
      listOf(
        "5743056 bytes" to "6237668 bytes",
        "5834 classes" to "6108 classes"
      ),
      changedLines.map { it.oldValue to it.newValue }
    )
  }
}
