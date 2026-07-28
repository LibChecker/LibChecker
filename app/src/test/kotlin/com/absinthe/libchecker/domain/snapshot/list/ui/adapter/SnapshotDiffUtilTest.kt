package com.absinthe.libchecker.domain.snapshot.list.ui.adapter

import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import org.junit.Assert.assertFalse
import org.junit.Test

class SnapshotDiffUtilTest {

  @Test
  fun dexContentChangeRebindsWhenIndicatorCountIsStable() {
    val oldItem = snapshotDiffItem("[{\"name\":\"base/classes.dex\",\"size\":1}]")
    val newItem = snapshotDiffItem("[{\"name\":\"base/classes.dex\",\"size\":2}]")

    assertFalse(SnapshotDiffUtil().areContentsTheSame(oldItem, newItem))
  }

  private fun snapshotDiffItem(dexInfo: String): SnapshotDiffItem {
    return SnapshotDiffItem(
      packageName = "com.example",
      updateTime = 1,
      labelDiff = SnapshotDiffItem.DiffNode("Example", "Example"),
      versionNameDiff = SnapshotDiffItem.DiffNode("1", "1"),
      versionCodeDiff = SnapshotDiffItem.DiffNode(1, 1),
      abiDiff = SnapshotDiffItem.DiffNode(0, 0),
      targetApiDiff = SnapshotDiffItem.DiffNode(35, 35),
      compileSdkDiff = SnapshotDiffItem.DiffNode(35, 35),
      minSdkDiff = SnapshotDiffItem.DiffNode(24, 24),
      nativeLibsDiff = SnapshotDiffItem.DiffNode("[]", "[]"),
      servicesDiff = SnapshotDiffItem.DiffNode("[]", "[]"),
      activitiesDiff = SnapshotDiffItem.DiffNode("[]", "[]"),
      receiversDiff = SnapshotDiffItem.DiffNode("[]", "[]"),
      providersDiff = SnapshotDiffItem.DiffNode("[]", "[]"),
      permissionsDiff = SnapshotDiffItem.DiffNode("[]", "[]"),
      metadataDiff = SnapshotDiffItem.DiffNode("[]", "[]"),
      packageSizeDiff = SnapshotDiffItem.DiffNode(1, 1),
      dexInfoDiff = SnapshotDiffItem.DiffNode(dexInfo, dexInfo),
      resourcesSizeDiff = SnapshotDiffItem.DiffNode(1, 1),
      changed = 1
    )
  }
}
