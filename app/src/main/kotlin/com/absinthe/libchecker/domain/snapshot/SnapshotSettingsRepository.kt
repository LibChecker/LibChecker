package com.absinthe.libchecker.domain.snapshot

interface SnapshotSettingsRepository {
  var options: Int
  var autoRemoveThreshold: Int
  val listDisplayOptions: SnapshotListDisplayOptions
}

data class SnapshotListDisplayOptions(
  val highlightDiffs: Boolean = false,
  val showUpdateTime: Boolean = false,
  val tintAbiLabels: Boolean = false
)
