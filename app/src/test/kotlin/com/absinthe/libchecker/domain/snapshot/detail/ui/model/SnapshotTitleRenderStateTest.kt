package com.absinthe.libchecker.domain.snapshot.detail.ui.model

import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemStatusDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailStatusCount
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailSummary
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotTitleDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotTitlePackageSizeData
import com.absinthe.libchecker.domain.snapshot.model.ADDED
import com.absinthe.libchecker.domain.snapshot.model.REMOVED
import org.junit.Assert.assertEquals
import org.junit.Test

class SnapshotTitleRenderStateTest {

  @Test
  fun mapsDisplayDataToRenderState() {
    val packageSize = SnapshotTitlePackageSizeData(
      text = "251 MB -> 283 MB\n+32 MB",
      breakStart = 17
    )
    val displayData = SnapshotTitleDisplayData(
      appName = "X",
      packageName = "com.twitter.android",
      versionInfo = "11.98.0 -> 12.5.0",
      packageSize = packageSize,
      apis = "Target: 35  Min: 28  Compile: 36"
    )

    assertEquals(
      SnapshotTitleRenderState(
        appName = "X",
        iconContentDescription = "X",
        packageName = "com.twitter.android",
        versionInfo = "11.98.0 -> 12.5.0",
        packageSize = SnapshotTitlePackageSizeRenderState(
          text = "251 MB -> 283 MB\n+32 MB",
          breakStart = 17
        ),
        apis = "Target: 35  Min: 28  Compile: 36",
        summary = null,
        copyPrimaryText = true
      ),
      displayData.toRenderState()
    )
  }

  @Test
  fun mapsMissingPackageSizeToNullRenderState() {
    val displayData = SnapshotTitleDisplayData(
      appName = "X",
      packageName = "com.twitter.android",
      versionInfo = "12.5.0",
      packageSize = null,
      apis = "Target: 35"
    )

    assertEquals(null, displayData.toRenderState().packageSize)
  }

  @Test
  fun mapsAppNameToIconContentDescription() {
    val displayData = SnapshotTitleDisplayData(
      appName = "X",
      packageName = "com.twitter.android",
      versionInfo = "12.5.0",
      packageSize = null,
      apis = "Target: 35"
    )

    assertEquals("X", displayData.toRenderState().iconContentDescription)
  }

  @Test
  fun mapsCopyPrimaryTextDisabledToRenderState() {
    val displayData = SnapshotTitleDisplayData(
      appName = "X",
      packageName = "com.twitter.android",
      versionInfo = "12.5.0",
      packageSize = null,
      apis = "Target: 35"
    )

    assertEquals(false, displayData.toRenderState(copyPrimaryText = false).copyPrimaryText)
  }

  @Test
  fun mapsSummaryToSignedStatusCounts() {
    val displayData = SnapshotTitleDisplayData(
      appName = "Han1meViewer",
      packageName = "com.yenaly.han1meviewer",
      versionInfo = "1 -> 2",
      packageSize = null,
      apis = "Target: 35 -> 37"
    )
    val summary = SnapshotDetailSummary(
      totalCount = 7,
      totalCountText = "7 changes",
      statusCounts = listOf(
        SnapshotDetailStatusCount(
          diffType = ADDED,
          count = 2,
          countText = "2",
          label = "Added",
          status = SnapshotDetailItemStatusDisplayData(1, 2, 3)
        ),
        SnapshotDetailStatusCount(
          diffType = REMOVED,
          count = 5,
          countText = "5",
          label = "Removed",
          status = SnapshotDetailItemStatusDisplayData(4, 5, 6)
        )
      ),
      description = "7 changes, Added 2, Removed 5"
    )

    assertEquals(
      SnapshotDetailSummaryRenderState(
        totalCountText = "7 changes",
        counts = listOf(
          SnapshotDetailCountRenderState(text = "+2", colorRes = 2),
          SnapshotDetailCountRenderState(text = "−5", colorRes = 5)
        ),
        contentDescription = "7 changes, Added 2, Removed 5"
      ),
      displayData.toRenderState(summary).summary
    )
  }
}
