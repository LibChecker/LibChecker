package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node

import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemStatusDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailRuleChipDisplayData
import com.absinthe.libchecker.domain.snapshot.model.MOVED
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDetailItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SnapshotDetailNodeChipClickActionTest {

  @Test
  fun returnsOpenLibraryDetailActionForNativeRuleChip() {
    val node = SnapshotNativeNode(
      buildDisplayData(
        name = "libjingle_peerconnection_so.so",
        type = NATIVE,
        ruleChip = SnapshotDetailRuleChipDisplayData(
          label = "WebRTC",
          iconRes = 0,
          regexName = "libjingle_.*",
          isSimpleColorIcon = false,
          useColorfulIcon = false
        )
      )
    )

    assertEquals(
      SnapshotDetailNodeChipClickAction.OpenLibraryDetail(
        SnapshotDetailLibraryDialogTarget(
          name = "libjingle_peerconnection_so.so",
          type = NATIVE,
          regexName = "libjingle_.*"
        )
      ),
      node.chipClickAction
    )
  }

  @Test
  fun returnsOpenLibraryDetailActionForComponentRuleChip() {
    val node = SnapshotComponentNode(
      buildDisplayData(
        name = "androidx.startup.InitializationProvider",
        type = SERVICE,
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
      SnapshotDetailNodeChipClickAction.OpenLibraryDetail(
        SnapshotDetailLibraryDialogTarget(
          name = "androidx.startup.InitializationProvider",
          type = SERVICE,
          regexName = null
        )
      ),
      node.chipClickAction
    )
  }

  @Test
  fun returnsNullChipClickActionWithoutRuleChip() {
    val node = SnapshotNativeNode(
      buildDisplayData(
        name = "libplain.so",
        type = NATIVE,
        ruleChip = null
      )
    )

    assertNull(node.chipClickAction)
  }

  @Test
  fun returnsNullChipClickActionForTitleNode() {
    val node = SnapshotTitleNode(
      childNode = mutableListOf(),
      type = NATIVE,
      title = "Native libraries",
      reportText = "[Native libraries]\n",
      expandedDescription = "Native libraries, Moved 1, Expanded",
      collapsedDescription = "Native libraries, Moved 1, Collapsed",
      counts = emptyList()
    )

    assertNull(node.chipClickAction)
  }

  private fun buildDisplayData(
    name: String,
    type: Int,
    ruleChip: SnapshotDetailRuleChipDisplayData?
  ): SnapshotDetailItemDisplayData {
    return SnapshotDetailItemDisplayData(
      item = SnapshotDetailItem(
        name = name,
        title = name,
        extra = "",
        diffType = MOVED,
        itemType = type
      ),
      title = name,
      extra = "",
      description = name,
      reportText = "snapshot item report\n",
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
