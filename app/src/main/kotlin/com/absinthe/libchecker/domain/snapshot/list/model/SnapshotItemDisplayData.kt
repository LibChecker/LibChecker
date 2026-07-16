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
  val highlightText: String,
  val contentDescription: String
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

fun buildSnapshotItemDescription(
  appName: CharSequence,
  packageName: CharSequence,
  versionInfo: CharSequence,
  packageSize: CharSequence?,
  apiText: CharSequence,
  abiText: CharSequence,
  updateTime: CharSequence?,
  stateDescription: CharSequence
): String {
  return listOf(
    appName,
    packageName,
    versionInfo,
    packageSize,
    apiText,
    abiText,
    updateTime,
    stateDescription
  )
    .mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotEmpty) }
    .joinToString()
}

fun buildSnapshotItemAbiDescription(data: SnapshotItemAbiDisplayData): String {
  val oldAbi = data.abiDisplayData.old.text
  if (!data.showChangedAbi) {
    return oldAbi
  }
  return listOfNotNull(oldAbi, data.abiDisplayData.new?.text)
    .joinToString(separator = " → ")
}
