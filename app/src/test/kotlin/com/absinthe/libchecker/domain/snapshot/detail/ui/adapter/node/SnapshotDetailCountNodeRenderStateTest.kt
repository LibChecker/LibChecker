package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node

import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemStatusDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.SnapshotDetailCountRenderState
import com.absinthe.libchecker.domain.snapshot.model.MOVED
import org.junit.Assert.assertEquals
import org.junit.Test

class SnapshotDetailCountNodeRenderStateTest {

  @Test
  fun exposesCountRenderState() {
    val node = SnapshotDetailCountNode(
      diffType = MOVED,
      count = 3,
      countText = "3",
      status = SnapshotDetailItemStatusDisplayData(
        iconRes = 10,
        colorRes = 11,
        labelRes = 13
      )
    )

    assertEquals(
      SnapshotDetailCountRenderState(
        diffType = MOVED,
        iconRes = 10,
        countText = "3",
        colorRes = 11
      ),
      node.countRenderState
    )
  }
}
