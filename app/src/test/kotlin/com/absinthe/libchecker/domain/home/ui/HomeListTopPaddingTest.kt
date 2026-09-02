package com.absinthe.libchecker.domain.home.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeListTopPaddingTest {

  @Test
  fun appbarInsetIsAddedOnlyWhileProgressiveBlurIsActive() {
    assertEquals(
      216,
      calculateHomeListTopPadding(
        initialPaddingTop = 16,
        appbarBottom = 200,
        progressiveBlurActive = true
      )
    )
    assertEquals(
      16,
      calculateHomeListTopPadding(
        initialPaddingTop = 16,
        appbarBottom = 200,
        progressiveBlurActive = false
      )
    )
  }

  @Test
  fun preLayoutInsetUsesSystemBarAndActionBarUntilAppbarIsMeasured() {
    assertEquals(
      88,
      resolveHomeListAppbarInset(
        appbarBottom = 0,
        actionBarHeight = 64,
        systemBarTopInset = 24
      )
    )
    assertEquals(
      92,
      resolveHomeListAppbarInset(
        appbarBottom = 92,
        actionBarHeight = 64,
        systemBarTopInset = 24
      )
    )
  }

  @Test
  fun maskWaitsUntilARealListItemMovesUnderTheAppbar() {
    assertEquals(false, isListItemUnderAppbar(appbarBottom = 200, firstListItemTop = null))
    assertEquals(false, isListItemUnderAppbar(appbarBottom = 200, firstListItemTop = 240))
    assertEquals(false, isListItemUnderAppbar(appbarBottom = 200, firstListItemTop = 200))
    assertEquals(true, isListItemUnderAppbar(appbarBottom = 200, firstListItemTop = 199))
  }

  @Test
  fun hdrRequiresEveryRenderingPrerequisite() {
    assertEquals(
      false,
      shouldEnableToolbarHdrHighlight(
        blurEnabled = false,
        contentUnderlaps = false,
        darkModeEnabled = true
      )
    )
    assertEquals(
      false,
      shouldEnableToolbarHdrHighlight(
        blurEnabled = false,
        contentUnderlaps = true,
        darkModeEnabled = true
      )
    )
    assertEquals(
      false,
      shouldEnableToolbarHdrHighlight(
        blurEnabled = true,
        contentUnderlaps = false,
        darkModeEnabled = true
      )
    )
    assertEquals(
      true,
      shouldEnableToolbarHdrHighlight(
        blurEnabled = true,
        contentUnderlaps = true,
        darkModeEnabled = true
      )
    )
    assertEquals(
      false,
      shouldEnableToolbarHdrHighlight(
        blurEnabled = true,
        contentUnderlaps = true,
        darkModeEnabled = false
      )
    )
  }

  @Test
  fun hdrStaysDisabledWhileSwitchingHomePages() {
    assertEquals(
      false,
      shouldEnableToolbarHdrHighlight(
        blurEnabled = true,
        contentUnderlaps = true,
        darkModeEnabled = true,
        pageTransitionRunning = true
      )
    )
  }

  @Test
  fun blurToggleKeepsTheVisibleListAnchorAtTheSameScreenPosition() {
    assertEquals(
      320,
      recyclerAnchorScrollCorrection(previousScreenTop = 1603, currentScreenTop = 1923)
    )
    assertEquals(
      -320,
      recyclerAnchorScrollCorrection(previousScreenTop = 1923, currentScreenTop = 1603)
    )
    assertEquals(
      0,
      recyclerAnchorScrollCorrection(previousScreenTop = 1603, currentScreenTop = 1603)
    )
  }

  @Test
  fun blurToggleWaitsForTheExpectedListViewportGeometry() {
    assertEquals(
      0,
      expectedAppbarScrollTargetScreenTop(
        rootScreenTop = 0,
        appbarScreenTop = 0,
        appbarHeight = 320,
        progressiveBlurActive = true
      )
    )
    assertEquals(
      320,
      expectedAppbarScrollTargetScreenTop(
        rootScreenTop = 0,
        appbarScreenTop = 0,
        appbarHeight = 320,
        progressiveBlurActive = false
      )
    )
  }
}
