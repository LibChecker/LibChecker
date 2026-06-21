package com.absinthe.libchecker.domain.snapshot

import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem

class BuildSnapshotPairDiffUseCase {

  operator fun invoke(
    left: SnapshotItem,
    right: SnapshotItem
  ): SnapshotDiffItem {
    return SnapshotDiffItem(
      packageName = "${left.packageName}/${right.packageName}",
      updateTime = -1,
      labelDiff = SnapshotDiffItem.DiffNode(left.label, right.label),
      versionNameDiff = SnapshotDiffItem.DiffNode(left.versionName, right.versionName),
      versionCodeDiff = SnapshotDiffItem.DiffNode(left.versionCode, right.versionCode),
      abiDiff = SnapshotDiffItem.DiffNode(left.abi, right.abi),
      targetApiDiff = SnapshotDiffItem.DiffNode(left.targetApi, right.targetApi),
      compileSdkDiff = SnapshotDiffItem.DiffNode(left.compileSdk, right.compileSdk),
      minSdkDiff = SnapshotDiffItem.DiffNode(left.minSdk, right.minSdk),
      nativeLibsDiff = SnapshotDiffItem.DiffNode(left.nativeLibs, right.nativeLibs),
      servicesDiff = SnapshotDiffItem.DiffNode(left.services, right.services),
      activitiesDiff = SnapshotDiffItem.DiffNode(left.activities, right.activities),
      receiversDiff = SnapshotDiffItem.DiffNode(left.receivers, right.receivers),
      providersDiff = SnapshotDiffItem.DiffNode(left.providers, right.providers),
      permissionsDiff = SnapshotDiffItem.DiffNode(left.permissions, right.permissions),
      metadataDiff = SnapshotDiffItem.DiffNode(left.metadata, right.metadata),
      packageSizeDiff = SnapshotDiffItem.DiffNode(left.packageSize, right.packageSize),
      isTrackItem = false
    )
  }
}
