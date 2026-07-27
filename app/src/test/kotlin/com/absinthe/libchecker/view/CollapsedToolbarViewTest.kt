package com.absinthe.libchecker.view

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CollapsedToolbarViewTest {

  @Test
  fun visibilityUsesHysteresisAroundCollapseThreshold() {
    assertFalse(resolveCollapsedToolbarVisibility(0.67f, currentlyRevealed = false))
    assertTrue(resolveCollapsedToolbarVisibility(0.68f, currentlyRevealed = false))
    assertTrue(resolveCollapsedToolbarVisibility(0.60f, currentlyRevealed = true))
    assertFalse(resolveCollapsedToolbarVisibility(0.52f, currentlyRevealed = true))
  }

  @Test
  fun motionMovesUpInSyncWithNonlinearBlur() {
    assertEquals(
      CollapsedToolbarMotion(alpha = 0f, translationFraction = 1f, blurFraction = 1f),
      calculateCollapsedToolbarMotion(0f)
    )
    assertEquals(
      0.75f,
      calculateCollapsedToolbarMotion(0.5f).alpha,
      0.0001f
    )
    assertEquals(
      0.7071f,
      calculateCollapsedToolbarMotion(0.5f).translationFraction,
      0.0001f
    )
    assertEquals(
      0.7071f,
      calculateCollapsedToolbarMotion(0.5f).blurFraction,
      0.0001f
    )
    assertEquals(
      CollapsedToolbarMotion(alpha = 1f, translationFraction = 0f, blurFraction = 0f),
      calculateCollapsedToolbarMotion(1f)
    )
  }

  @Test
  fun motionClampsProgressToValidRange() {
    assertEquals(
      calculateCollapsedToolbarMotion(0f),
      calculateCollapsedToolbarMotion(-1f)
    )
    assertEquals(
      calculateCollapsedToolbarMotion(1f),
      calculateCollapsedToolbarMotion(2f)
    )
  }
}
