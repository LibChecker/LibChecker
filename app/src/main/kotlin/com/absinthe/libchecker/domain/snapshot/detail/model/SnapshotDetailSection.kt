package com.absinthe.libchecker.domain.snapshot.detail.model

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.domain.snapshot.model.ADDED
import com.absinthe.libchecker.domain.snapshot.model.CHANGED
import com.absinthe.libchecker.domain.snapshot.model.MOVED
import com.absinthe.libchecker.domain.snapshot.model.REMOVED
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDetailItem
import com.absinthe.rulesbundle.Rule
import java.text.NumberFormat

data class SnapshotDetailContent(
  val sections: List<SnapshotDetailSection>,
  val summary: SnapshotDetailSummary
)

data class SnapshotDetailSummary(
  val totalCount: Int,
  val totalCountText: String,
  val statusCounts: List<SnapshotDetailStatusCount>,
  val description: String
)

data class SnapshotDetailSection(
  @LibType val type: Int,
  val title: String,
  val reportText: String,
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
  val reportText: String,
  val status: SnapshotDetailItemStatusDisplayData,
  val ruleChip: SnapshotDetailRuleChipDisplayData?
)

fun buildSnapshotDetailReportSectionText(title: CharSequence): String {
  return "[$title]\n"
}

fun buildSnapshotDetailReportItemText(item: SnapshotDetailItem): String {
  return buildString {
    append(buildSnapshotDetailReportDiffTypeLabel(item.diffType))
    append(" ")
    append(item.title)
    appendLine()
    if (item.itemType == NATIVE || item.itemType == METADATA) {
      append("\t")
      append(item.extra)
      appendLine()
    }
  }
}

private fun buildSnapshotDetailReportDiffTypeLabel(diffType: Int): String {
  return when (diffType) {
    ADDED -> "🟢+"
    REMOVED -> "🔴-"
    CHANGED -> "🟡~"
    MOVED -> "🔵<->"
    else -> throw IllegalArgumentException("wrong diff type")
  }
}

fun buildSnapshotDetailItemDescription(
  statusLabel: CharSequence?,
  title: CharSequence?,
  extra: CharSequence?,
  ruleLabel: CharSequence?
): String {
  return listOf(statusLabel, ruleLabel, title, extra)
    .mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotEmpty) }
    .joinToString()
}

fun buildSnapshotDetailSummary(
  sections: List<SnapshotDetailSection>,
  totalCountFormatter: (Int) -> String
): SnapshotDetailSummary {
  val countsByStatus = sections
    .flatMap(SnapshotDetailSection::statusCounts)
    .groupBy(SnapshotDetailStatusCount::diffType)
  val statusCounts = summaryStatusOrder.mapNotNull { diffType ->
    val counts = countsByStatus[diffType].orEmpty()
    val count = counts.sumOf(SnapshotDetailStatusCount::count)
    counts.firstOrNull()?.copy(
      count = count,
      countText = NumberFormat.getIntegerInstance().format(count)
    )
  }
  val totalCount = statusCounts.sumOf(SnapshotDetailStatusCount::count)
  val totalCountText = totalCountFormatter(totalCount)
  return SnapshotDetailSummary(
    totalCount = totalCount,
    totalCountText = totalCountText,
    statusCounts = statusCounts,
    description = (
      listOf(totalCountText) + statusCounts.map { "${it.label} ${it.countText}" }
      ).joinToString()
  )
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
  val diffType: Int,
  val count: Int,
  val countText: String,
  val label: String,
  val status: SnapshotDetailItemStatusDisplayData
)

private val summaryStatusOrder = listOf(ADDED, REMOVED, CHANGED, MOVED)
