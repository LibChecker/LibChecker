package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node

import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemStatusDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.SnapshotDetailCountRenderState
import org.junit.Assert.assertEquals
import org.junit.Test

class SnapshotDetailCountNodeRenderStateTest {

  @Test
  fun exposesCountRenderState() {
    val node = SnapshotDetailCountNode(
      count = 3,
      countText = "3",
      status = SnapshotDetailItemStatusDisplayData(
        iconRes = 10,
        colorRes = 11,
        countColorRes = 12,
        labelRes = 13
      )
    )

    assertEquals(
      SnapshotDetailCountRenderState(
        text = "3",
        backgroundTintRes = 12
      ),
      node.countRenderState
    )
  }
}
