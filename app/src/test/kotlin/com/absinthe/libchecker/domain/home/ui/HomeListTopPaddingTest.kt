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
}
