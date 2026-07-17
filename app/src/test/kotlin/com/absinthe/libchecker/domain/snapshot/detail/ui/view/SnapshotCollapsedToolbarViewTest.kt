package com.absinthe.libchecker.domain.snapshot.detail.ui.view

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SnapshotCollapsedToolbarViewTest {

  @Test
  fun visibilityUsesHysteresisAroundCollapseThreshold() {
    assertFalse(resolveSnapshotCollapsedToolbarVisibility(0.67f, currentlyRevealed = false))
    assertTrue(resolveSnapshotCollapsedToolbarVisibility(0.68f, currentlyRevealed = false))
    assertTrue(resolveSnapshotCollapsedToolbarVisibility(0.60f, currentlyRevealed = true))
    assertFalse(resolveSnapshotCollapsedToolbarVisibility(0.52f, currentlyRevealed = true))
  }

  @Test
  fun motionMovesUpInSyncWithNonlinearBlur() {
    assertEquals(
      SnapshotCollapsedToolbarMotion(alpha = 0f, translationFraction = 1f, blurFraction = 1f),
      calculateSnapshotCollapsedToolbarMotion(0f)
    )
    assertEquals(
      0.75f,
      calculateSnapshotCollapsedToolbarMotion(0.5f).alpha,
      0.0001f
    )
    assertEquals(
      0.7071f,
      calculateSnapshotCollapsedToolbarMotion(0.5f).translationFraction,
      0.0001f
    )
    assertEquals(
      0.7071f,
      calculateSnapshotCollapsedToolbarMotion(0.5f).blurFraction,
      0.0001f
    )
    assertEquals(
      SnapshotCollapsedToolbarMotion(alpha = 1f, translationFraction = 0f, blurFraction = 0f),
      calculateSnapshotCollapsedToolbarMotion(1f)
    )
  }

  @Test
  fun motionClampsProgressToValidRange() {
    assertEquals(
      calculateSnapshotCollapsedToolbarMotion(0f),
      calculateSnapshotCollapsedToolbarMotion(-1f)
    )
    assertEquals(
      calculateSnapshotCollapsedToolbarMotion(1f),
      calculateSnapshotCollapsedToolbarMotion(2f)
    )
  }
}
