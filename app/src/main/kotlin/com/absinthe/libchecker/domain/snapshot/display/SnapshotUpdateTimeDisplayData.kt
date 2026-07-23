package com.absinthe.libchecker.domain.snapshot.display

data class SnapshotUpdateTimeDisplayData(
  val text: SnapshotUpdateTimeText,
  val isApexPackage: Boolean
)

sealed interface SnapshotUpdateTimeText {
  data object Preinstalled : SnapshotUpdateTimeText
  data class LastUpdated(val timeText: String) : SnapshotUpdateTimeText
}
