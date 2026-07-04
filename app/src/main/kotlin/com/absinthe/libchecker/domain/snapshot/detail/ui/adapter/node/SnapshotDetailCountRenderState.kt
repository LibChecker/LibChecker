package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node

import com.absinthe.libchecker.domain.snapshot.detail.ui.model.SnapshotDetailCountRenderState

val SnapshotDetailCountNode.countRenderState: SnapshotDetailCountRenderState
  get() = SnapshotDetailCountRenderState(
    text = countText,
    backgroundTintRes = status.countColorRes
  )
