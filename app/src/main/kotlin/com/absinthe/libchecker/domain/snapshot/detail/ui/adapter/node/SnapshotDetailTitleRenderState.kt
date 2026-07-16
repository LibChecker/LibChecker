package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node

import com.absinthe.libchecker.domain.snapshot.detail.ui.model.SnapshotDetailTitleRenderState

val SnapshotTitleNode.titleRenderState: SnapshotDetailTitleRenderState
  get() = SnapshotDetailTitleRenderState(
    title = title,
    counts = counts.map { it.countRenderState },
    contentDescription = contentDescription,
    expanded = isExpanded
  )
