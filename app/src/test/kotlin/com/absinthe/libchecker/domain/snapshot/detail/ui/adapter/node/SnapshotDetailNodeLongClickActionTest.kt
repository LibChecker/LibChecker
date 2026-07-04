package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node

import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemStatusDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailRuleChipDisplayData
import com.absinthe.libchecker.domain.snapshot.model.MOVED
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDetailItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SnapshotDetailNodeLongClickActionTest {

  @Test
  fun returnsNullLongClickActionForTitleNode() {
    val node = SnapshotTitleNode(
      childNode = mutableListOf(),
      type = SERVICE,
      title = "Services",
      reportText = "[Services]\n",
      expandedDescription = "Services, Moved 1, Expanded",
      collapsedDescription = "Services, Moved 1, Collapsed",
      counts = emptyList()
    )

    assertNull(node.longClickAction(ownerPackageName = "com.example.app"))
  }

  @Test
  fun returnsOpenReferenceActionForExternalComponent() {
    val node = SnapshotComponentNode(
      buildDisplayData(
        name = "androidx.startup.InitializationProvider",
        ruleChip = SnapshotDetailRuleChipDisplayData(
          label = "Jetpack App Startup",
          iconRes = 0,
          regexName = null,
          isSimpleColorIcon = false,
          useColorfulIcon = false
        )
      )
    )

    assertEquals(
      SnapshotDetailNodeLongClickAction.OpenReference(
        SnapshotReferenceNavigationTarget(
          refName = "androidx.startup.InitializationProvider",
          label = "Jetpack App Startup",
          refType = SERVICE
        )
      ),
      node.longClickAction(ownerPackageName = "com.example.app")
    )
  }

  @Test
  fun returnsNullLongClickActionForOwnerComponent() {
    val node = SnapshotComponentNode(
      buildDisplayData(
        name = "com.example.app.SyncService",
        ruleChip = null
      )
    )

    assertNull(node.longClickAction(ownerPackageName = "com.example.app"))
  }

  private fun buildDisplayData(
    name: String,
    ruleChip: SnapshotDetailRuleChipDisplayData?
  ): SnapshotDetailItemDisplayData {
    return SnapshotDetailItemDisplayData(
      item = SnapshotDetailItem(
        name = name,
        title = name,
        extra = "",
        diffType = MOVED,
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
      ruleChip = ruleChip
    )
  }
}
