package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node

import androidx.annotation.ColorRes

data class SnapshotDetailCountRenderState(
  val text: String,
  @ColorRes val backgroundTintRes: Int
)

val SnapshotDetailCountNode.countRenderState: SnapshotDetailCountRenderState
  get() = SnapshotDetailCountRenderState(
    text = countText,
    backgroundTintRes = status.countColorRes
  )
