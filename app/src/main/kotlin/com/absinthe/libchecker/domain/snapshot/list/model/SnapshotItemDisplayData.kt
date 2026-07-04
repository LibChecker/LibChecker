package com.absinthe.libchecker.domain.snapshot.list.model

import com.absinthe.libchecker.domain.snapshot.display.SnapshotAbiDisplayData
import com.absinthe.libchecker.domain.snapshot.display.SnapshotUpdateTimeDisplayData
import com.absinthe.libchecker.domain.snapshot.model.SnapshotPackageIconSource

data class SnapshotItemDisplayData(
  val cardPresentation: SnapshotItemCardPresentation,
  val iconSource: SnapshotPackageIconSource?,
  val packageName: String,
  val appName: SnapshotItemAppNameDisplayData,
  val isNewInstalled: Boolean,
  val isDeleted: Boolean,
  val stateIndicator: SnapshotItemStateIndicatorData,
  val versionInfo: CharSequence,
  val packageSize: SnapshotItemPackageSizeDisplayData?,
  val apiText: CharSequence,
  val abi: SnapshotItemAbiDisplayData,
  val updateTimeDisplayData: SnapshotUpdateTimeDisplayData?,
  val highlightText: String
)

data class SnapshotItemAppNameDisplayData(
  val text: CharSequence,
  val showTrackIcon: Boolean,
  val packageStateLabel: SnapshotItemPackageStateLabel?
)

data class SnapshotItemStateIndicatorData(
  val added: Int,
  val removed: Int,
  val changed: Int,
  val moved: Int,
  val stateDescription: CharSequence,
  val animate: Boolean
)

data class SnapshotItemPackageSizeDisplayData(
  val text: CharSequence,
  val breakStart: Int
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

enum class SnapshotItemPackageStateLabel {
  New,
  Deleted
}
