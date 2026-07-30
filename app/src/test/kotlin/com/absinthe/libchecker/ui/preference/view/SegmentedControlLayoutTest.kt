package com.absinthe.libchecker.ui.preference.view

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SegmentedControlLayoutTest {

  @Test
  fun `text segments shrink evenly to the available width`() {
    assertEquals(
      64,
      calculateSegmentWidth(
        preferredWidth = 80,
        segmentCount = 3,
        groupInset = 4,
        availableWidth = 200
      )
    )
  }

  @Test
  fun `segments keep their preferred width when space is available`() {
    assertEquals(
      80,
      calculateSegmentWidth(
        preferredWidth = 80,
        segmentCount = 3,
        groupInset = 4,
        availableWidth = 248
      )
    )
  }

  @Test
  fun `rtl index translations move from the physical right edge`() {
    assertEquals(-96f, segmentTranslation(index = 2, segmentWidth = 48, isRtl = true))
    assertEquals(96f, segmentTranslation(index = 2, segmentWidth = 48, isRtl = false))
  }

  @Test
  fun `pointer positions resolve in logical order for rtl`() {
    assertEquals(
      0,
      segmentIndexForPointer(
        pointerX = 124f,
        viewWidth = 152,
        segmentWidth = 48,
        segmentCount = 3,
        groupInset = 4,
        isRtl = true
      )
    )
    assertEquals(
      2,
      segmentIndexForPointer(
        pointerX = 28f,
        viewWidth = 152,
        segmentWidth = 48,
        segmentCount = 3,
        groupInset = 4,
        isRtl = true
      )
    )
  }

  @Test
  fun `invalid stored value can be repaired by choosing the visible fallback`() {
    assertTrue(
      shouldNotifySegmentSelection(
        selectedIndex = 0,
        requestedIndex = 0,
        hasValidSelectedValue = false
      )
    )
    assertFalse(
      shouldNotifySegmentSelection(
        selectedIndex = 0,
        requestedIndex = 0,
        hasValidSelectedValue = true
      )
    )
  }
}
