package com.absinthe.libchecker.domain.snapshot.list.model

import com.absinthe.libchecker.domain.snapshot.display.SnapshotAbiDisplayData
import com.absinthe.libchecker.domain.snapshot.display.SnapshotAbiDisplayItem
import com.absinthe.libchecker.domain.snapshot.display.SnapshotUpdateTimeDisplayData
import com.absinthe.libchecker.domain.snapshot.display.SnapshotUpdateTimeText
import org.junit.Assert.assertEquals
import org.junit.Test

class SnapshotItemDisplayDataDescriptionTest {

  @Test
  fun displayDataCarriesContentDescription() {
    val displayData = SnapshotItemDisplayData(
      cardPresentation = SnapshotItemCardPresentation.Normal,
      iconSource = null,
      packageName = "com.twitter.android",
      appName = SnapshotItemAppNameDisplayData(
        text = "X",
        showTrackIcon = false,
        packageStateLabel = null
      ),
      isNewInstalled = false,
      isDeleted = false,
      stateIndicator = SnapshotItemStateIndicatorData(
        added = 1,
        removed = 0,
        changed = 2,
        moved = 0,
        stateDescription = "Added 1, Changed 2",
        animate = false
      ),
      versionInfo = "12.5.0",
      packageSize = SnapshotItemPackageSizeDisplayData(
        text = "251 MB -> 283 MB",
        breakStart = -1
      ),
      apiText = "Target: 35",
      abi = SnapshotItemAbiDisplayData(
        abiDisplayData = SnapshotAbiDisplayData(
          old = SnapshotAbiDisplayItem(
            text = "arm64-v8a",
            badgeRes = null,
            isMultiArch = false
          ),
          new = null
        ),
        showChangedAbi = false,
        tintChangedAbiBadge = false
      ),
      updateTimeDisplayData = SnapshotUpdateTimeDisplayData(
        text = SnapshotUpdateTimeText.LastUpdated("2026-06-11 20:26:22"),
        isApexPackage = false
      ),
      highlightText = "",
      contentDescription = "X, com.twitter.android, 12.5.0"
    )

    assertEquals("X, com.twitter.android, 12.5.0", displayData.contentDescription)
  }

  @Test
  fun buildsDescriptionFromRenderableTextParts() {
    assertEquals(
      "X, com.twitter.android, 12.5.0, 251 MB -> 283 MB, Target: 35, arm64-v8a, Last updated: 2026-06-11 20:26:22, Added 1",
      buildSnapshotItemDescription(
        appName = "X",
        packageName = "com.twitter.android",
        versionInfo = "12.5.0",
        packageSize = "251 MB -> 283 MB",
        apiText = "Target: 35",
        abiText = "arm64-v8a",
        updateTime = "Last updated: 2026-06-11 20:26:22",
        stateDescription = "Added 1"
      )
    )
  }

  @Test
  fun skipsBlankDescriptionParts() {
    assertEquals(
      "X, com.twitter.android, Target: 35",
      buildSnapshotItemDescription(
        appName = "X",
        packageName = "com.twitter.android",
        versionInfo = " ",
        packageSize = null,
        apiText = "Target: 35",
        abiText = "",
        updateTime = null,
        stateDescription = ""
      )
    )
  }

  @Test
  fun buildsChangedAbiDescriptionFromAbiText() {
    assertEquals(
      "arm64-v8a → armeabi-v7a",
      buildSnapshotItemAbiDescription(
        SnapshotItemAbiDisplayData(
          abiDisplayData = SnapshotAbiDisplayData(
            old = SnapshotAbiDisplayItem(
              text = "arm64-v8a",
              badgeRes = null,
              isMultiArch = false
            ),
            new = SnapshotAbiDisplayItem(
              text = "armeabi-v7a",
              badgeRes = null,
              isMultiArch = false
            )
          ),
          showChangedAbi = true,
          tintChangedAbiBadge = false
        )
      )
    )
  }
}
