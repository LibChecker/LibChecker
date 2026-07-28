package com.absinthe.libchecker.domain.snapshot

interface SnapshotSettingsRepository {
  var options: Int
  var autoRemoveThreshold: Int
  var keepRule: String
  val listDisplayOptions: SnapshotListDisplayOptions
}

data class SnapshotListDisplayOptions(
  val highlightDiffs: Boolean = false,
  val emphasizeDiffs: Boolean = false,
  val showUpdateTime: Boolean = false,
  val tintAbiLabels: Boolean = false
)
