package com.absinthe.libchecker.ui.base

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class WindowBlurCompatControllerTest {

  @Test
  fun newWindowInheritsExistingHostBlur() {
    assertEquals(80f, inheritedBlurStartRadius(0f, listOf(40f, 80f)))
    assertEquals(64f, inheritedBlurStartRadius(64f, listOf(80f)))
    assertEquals(0f, inheritedBlurStartRadius(0f, emptyList()))
  }

  @Test
  fun lowRadiusBlurKeepsTheLiveHostAsItsSharpBaseline() {
    assertArrayEquals(
      floatArrayOf(0f, 0f, 0f),
      floatArrayOf(
        fixedSharpLayerAlpha(0f),
        fixedSharpLayerAlpha(12f),
        fixedSharpLayerAlpha(24f)
      ),
      0f
    )
  }

  @Test
  fun fixedBlurLayersAreTransparentWithoutBlur() {
    assertArrayEquals(floatArrayOf(0f, 0f, 0f, 0f), fixedBlurLayerAlphas(0f), 0f)
  }

  @Test
  fun fixedBlurLayersCrossFadeBetweenAdjacentRadii() {
    assertArrayEquals(floatArrayOf(0.5f, 0f, 0f, 0f), fixedBlurLayerAlphas(12f), 0f)
    assertArrayEquals(floatArrayOf(1f, 0.5f, 0f, 0f), fixedBlurLayerAlphas(36f), 0f)
    assertArrayEquals(floatArrayOf(0f, 0f, 1f, 0f), fixedBlurLayerAlphas(64f), 0f)
    assertArrayEquals(floatArrayOf(0f, 0f, 0f, 1f), fixedBlurLayerAlphas(80f), 0f)
  }

  @Test
  fun hostBlurOverlayBoundaryChangesAreAppliedImmediately() {
    assertEquals(true, shouldApplyHostBlurImmediately(hasOverlay = false, radius = 80f))
    assertEquals(true, shouldApplyHostBlurImmediately(hasOverlay = true, radius = 0f))
    assertEquals(false, shouldApplyHostBlurImmediately(hasOverlay = false, radius = 0f))
    assertEquals(false, shouldApplyHostBlurImmediately(hasOverlay = true, radius = 80f))
  }
}
