package com.absinthe.libchecker.ui.base

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class WindowBlurCompatControllerTest {

  @Test
  fun sharpLayerFadesIntoLiveContentAtLowRadius() {
    assertArrayEquals(
      floatArrayOf(0f, 0.5f, 1f),
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
}
