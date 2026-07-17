package com.absinthe.libchecker.domain.snapshot.display

import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SnapshotVersionDisplayTest {

  @Test
  fun `shows archived state before restored version`() {
    val item = snapshotDiffItem(
      oldVersionName = "null",
      newVersionName = "8.0.69",
      oldArchived = true,
      newArchived = false
    )

    assertEquals(
      SnapshotDiffItem.DiffNode("已归档 (3022)", "8.0.69 (3022)"),
      item.buildSnapshotVersionDisplayDiff("已归档")
    )
    assertFalse(item.isNothingChanged())
  }

  @Test
  fun `shows archived state after installed version`() {
    val item = snapshotDiffItem(
      oldVersionName = "8.0.69",
      newVersionName = "",
      oldArchived = false,
      newArchived = true
    )

    assertEquals(
      SnapshotDiffItem.DiffNode("8.0.69 (3022)", "已归档 (3022)"),
      item.buildSnapshotVersionDisplayDiff("已归档")
    )
  }

  @Test
  fun `removes legacy null version text`() {
    val item = snapshotDiffItem(
      oldVersionName = "null",
      newVersionName = "8.0.69",
      oldArchived = false,
      newArchived = false
    )

    assertEquals(
      SnapshotDiffItem.DiffNode("3022", "8.0.69 (3022)"),
      item.buildSnapshotVersionDisplayDiff("已归档")
    )
  }

  private fun snapshotDiffItem(
    oldVersionName: String,
    newVersionName: String,
    oldArchived: Boolean,
    newArchived: Boolean
  ): SnapshotDiffItem {
    return SnapshotDiffItem(
      packageName = "com.example",
      updateTime = 0L,
      labelDiff = SnapshotDiffItem.DiffNode("Example", "Example"),
      versionNameDiff = SnapshotDiffItem.DiffNode(oldVersionName, newVersionName),
      versionCodeDiff = SnapshotDiffItem.DiffNode(3022L, 3022L),
      abiDiff = SnapshotDiffItem.DiffNode(0, 0),
      targetApiDiff = SnapshotDiffItem.DiffNode(35, 35),
      compileSdkDiff = SnapshotDiffItem.DiffNode(35, 35),
      minSdkDiff = SnapshotDiffItem.DiffNode(24, 24),
      nativeLibsDiff = SnapshotDiffItem.DiffNode("", ""),
      servicesDiff = SnapshotDiffItem.DiffNode("", ""),
      activitiesDiff = SnapshotDiffItem.DiffNode("", ""),
      receiversDiff = SnapshotDiffItem.DiffNode("", ""),
      providersDiff = SnapshotDiffItem.DiffNode("", ""),
      permissionsDiff = SnapshotDiffItem.DiffNode("", ""),
      metadataDiff = SnapshotDiffItem.DiffNode("", ""),
      packageSizeDiff = SnapshotDiffItem.DiffNode(0L, 0L),
      archivedDiff = SnapshotDiffItem.DiffNode(oldArchived, newArchived)
    )
  }
}
