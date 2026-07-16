package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node

import org.junit.Assert.assertEquals
import org.junit.Test

class SnapshotTitleNodeTest {

  @Test
  fun returnsExpandedDescriptionWhenExpanded() {
    val node = buildSnapshotTitleNode()

    assertEquals("Services, Moved 1, Expanded", node.contentDescription)
  }

  @Test
  fun returnsCollapsedDescriptionWhenCollapsed() {
    val node = buildSnapshotTitleNode().apply {
      isExpanded = false
    }

    assertEquals("Services, Moved 1, Collapsed", node.contentDescription)
  }

  private fun buildSnapshotTitleNode(): SnapshotTitleNode {
    return SnapshotTitleNode(
      childNode = mutableListOf(),
      type = 0,
      title = "Services",
      reportText = "[Services]\n",
      expandedDescription = "Services, Moved 1, Expanded",
      collapsedDescription = "Services, Moved 1, Collapsed",
      counts = emptyList()
    )
  }
}
