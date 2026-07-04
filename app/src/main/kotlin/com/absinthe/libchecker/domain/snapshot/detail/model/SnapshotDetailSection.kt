package com.absinthe.libchecker.domain.snapshot.detail.model

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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
  val title: CharSequence,
  val extra: CharSequence,
  val status: SnapshotDetailItemStatusDisplayData,
  val rule: Rule?,
  val colorfulRuleIcon: Boolean
)

data class SnapshotDetailItemStatusDisplayData(
  @DrawableRes val iconRes: Int,
  @ColorRes val colorRes: Int,
  @StringRes val labelRes: Int
)

data class SnapshotDetailStatusCount(
  val status: Int,
  val count: Int
)
