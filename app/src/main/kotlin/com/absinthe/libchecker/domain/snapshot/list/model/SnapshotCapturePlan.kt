package com.absinthe.libchecker.domain.snapshot.list.model

sealed interface SnapshotCapturePlan {

  data class Capture(
    val dropPrevious: Boolean
  ) : SnapshotCapturePlan

  data class ConfirmKeepPrevious(
    val bridgeUri: String
  ) : SnapshotCapturePlan

  data object NoAction : SnapshotCapturePlan
}
