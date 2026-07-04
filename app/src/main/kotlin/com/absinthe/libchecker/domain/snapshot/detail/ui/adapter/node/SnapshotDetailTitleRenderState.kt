package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node

data class SnapshotDetailTitleRenderState(
  val title: String,
  val counts: List<SnapshotDetailCountNode>,
  val contentDescription: String,
  val expanded: Boolean
)

val SnapshotTitleNode.titleRenderState: SnapshotDetailTitleRenderState
  get() = SnapshotDetailTitleRenderState(
    title = title,
    counts = counts,
    contentDescription = contentDescription,
    expanded = isExpanded
  )
