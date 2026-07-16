package com.absinthe.libchecker.domain.snapshot.list.model

import com.absinthe.libchecker.constant.options.SnapshotOptions
import com.absinthe.libchecker.domain.snapshot.display.SnapshotAbiDisplayData
import com.absinthe.libchecker.domain.snapshot.display.SnapshotAbiDisplayItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SnapshotMenuBottomSheetStateTest {

  @Test
  fun `builds supported options and current checked state`() {
    val state = buildSnapshotMenuBottomSheetState(
      currentOptions = SnapshotOptions.SHOW_UPDATE_TIME,
      demoDisplayData = createSnapshotItemDisplayData(),
      includeIecUnits = true
    )

    assertEquals(
      listOf(
        SnapshotOptions.SHOW_UPDATE_TIME,
        SnapshotOptions.HIDE_NO_COMPONENT_CHANGES,
        SnapshotOptions.DIFF_HIGHLIGHT,
        SnapshotOptions.USE_IEC_UNITS
      ),
      state.options.map { it.option }
    )
    assertTrue(state.options.first().isChecked)
    assertFalse(state.options.last().isChecked)
  }

  @Test
  fun `omits IEC units when unsupported`() {
    val state = buildSnapshotMenuBottomSheetState(
      currentOptions = SnapshotOptions.USE_IEC_UNITS,
      demoDisplayData = createSnapshotItemDisplayData(),
      includeIecUnits = false
    )

    assertFalse(state.options.any { it.option == SnapshotOptions.USE_IEC_UNITS })
  }

  private fun createSnapshotItemDisplayData(): SnapshotItemDisplayData {
    return SnapshotItemDisplayData(
      cardPresentation = SnapshotItemCardPresentation.Rounded,
      iconSource = null,
      packageName = "com.example",
      appName = SnapshotItemAppNameDisplayData(
        text = "Example",
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
      contentDescription = "Example"
    )
  }
}
