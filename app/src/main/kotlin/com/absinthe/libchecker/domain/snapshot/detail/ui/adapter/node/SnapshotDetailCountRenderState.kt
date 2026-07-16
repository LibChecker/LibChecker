package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node

import com.absinthe.libchecker.domain.snapshot.detail.ui.model.SnapshotDetailCountRenderState
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.buildSnapshotDetailSignedCountText

val SnapshotDetailCountNode.countRenderState: SnapshotDetailCountRenderState
  get() = SnapshotDetailCountRenderState(
    text = buildSnapshotDetailSignedCountText(diffType, countText),
    colorRes = status.colorRes
  )
