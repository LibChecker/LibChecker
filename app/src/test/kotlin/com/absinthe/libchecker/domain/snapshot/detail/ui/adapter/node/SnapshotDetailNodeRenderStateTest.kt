package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node

import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemStatusDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailRuleChipDisplayData
import com.absinthe.libchecker.domain.snapshot.model.MOVED
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDetailItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SnapshotDetailNodeRenderStateTest {

  @Test
  fun exposesItemRenderStateForNodeWithRuleChip() {
    val ruleChip = SnapshotDetailRuleChipDisplayData(
      label = "WebRTC",
      iconRes = 20,
      regexName = "libjingle_.*",
      isSimpleColorIcon = false,
      useColorfulIcon = true
    )
    val node = SnapshotNativeNode(
      buildDisplayData(
        name = "libjingle_peerconnection_so.so",
        title = "libjingle_peerconnection_so.so",
        extra = "(12 MB)",
        description = "Changed, libjingle_peerconnection_so.so, (12 MB), WebRTC",
        iconRes = 10,
        backgroundColor = 0x12345678,
        ruleChip = ruleChip
      )
    )

    assertEquals(
      SnapshotDetailItemRenderState(
        title = "libjingle_peerconnection_so.so",
        extra = "(12 MB)",
        iconRes = 10,
        backgroundColor = 0x12345678,
        contentDescription = "Changed, libjingle_peerconnection_so.so, (12 MB), WebRTC",
        ruleChip = ruleChip,
        chipClickAction = SnapshotDetailNodeChipClickAction.OpenLibraryDetail(
          SnapshotDetailLibraryDialogTarget(
            name = "libjingle_peerconnection_so.so",
            type = NATIVE,
            regexName = "libjingle_.*"
          )
        )
      ),
      node.itemRenderState
    )
  }

  @Test
  fun exposesItemRenderStateForNodeWithoutRuleChip() {
    val node = SnapshotNativeNode(
      buildDisplayData(
        name = "libplain.so",
        title = "libplain.so",
        extra = "(4 KB)",
        description = "Changed, libplain.so, (4 KB)",
        iconRes = 11,
        backgroundColor = 0x22334455,
        ruleChip = null
      )
    )

    val renderState = node.itemRenderState

    assertEquals("libplain.so", renderState.title)
    assertEquals("(4 KB)", renderState.extra)
    assertEquals(11, renderState.iconRes)
    assertEquals(0x22334455, renderState.backgroundColor)
    assertEquals("Changed, libplain.so, (4 KB)", renderState.contentDescription)
    assertNull(renderState.ruleChip)
    assertNull(renderState.chipClickAction)
  }

  private fun buildDisplayData(
    name: String,
    title: String,
    extra: String,
    description: String,
    iconRes: Int,
    backgroundColor: Int,
    ruleChip: SnapshotDetailRuleChipDisplayData?
  ): SnapshotDetailItemDisplayData {
    return SnapshotDetailItemDisplayData(
      item = SnapshotDetailItem(
        name = name,
        title = title,
        extra = extra,
        diffType = MOVED,
        itemType = NATIVE
      ),
      title = title,
      extra = extra,
      description = description,
      reportText = "snapshot item report\n",
      status = SnapshotDetailItemStatusDisplayData(
        iconRes = iconRes,
        colorRes = 0,
        countColorRes = 0,
        labelRes = 0
      ),
      backgroundColor = backgroundColor,
      ruleChip = ruleChip
    )
  }
}
