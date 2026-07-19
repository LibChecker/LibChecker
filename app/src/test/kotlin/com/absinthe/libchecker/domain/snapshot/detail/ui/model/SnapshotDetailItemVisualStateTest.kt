package com.absinthe.libchecker.domain.snapshot.detail.ui.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SnapshotDetailItemVisualStateTest {

  @Test
  fun resolvesLightThemeWithSubtleStatusGradient() {
    val colors = resolveSnapshotDetailItemColors(
      theme = SnapshotDetailThemeColors(
        surface = 1,
        onSurface = 2,
        onSurfaceVariant = 3,
        outlineVariant = 4
      ),
      statusColor = 6
    )

    assertEquals(1, colors.surface)
    assertEquals(1, colors.gradientStart)
    assertEquals(1, colors.gradientMiddle)
    assertEquals(2, colors.title)
    assertEquals(3, colors.supportingText)
    assertEquals(2, colors.divider)
    assertEquals(6, colors.status)
    assertEquals(3, colors.chipText)
    assertEquals(4, colors.chipOutline)
  }

  @Test
  fun resolvesDarkThemeFromTheSuppliedThemePalette() {
    val colors = resolveSnapshotDetailItemColors(
      theme = SnapshotDetailThemeColors(
        surface = 11,
        onSurface = 12,
        onSurfaceVariant = 13,
        outlineVariant = 14
      ),
      statusColor = 16
    )

    assertEquals(11, colors.surface)
    assertEquals(12, colors.title)
    assertEquals(13, colors.supportingText)
    assertEquals(12, colors.divider)
    assertEquals(16, colors.status)
  }

  @Test
  fun blendsStatusIntoRowWithoutChangingTheOpaqueBase() {
    val colors = resolveSnapshotDetailItemColors(
      theme = SnapshotDetailThemeColors(
        surface = 0xFF000000.toInt(),
        onSurface = 1,
        onSurfaceVariant = 2,
        outlineVariant = 3
      ),
      statusColor = 0xFFFFFFFF.toInt()
    )

    assertEquals(0xFF191919.toInt(), colors.gradientStart)
    assertEquals(0xFF0A0A0A.toInt(), colors.gradientMiddle)
    assertEquals(0xFF000000.toInt(), colors.surface)
  }

  @Test
  fun softensDividerAgainstTheRowSurface() {
    assertEquals(
      0xFF8C8C8C.toInt(),
      resolveSnapshotDetailDividerColor(
        surface = 0xFF000000.toInt(),
        outlineVariant = 0xFFFFFFFF.toInt()
      )
    )
  }

  @Test
  fun placesRuleChipAfterStatusWhenStatusRowFits() {
    val plan = planSnapshotDetailItemLayout(
      contentWidth = 300,
      naturalStatusWidth = 80,
      chipWidth = 54,
      chipGap = 6
    )

    assertEquals(300, plan.contentWidth)
    assertTrue(plan.chipOnStatusLine)
  }

  @Test
  fun clampsContentWidthAtZero() {
    val plan = planSnapshotDetailItemLayout(
      contentWidth = -1,
      naturalStatusWidth = 80,
      chipWidth = 54,
      chipGap = 6
    )

    assertEquals(0, plan.contentWidth)
    assertFalse(plan.chipOnStatusLine)
  }

  @Test
  fun movesRuleChipBelowWhenStatusRowCannotFitIt() {
    val plan = planSnapshotDetailItemLayout(
      contentWidth = 300,
      naturalStatusWidth = 260,
      chipWidth = 54,
      chipGap = 6
    )

    assertFalse(plan.chipOnStatusLine)
  }
}
