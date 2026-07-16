package com.absinthe.libchecker.domain.snapshot.list.usecase

import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.utils.DateUtils

class BuildSnapshotMenuDemoItemUseCase(
  private val versionCode: Long,
  private val sdkInt: Int
) {

  operator fun invoke(): SnapshotDiffItem {
    return SnapshotDiffItem(
      packageName = Constants.EXAMPLE_PACKAGE,
      updateTime = System.currentTimeMillis(),
      labelDiff = SnapshotDiffItem.DiffNode(DateUtils.getCurrentSeasonString(), DateUtils.getNextSeasonString()),
      versionNameDiff = SnapshotDiffItem.DiffNode("2020.3.19", DateUtils.getToday()),
      versionCodeDiff = SnapshotDiffItem.DiffNode(1120, versionCode),
      abiDiff = SnapshotDiffItem.DiffNode(Constants.ARMV7.toShort(), Constants.ARMV8.toShort()),
      targetApiDiff = SnapshotDiffItem.DiffNode((sdkInt - 1).toShort(), sdkInt.toShort()),
      compileSdkDiff = SnapshotDiffItem.DiffNode((sdkInt - 1).toShort(), sdkInt.toShort()),
      minSdkDiff = SnapshotDiffItem.DiffNode((sdkInt - 11).toShort(), (sdkInt - 10).toShort()),
      packageSizeDiff = SnapshotDiffItem.DiffNode(12345678L, 87654321L),
      nativeLibsDiff = SnapshotDiffItem.DiffNode(""),
      servicesDiff = SnapshotDiffItem.DiffNode(""),
      activitiesDiff = SnapshotDiffItem.DiffNode(""),
      receiversDiff = SnapshotDiffItem.DiffNode(""),
      providersDiff = SnapshotDiffItem.DiffNode(""),
      permissionsDiff = SnapshotDiffItem.DiffNode(""),
      metadataDiff = SnapshotDiffItem.DiffNode(""),
      added = 100,
      removed = 100,
      changed = 100,
      moved = 100
    )
  }
}
