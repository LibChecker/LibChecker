package com.absinthe.libchecker.ui.preference.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreferenceItemGroupPositionTest {

  @Test
  fun `group position follows adjacent items`() {
    assertEquals(PreferenceItemGroupPosition.SINGLE, position(false, false))
    assertEquals(PreferenceItemGroupPosition.FIRST, position(false, true))
    assertEquals(PreferenceItemGroupPosition.MIDDLE, position(true, true))
    assertEquals(PreferenceItemGroupPosition.LAST, position(true, false))
  }

  @Test
  fun `outer corner flags match group position`() {
    assertTrue(PreferenceItemGroupPosition.SINGLE.usesOuterTopCorners)
    assertTrue(PreferenceItemGroupPosition.SINGLE.usesOuterBottomCorners)
    assertTrue(PreferenceItemGroupPosition.FIRST.usesOuterTopCorners)
    assertFalse(PreferenceItemGroupPosition.FIRST.usesOuterBottomCorners)
    assertFalse(PreferenceItemGroupPosition.MIDDLE.usesOuterTopCorners)
    assertFalse(PreferenceItemGroupPosition.MIDDLE.usesOuterBottomCorners)
    assertFalse(PreferenceItemGroupPosition.LAST.usesOuterTopCorners)
    assertTrue(PreferenceItemGroupPosition.LAST.usesOuterBottomCorners)
  }

  private fun position(
    hasPreviousItem: Boolean,
    hasNextItem: Boolean
  ): PreferenceItemGroupPosition {
    return PreferenceItemGroupPosition.from(hasPreviousItem, hasNextItem)
  }
}
