package com.absinthe.libchecker.domain.snapshot.detail.ui.model

import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailSection

data class SnapshotDetailTitleRenderState(
  val title: String,
  val counts: List<SnapshotDetailCountRenderState>,
  val contentDescription: String,
  val expanded: Boolean
)

fun SnapshotDetailSection.toTitleRenderState(expanded: Boolean): SnapshotDetailTitleRenderState {
  return SnapshotDetailTitleRenderState(
    title = title,
    counts = statusCounts.map { count ->
      SnapshotDetailCountRenderState(
        diffType = count.diffType,
        iconRes = count.status.iconRes,
        countText = count.countText,
        colorRes = count.status.colorRes
      )
    },
    contentDescription = if (expanded) expandedDescription else collapsedDescription,
    expanded = expanded
  )
}
