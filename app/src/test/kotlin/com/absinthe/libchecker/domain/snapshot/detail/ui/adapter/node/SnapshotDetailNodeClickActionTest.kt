package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node

import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemStatusDisplayData
import com.absinthe.libchecker.domain.snapshot.model.MOVED
import com.absinthe.libchecker.domain.snapshot.model.REMOVED
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDetailItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SnapshotDetailNodeClickActionTest {

  @Test
  fun returnsToggleSectionActionForTitleNode() {
    val node = SnapshotTitleNode(
      childNode = mutableListOf(),
      type = SERVICE,
      title = "Services",
      reportText = "[Services]\n",
      expandedDescription = "Services, Moved 1, Expanded",
      collapsedDescription = "Services, Moved 1, Collapsed",
      counts = emptyList()
    )

    assertEquals(SnapshotDetailNodeClickAction.ToggleSection, node.clickAction)
  }

  @Test
  fun returnsOpenDetailActionForNavigableItemNode() {
    val node = SnapshotComponentNode(
      buildDisplayData(
        name = "com.example.SyncService",
        diffType = MOVED
      )
    )

    assertEquals(
      SnapshotDetailNodeClickAction.OpenDetail(
        SnapshotDetailNavigationTarget(
          refName = "com.example.SyncService",
          refType = SERVICE
        )
      ),
      node.clickAction
    )
  }

  @Test
  fun returnsNullClickActionForRemovedItemNode() {
    val node = SnapshotComponentNode(
      buildDisplayData(
        name = "com.example.RemovedService",
        diffType = REMOVED
      )
    )

    assertNull(node.clickAction)
  }

  private fun buildDisplayData(
    name: String,
    diffType: Int
  ): SnapshotDetailItemDisplayData {
    return SnapshotDetailItemDisplayData(
      item = SnapshotDetailItem(
        name = name,
        title = name,
        extra = "",
        diffType = diffType,
        itemType = SERVICE
      ),
      title = name,
      extra = "",
      description = name,
      reportText = "component report\n",
      status = SnapshotDetailItemStatusDisplayData(
        iconRes = 0,
        colorRes = 0,
        countColorRes = 0,
        labelRes = 0
      ),
      backgroundColor = 0,
      ruleChip = null
    )
  }
}
