package com.absinthe.libchecker.domain.snapshot.detail.model

import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDetailItem
import com.absinthe.rulesbundle.Rule

data class SnapshotDetailSection(
  @LibType val type: Int,
  val items: List<SnapshotDetailItemDisplayData>,
  val statusCounts: List<SnapshotDetailStatusCount>
)

data class SnapshotDetailItemDisplayData(
  val item: SnapshotDetailItem,
  val rule: Rule?,
  val colorfulRuleIcon: Boolean
)

data class SnapshotDetailStatusCount(
  val status: Int,
  val count: Int
)
