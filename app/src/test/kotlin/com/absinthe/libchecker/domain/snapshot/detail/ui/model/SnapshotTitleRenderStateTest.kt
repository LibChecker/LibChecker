package com.absinthe.libchecker.domain.snapshot.detail.ui.model

import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotTitleDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotTitlePackageSizeData
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
}
