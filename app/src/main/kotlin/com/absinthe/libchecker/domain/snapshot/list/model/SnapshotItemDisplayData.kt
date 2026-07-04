package com.absinthe.libchecker.domain.snapshot.list.model

import com.absinthe.libchecker.domain.snapshot.display.SnapshotAbiDisplayData
import com.absinthe.libchecker.domain.snapshot.display.SnapshotUpdateTimeDisplayData
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.domain.snapshot.model.SnapshotPackageIconSource

data class SnapshotItemDisplayData(
  val cardPresentation: SnapshotItemCardPresentation,
  val iconSource: SnapshotPackageIconSource?,
  val packageName: String,
  val labelDiff: SnapshotDiffItem.DiffNode<String>,
  val isTrackItem: Boolean,
  val isNewInstalled: Boolean,
  val isDeleted: Boolean,
  val stateIndicator: SnapshotItemStateIndicatorData,
  val versionNameDiff: SnapshotDiffItem.DiffNode<String>,
  val versionCodeDiff: SnapshotDiffItem.DiffNode<Long>,
  val packageSizeDiff: SnapshotDiffItem.DiffNode<Long>,
  val api: SnapshotItemApiDisplayData,
  val abi: SnapshotItemAbiDisplayData,
  val updateTimeDisplayData: SnapshotUpdateTimeDisplayData?,
  val highlightDiffs: Boolean,
  val highlightText: String
)

data class SnapshotItemStateIndicatorData(
  val added: Int,
  val removed: Int,
  val changed: Int,
  val moved: Int,
  val animate: Boolean
)

data class SnapshotItemApiDisplayData(
  val targetApiDiff: SnapshotDiffItem.DiffNode<Short>,
  val minSdkDiff: SnapshotDiffItem.DiffNode<Short>,
  val compileSdkDiff: SnapshotDiffItem.DiffNode<Short>
)

data class SnapshotItemAbiDisplayData(
  val abiDisplayData: SnapshotAbiDisplayData,
  val showChangedAbi: Boolean,
  val tintChangedAbiBadge: Boolean
)

enum class SnapshotItemCardPresentation {
  Normal,
  Rounded
}
