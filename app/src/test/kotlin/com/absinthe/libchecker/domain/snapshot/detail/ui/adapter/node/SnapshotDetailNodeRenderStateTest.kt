package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node

import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemStatusDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailRuleChipDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.SnapshotDetailItemViewRenderState
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.SnapshotDetailMovedPathRenderState
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.SnapshotDetailRuleChipIconStyle
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.SnapshotDetailRuleChipRenderState
import com.absinthe.libchecker.domain.snapshot.model.MOVED
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDetailItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SnapshotDetailNodeRenderStateTest {

  @Test
  fun exposesVisibleStatusSemanticsAndRuleAction() {
    val node = SnapshotNativeNode(
      buildDisplayData(
        ruleChip = ruleChip(colorful = true),
        description = "Moved, libjingle.so, 12 MB, WebRTC"
      )
    )

    assertEquals(
      SnapshotDetailItemRenderState(
        title = "libjingle.so",
        extra = "12 MB",
        iconRes = 10,
        statusColorRes = 11,
        statusLabelRes = 12,
        contentDescription = "Moved, libjingle.so, 12 MB, WebRTC",
        ruleChip = SnapshotDetailRuleChipRenderState(
          label = "WebRTC",
          iconRes = 20,
          iconStyle = SnapshotDetailRuleChipIconStyle.Original
        ),
        chipClickAction = SnapshotDetailNodeChipClickAction.OpenLibraryDetail(
          SnapshotDetailLibraryDialogTarget(
            name = "libjingle.so",
            type = NATIVE,
            regexName = "libjingle.*"
          )
        )
      ),
      node.itemRenderState
    )
  }

  @Test
  fun exposesViewStateWithoutAdapterAction() {
    val node = SnapshotNativeNode(buildDisplayData(ruleChip = ruleChip(colorful = true)))

    assertEquals(
      SnapshotDetailItemViewRenderState(
        title = "libjingle.so",
        extra = "12 MB",
        iconRes = 10,
        statusColorRes = 11,
        statusLabelRes = 12,
        contentDescription = "Moved, libjingle.so, 12 MB",
        ruleChip = SnapshotDetailRuleChipRenderState(
          label = "WebRTC",
          iconRes = 20,
          iconStyle = SnapshotDetailRuleChipIconStyle.Original
        )
      ),
      node.itemRenderState.viewRenderState
    )
  }

  @Test
  fun formatsMovedComponentAsPreviousPathAndSingleCurrentName() {
    val previousName = "com.example.old.SyncService"
    val currentName = "com.example.new.SyncService"
    val node = SnapshotComponentNode(
      SnapshotDetailItemDisplayData(
        item = SnapshotDetailItem(
          name = currentName,
          title = "$previousName\n→\n$currentName",
          extra = "",
          diffType = MOVED,
          itemType = SERVICE,
          previousName = previousName
        ),
        title = "$previousName\n→\n$currentName",
        extra = "",
        description = "Moved, $previousName, $currentName",
        reportText = "snapshot report\n",
        status = SnapshotDetailItemStatusDisplayData(
          iconRes = 10,
          colorRes = 11,
          labelRes = 12
        ),
        ruleChip = null
      )
    )

    assertEquals(currentName, node.itemRenderState.title)
    assertEquals(
      SnapshotDetailMovedPathRenderState("com.example.old"),
      node.itemRenderState.movedPath
    )
    assertEquals(currentName, node.itemRenderState.viewRenderState.title)
    assertEquals(
      SnapshotDetailMovedPathRenderState("com.example.old"),
      node.itemRenderState.viewRenderState.movedPath
    )
  }

  @Test
  fun omitsRuleStateAndActionWhenNoRuleMatches() {
    val state = SnapshotNativeNode(buildDisplayData(ruleChip = null)).itemRenderState

    assertNull(state.ruleChip)
    assertNull(state.chipClickAction)
  }

  @Test
  fun desaturatesRuleIconWhenColorfulIconsAreDisabled() {
    val state = SnapshotNativeNode(
      buildDisplayData(ruleChip = ruleChip(colorful = false))
    ).itemRenderState

    assertEquals(SnapshotDetailRuleChipIconStyle.Desaturated, state.ruleChip?.iconStyle)
  }

  @Test
  fun usesThemeTintForSimpleRuleIcon() {
    val state = SnapshotNativeNode(
      buildDisplayData(
        ruleChip = ruleChip(colorful = false, simpleColor = true)
      )
    ).itemRenderState

    assertEquals(SnapshotDetailRuleChipIconStyle.ThemeTint, state.ruleChip?.iconStyle)
  }

  private fun buildDisplayData(
    ruleChip: SnapshotDetailRuleChipDisplayData?,
    description: String = "Moved, libjingle.so, 12 MB"
  ): SnapshotDetailItemDisplayData {
    return SnapshotDetailItemDisplayData(
      item = SnapshotDetailItem(
        name = "libjingle.so",
        title = "libjingle.so",
        extra = "12 MB",
        diffType = MOVED,
        itemType = NATIVE
      ),
      title = "libjingle.so",
      extra = "12 MB",
      description = description,
      reportText = "snapshot report\n",
      status = SnapshotDetailItemStatusDisplayData(
        iconRes = 10,
        colorRes = 11,
        labelRes = 12
      ),
      ruleChip = ruleChip
    )
  }

  private fun ruleChip(
    colorful: Boolean,
    simpleColor: Boolean = false
  ): SnapshotDetailRuleChipDisplayData {
    return SnapshotDetailRuleChipDisplayData(
      label = "WebRTC",
      iconRes = 20,
      regexName = "libjingle.*",
      isSimpleColorIcon = simpleColor,
      useColorfulIcon = colorful
    )
  }
}
