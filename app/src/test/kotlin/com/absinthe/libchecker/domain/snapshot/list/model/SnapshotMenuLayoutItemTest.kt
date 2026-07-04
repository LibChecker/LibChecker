package com.absinthe.libchecker.domain.snapshot.list.model

import com.absinthe.libchecker.domain.snapshot.display.SnapshotAbiDisplayData
import com.absinthe.libchecker.domain.snapshot.display.SnapshotAbiDisplayItem
import org.junit.Assert.assertEquals
import org.junit.Test

class SnapshotMenuLayoutItemTest {

  @Test
  fun buildsDemoAndOptionsLayoutItems() {
    val demoDisplayData = createSnapshotItemDisplayData("Example")

    val items = buildSnapshotMenuLayoutItems(demoDisplayData)

    assertEquals(
      listOf(
        SnapshotMenuLayoutItem.Demo(demoDisplayData),
        SnapshotMenuLayoutItem.Options
      ),
      items
    )
  }

  @Test
  fun replacesDemoDisplayDataWithoutChangingOptionsItem() {
    val initialDisplayData = createSnapshotItemDisplayData("Example")
    val updatedDisplayData = createSnapshotItemDisplayData("Example updated")

    val items = buildSnapshotMenuLayoutItems(initialDisplayData)
      .replaceSnapshotMenuDemoDisplayData(updatedDisplayData)

    assertEquals(
      listOf(
        SnapshotMenuLayoutItem.Demo(updatedDisplayData),
        SnapshotMenuLayoutItem.Options
      ),
      items
    )
  }

  private fun createSnapshotItemDisplayData(appName: String): SnapshotItemDisplayData {
    return SnapshotItemDisplayData(
      cardPresentation = SnapshotItemCardPresentation.Rounded,
      iconSource = null,
      packageName = "com.example",
      appName = SnapshotItemAppNameDisplayData(
        text = appName,
        showTrackIcon = false,
        packageStateLabel = null
      ),
      isNewInstalled = false,
      isDeleted = false,
      stateIndicator = SnapshotItemStateIndicatorData(
        added = 0,
        removed = 0,
        changed = 0,
        moved = 0,
        stateDescription = "",
        animate = true
      ),
      versionInfo = "1.0",
      packageSize = null,
      apiText = "Target: 37",
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
      updateTimeDisplayData = null,
      highlightText = "",
      contentDescription = appName
    )
  }
}
