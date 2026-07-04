package com.absinthe.libchecker.domain.snapshot.detail.model

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDetailItem
import com.absinthe.rulesbundle.Rule

data class SnapshotDetailSection(
  @LibType val type: Int,
  val title: String,
  val expandedDescription: String,
  val collapsedDescription: String,
  val items: List<SnapshotDetailItemDisplayData>,
  val statusCounts: List<SnapshotDetailStatusCount>
)

data class SnapshotDetailItemDisplayData(
  val item: SnapshotDetailItem,
  val title: CharSequence,
  val extra: CharSequence,
  val description: String,
  val status: SnapshotDetailItemStatusDisplayData,
  val ruleChip: SnapshotDetailRuleChipDisplayData?
)

fun buildSnapshotDetailItemDescription(
  statusLabel: CharSequence?,
  title: CharSequence?,
  extra: CharSequence?,
  ruleLabel: CharSequence?
): String {
  return listOf(statusLabel, title, extra, ruleLabel)
    .mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotEmpty) }
    .joinToString()
}

fun buildSnapshotDetailSectionDescription(
  title: CharSequence?,
  statusCounts: List<SnapshotDetailStatusCount>,
  expansionStateLabel: CharSequence?
): String {
  return (
    listOf(title) +
      statusCounts.map { "${it.label} ${it.countText}" } +
      listOf(expansionStateLabel)
    )
    .mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotEmpty) }
    .joinToString()
}

fun buildSnapshotDetailRuleChipDisplayData(
  rule: Rule?,
  colorfulRuleIcon: Boolean
): SnapshotDetailRuleChipDisplayData? {
  return rule?.let {
    SnapshotDetailRuleChipDisplayData(
      label = it.label,
      iconRes = it.iconRes,
      regexName = it.regexName,
      isSimpleColorIcon = it.isSimpleColorIcon,
      useColorfulIcon = colorfulRuleIcon
    )
  }
}

data class SnapshotDetailItemStatusDisplayData(
  @DrawableRes val iconRes: Int,
  @ColorRes val colorRes: Int,
  @ColorRes val countColorRes: Int,
  @StringRes val labelRes: Int
)

data class SnapshotDetailRuleChipDisplayData(
  val label: String,
  @DrawableRes val iconRes: Int,
  val regexName: String?,
  val isSimpleColorIcon: Boolean,
  val useColorfulIcon: Boolean
)

data class SnapshotDetailStatusCount(
  val count: Int,
  val countText: String,
  val label: String,
  val status: SnapshotDetailItemStatusDisplayData
)
