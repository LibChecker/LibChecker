package com.absinthe.libchecker.domain.snapshot.comparison.model

import com.absinthe.libchecker.database.entity.SnapshotItem

data class SnapshotComparisonLists(
  val left: List<SnapshotItem>,
  val right: List<SnapshotItem>
)
