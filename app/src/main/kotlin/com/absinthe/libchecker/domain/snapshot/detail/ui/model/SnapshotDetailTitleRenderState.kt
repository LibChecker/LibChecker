package com.absinthe.libchecker.domain.snapshot.detail.ui.model

data class SnapshotDetailTitleRenderState(
  val title: String,
  val counts: List<SnapshotDetailCountRenderState>,
  val contentDescription: String,
  val expanded: Boolean
)
