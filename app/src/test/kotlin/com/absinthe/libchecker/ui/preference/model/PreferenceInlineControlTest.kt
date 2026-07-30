package com.absinthe.libchecker.ui.preference.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PreferenceInlineControlTest {

  @Test
  fun `icon segmented choice requires one icon and label per value`() {
    assertThrows(IllegalArgumentException::class.java) {
      PreferenceInlineControl.IconSegmentedChoice(
        accessibilityLabels = listOf("Light", "Dark", "System"),
        entryValues = listOf("off", "on", "system"),
        iconResIds = listOf(1, 2),
        selectedValue = "system"
      )
    }
  }

  @Test
  fun `draggable choice requires matching non-empty entries and values`() {
    assertThrows(IllegalArgumentException::class.java) {
      PreferenceInlineControl.DraggableChoice(
        entries = listOf("Default"),
        entryValues = emptyList(),
        selectedValue = null
      )
    }
  }

  @Test
  fun `range preserves a valid selected value`() {
    val range = PreferenceInlineControl.Range(
      value = 2,
      valueFrom = 1,
      valueTo = 50
    )

    assertEquals(2, range.value)
  }

  @Test
  fun `range rejects a selected value outside its bounds`() {
    assertThrows(IllegalArgumentException::class.java) {
      PreferenceInlineControl.Range(
        value = 51,
        valueFrom = 1,
        valueTo = 50
      )
    }
  }
}
