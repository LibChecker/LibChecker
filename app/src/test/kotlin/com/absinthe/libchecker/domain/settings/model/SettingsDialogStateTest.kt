package com.absinthe.libchecker.domain.settings.model

import com.absinthe.libchecker.domain.app.update.AppUpdateChannel
import com.absinthe.libchecker.domain.rules.CloudRulesVersionInfo
import com.absinthe.libchecker.domain.snapshot.display.SnapshotAbiDisplayData
import com.absinthe.libchecker.domain.snapshot.display.SnapshotAbiDisplayItem
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotItemAbiDisplayData
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotItemAppNameDisplayData
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotItemCardPresentation
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotItemDisplayData
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotItemStateIndicatorData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsDialogStateTest {

  @Test
  fun `in app update state retains preview while installing and clears it for a new channel`() {
    val item = buildSnapshotItemDisplayData()
    val initial = InAppUpdateDialogState(
      selectedChannel = AppUpdateChannel.STABLE,
      content = InAppUpdateDialogContent.Loading(),
      isChannelSelectionEnabled = true,
      isUpdateEnabled = false
    )

    val ready = initial.showContent(item, hasUpdate = true)
    val installing = ready.showInstallProgress()
    val switched = installing.selectChannel(AppUpdateChannel.CI)

    assertSame(item, (ready.content as InAppUpdateDialogContent.Ready).item)
    assertTrue(ready.isUpdateEnabled)
    assertSame(item, (installing.content as InAppUpdateDialogContent.Loading).retainedItem)
    assertFalse(installing.isUpdateEnabled)
    assertEquals(AppUpdateChannel.CI, switched.selectedChannel)
    assertEquals(InAppUpdateDialogContent.Loading(), switched.content)
    assertFalse(switched.isChannelSelectionEnabled)
  }

  @Test
  fun `cloud rule versions map to update availability`() {
    assertEquals(
      CloudRulesDialogState.Content(
        localVersion = 8,
        remoteVersion = 9,
        updateAvailable = true
      ),
      CloudRulesVersionInfo(localVersion = 8, remoteVersion = 9).toCloudRulesDialogState()
    )
  }

  private fun buildSnapshotItemDisplayData(): SnapshotItemDisplayData {
    return SnapshotItemDisplayData(
      cardPresentation = SnapshotItemCardPresentation.Rounded,
      iconSource = null,
      packageName = "sample.package",
      appName = SnapshotItemAppNameDisplayData(
        text = "Sample",
        showTrackIcon = false,
        packageStateLabel = null
      ),
      isNewInstalled = false,
      isDeleted = false,
      stateIndicator = SnapshotItemStateIndicatorData(
        added = 0,
        removed = 0,
        changed = 1,
        moved = 0,
        stateDescription = "Changed",
        animate = false
      ),
      versionInfo = "1 -> 2",
      packageSize = null,
      apiText = "Target 37",
      abi = SnapshotItemAbiDisplayData(
        abiDisplayData = SnapshotAbiDisplayData(
          old = SnapshotAbiDisplayItem("arm64-v8a", badgeRes = null, isMultiArch = false),
          new = null
        ),
        showChangedAbi = false,
        tintChangedAbiBadge = false
      ),
      updateTimeDisplayData = null,
      highlightText = "",
      contentDescription = "Sample"
    )
  }
}
