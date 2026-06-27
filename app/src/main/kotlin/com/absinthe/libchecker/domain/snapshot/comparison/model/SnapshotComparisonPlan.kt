package com.absinthe.libchecker.domain.snapshot.comparison.model

import com.absinthe.libchecker.domain.snapshot.ArchiveSnapshotItem

sealed interface SnapshotComparisonPlan {

  data class TimestampRange(
    val previousTimestamp: Long,
    val currentTimestamp: Long
  ) : SnapshotComparisonPlan

  data class ArchivePair(
    val left: ArchiveSnapshotItem,
    val right: ArchiveSnapshotItem,
    val requiresDifferentPackageConfirmation: Boolean
  ) : SnapshotComparisonPlan

  data class SnapshotLists(
    val lists: SnapshotComparisonLists
  ) : SnapshotComparisonPlan
}
