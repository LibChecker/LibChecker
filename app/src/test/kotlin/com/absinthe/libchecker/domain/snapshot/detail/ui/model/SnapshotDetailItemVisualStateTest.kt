package com.absinthe.libchecker.domain.snapshot.detail.ui.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SnapshotDetailItemVisualStateTest {

  @Test
  fun resolvesLightThemeSurfacesWithoutTintingTheWholeItem() {
    val colors = resolveSnapshotDetailItemColors(
      theme = SnapshotDetailThemeColors(
        surface = 1,
        onSurface = 2,
        onSurfaceVariant = 3,
        outlineVariant = 4,
        chipSurface = 5
      ),
      statusColor = 6
    )

    assertEquals(1, colors.surface)
    assertEquals(2, colors.title)
    assertEquals(3, colors.supportingText)
    assertEquals(4, colors.divider)
    assertEquals(6, colors.status)
    assertEquals(5, colors.chipSurface)
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
        outlineVariant = 14,
        chipSurface = 15
      ),
      statusColor = 16
    )

    assertEquals(11, colors.surface)
    assertEquals(12, colors.title)
    assertEquals(13, colors.supportingText)
    assertEquals(14, colors.divider)
    assertEquals(16, colors.status)
    assertEquals(15, colors.chipSurface)
  }

  @Test
  fun keepsOrdinaryStatusAndShortTitleOnOneLine() {
    val plan = planSnapshotDetailItemLayout(
      contentWidth = 300,
      statusClusterWidth = 72,
      naturalTitleWidth = 100,
      chipWidth = 54,
      titleGap = 12,
      chipGap = 6,
      minimumTitleWidth = 112
    )

    assertFalse(plan.titleStartsOnNewLine)
    assertEquals(216, plan.titleWidth)
    assertTrue(plan.chipOnTitleLine)
  }

  @Test
  fun movesTitleBelowLongLocalizedStatusWithoutNegativeWidth() {
    val plan = planSnapshotDetailItemLayout(
      contentWidth = 300,
      statusClusterWidth = 190,
      naturalTitleWidth = 140,
      chipWidth = 54,
      titleGap = 12,
      chipGap = 6,
      minimumTitleWidth = 112
    )

    assertTrue(plan.titleStartsOnNewLine)
    assertEquals(300, plan.titleWidth)
  }

  @Test
  fun movesRuleChipBelowWrappedClassName() {
    val plan = planSnapshotDetailItemLayout(
      contentWidth = 300,
      statusClusterWidth = 72,
      naturalTitleWidth = 500,
      chipWidth = 54,
      titleGap = 12,
      chipGap = 6,
      minimumTitleWidth = 112
    )

    assertFalse(plan.chipOnTitleLine)
  }
}
