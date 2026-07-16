package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node

import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemStatusDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.SnapshotDetailCountRenderState
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.SnapshotDetailTitleRenderState
import org.junit.Assert.assertEquals
import org.junit.Test

class SnapshotTitleNodeRenderStateTest {

  @Test
  fun exposesExpandedTitleRenderState() {
    val count = SnapshotDetailCountNode(
      count = 2,
      countText = "2",
      status = SnapshotDetailItemStatusDisplayData(
        iconRes = 10,
        colorRes = 11,
        countColorRes = 12,
        labelRes = 13
      )
    )
    val node = buildSnapshotTitleNode(counts = listOf(count))

    assertEquals(
      SnapshotDetailTitleRenderState(
        title = "Native libraries",
        counts = listOf(
          SnapshotDetailCountRenderState(
            text = "2",
            backgroundTintRes = 12
          )
        ),
        contentDescription = "Native libraries, Moved 2, Expanded",
        expanded = true
      ),
      node.titleRenderState
    )
  }

  @Test
  fun exposesCollapsedTitleRenderState() {
    val node = buildSnapshotTitleNode().apply {
      isExpanded = false
    }

    assertEquals(
      SnapshotDetailTitleRenderState(
        title = "Native libraries",
        counts = emptyList(),
        contentDescription = "Native libraries, Moved 2, Collapsed",
        expanded = false
      ),
      node.titleRenderState
    )
  }

  private fun buildSnapshotTitleNode(
    counts: List<SnapshotDetailCountNode> = emptyList()
  ): SnapshotTitleNode {
    return SnapshotTitleNode(
      childNode = mutableListOf(),
      type = NATIVE,
      title = "Native libraries",
      reportText = "[Native libraries]\n",
      expandedDescription = "Native libraries, Moved 2, Expanded",
      collapsedDescription = "Native libraries, Moved 2, Collapsed",
      counts = counts
    )
  }
}
