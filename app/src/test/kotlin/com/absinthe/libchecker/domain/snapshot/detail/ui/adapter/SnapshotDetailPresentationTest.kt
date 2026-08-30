package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter

import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemStatusDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailRuleChipDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailSection
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailStatusCount
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.SnapshotDetailCountRenderState
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.SnapshotDetailItemViewRenderState
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.SnapshotDetailMovedPathRenderState
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.SnapshotDetailRuleChipIconStyle
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.SnapshotDetailRuleChipRenderState
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.SnapshotDetailTitleRenderState
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.toItemViewRenderState
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.toTitleRenderState
import com.absinthe.libchecker.domain.snapshot.model.ADDED
import com.absinthe.libchecker.domain.snapshot.model.MOVED
import com.absinthe.libchecker.domain.snapshot.model.REMOVED
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDetailItem
import org.junit.Assert.assertEquals
import org.junit.Test

class SnapshotDetailPresentationTest {

  @Test
  fun mapsHeaderCountsAndExpandedCollapsedDescriptions() {
    val section = SnapshotDetailSection(
      type = SERVICE,
      title = "Services",
      reportText = "[Services]\n",
      expandedDescription = "Services, Added 2, Removed 1, Expanded",
      collapsedDescription = "Services, Added 2, Removed 1, Collapsed",
      items = emptyList(),
      statusCounts = listOf(
        SnapshotDetailStatusCount(
          diffType = ADDED,
          count = 2,
          countText = "2",
          label = "Added",
          status = SnapshotDetailItemStatusDisplayData(
            iconRes = 10,
            colorRes = 11,
            labelRes = 12
          )
        ),
        SnapshotDetailStatusCount(
          diffType = REMOVED,
          count = 1,
          countText = "1",
          label = "Removed",
          status = SnapshotDetailItemStatusDisplayData(
            iconRes = 20,
            colorRes = 21,
            labelRes = 22
          )
        )
      )
    )

    assertEquals(
      SnapshotDetailTitleRenderState(
        title = "Services",
        counts = listOf(
          SnapshotDetailCountRenderState(
            diffType = ADDED,
            iconRes = 10,
            countText = "2",
            colorRes = 11
          ),
          SnapshotDetailCountRenderState(
            diffType = REMOVED,
            iconRes = 20,
            countText = "1",
            colorRes = 21
          )
        ),
        contentDescription = "Services, Added 2, Removed 1, Expanded",
        expanded = true
      ),
      section.toTitleRenderState(expanded = true)
    )

    assertEquals(
      SnapshotDetailTitleRenderState(
        title = "Services",
        counts = listOf(
          SnapshotDetailCountRenderState(
            diffType = ADDED,
            iconRes = 10,
            countText = "2",
            colorRes = 11
          ),
          SnapshotDetailCountRenderState(
            diffType = REMOVED,
            iconRes = 20,
            countText = "1",
            colorRes = 21
          )
        ),
        contentDescription = "Services, Added 2, Removed 1, Collapsed",
        expanded = false
      ),
      section.toTitleRenderState(expanded = false)
    )
  }

  @Test
  fun mapsMovedPathAndThemeTintRuleChipStyle() {
    val currentName = "com.example.new.SyncService"
    val item = SnapshotDetailItemDisplayData(
      item = SnapshotDetailItem(
        name = currentName,
        title = "com.example.old.SyncService\n→\n$currentName",
        extra = "",
        diffType = MOVED,
        itemType = SERVICE,
        previousName = "com.example.old.SyncService"
      ),
      title = "com.example.old.SyncService\n→\n$currentName",
      extra = "",
      description = "Moved, com.example.old.SyncService, $currentName",
      reportText = "service report\n",
      status = SnapshotDetailItemStatusDisplayData(
        iconRes = 30,
        colorRes = 31,
        labelRes = 32
      ),
      ruleChip = SnapshotDetailRuleChipDisplayData(
        label = "Jetpack App Startup",
        iconRes = 40,
        regexName = null,
        isSimpleColorIcon = true,
        useColorfulIcon = false
      )
    )

    assertEquals(
      SnapshotDetailItemViewRenderState(
        title = currentName,
        extra = "",
        iconRes = 30,
        statusColorRes = 31,
        statusLabelRes = 32,
        contentDescription = "Moved, com.example.old.SyncService, $currentName",
        ruleChip = SnapshotDetailRuleChipRenderState(
          label = "Jetpack App Startup",
          iconRes = 40,
          iconStyle = SnapshotDetailRuleChipIconStyle.ThemeTint
        ),
        movedPath = SnapshotDetailMovedPathRenderState("com.example.old")
      ),
      item.toItemViewRenderState()
    )
  }

  @Test
  fun mapsOriginalRuleChipStyleWhenColorfulIconsAreEnabled() {
    val item = buildNativeItem(
      ruleChip = SnapshotDetailRuleChipDisplayData(
        label = "WebRTC",
        iconRes = 50,
        regexName = "libjingle.*",
        isSimpleColorIcon = false,
        useColorfulIcon = true
      )
    )

    assertEquals(
      SnapshotDetailRuleChipIconStyle.Original,
      item.toItemViewRenderState().ruleChip?.iconStyle
    )
  }

  @Test
  fun mapsDesaturatedRuleChipStyleWhenColorfulIconsAreDisabled() {
    val item = buildNativeItem(
      ruleChip = SnapshotDetailRuleChipDisplayData(
        label = "WebRTC",
        iconRes = 50,
        regexName = "libjingle.*",
        isSimpleColorIcon = false,
        useColorfulIcon = false
      )
    )

    assertEquals(
      SnapshotDetailRuleChipIconStyle.Desaturated,
      item.toItemViewRenderState().ruleChip?.iconStyle
    )
  }

  private fun buildNativeItem(ruleChip: SnapshotDetailRuleChipDisplayData): SnapshotDetailItemDisplayData {
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
      description = "Moved, libjingle.so, 12 MB",
      reportText = "native report\n",
      status = SnapshotDetailItemStatusDisplayData(
        iconRes = 60,
        colorRes = 61,
        labelRes = 62
      ),
      ruleChip = ruleChip
    )
  }
}
