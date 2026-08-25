package com.absinthe.libchecker.view.app

import org.junit.Assert.assertEquals
import org.junit.Test

class BlurCoordinatorLayoutTest {

  @Test
  fun appbarBackdropStaysFullResolutionWhileNavRemainsDownsampled() {
    assertEquals(1, APPBAR_BACKDROP_DOWNSAMPLE)
    assertEquals(2, NAV_BACKDROP_DOWNSAMPLE)
    assertEquals(
      26.25f,
      calculateDownsampledBlurRadius(
        blurRadiusDp = 10f,
        density = 2.625f,
        downsample = APPBAR_BACKDROP_DOWNSAMPLE
      ),
      0.0001f
    )
  }

  @Test
  fun appbarSurfaceTintFadesWithTheProgressiveBlur() {
    assertEquals(0.3f, progressiveSurfaceTintAlpha(progress = 0f), 0.0001f)
    assertEquals(0.2432f, progressiveSurfaceTintAlpha(progress = 0.5f), 0.0001f)
    assertEquals(0f, progressiveSurfaceTintAlpha(progress = 1f), 0.0001f)
  }

  @Test
  fun appbarDarkMaskOnlyActivatesWhenContentUnderlapsBlur() {
    assertEquals(0f, appbarDarkMaskActivation(blurEnabled = false, contentUnderlaps = true))
    assertEquals(0f, appbarDarkMaskActivation(blurEnabled = true, contentUnderlaps = false))
    assertEquals(1f, appbarDarkMaskActivation(blurEnabled = true, contentUnderlaps = true))
  }

  @Test
  fun appbarDarkMaskFadesTowardTheAppbarBottom() {
    assertEquals(0.12f, progressiveDarkMaskAlpha(progress = 0f), 0.0001f)
    assertEquals(0.0973f, progressiveDarkMaskAlpha(progress = 0.5f), 0.0001f)
    assertEquals(0f, progressiveDarkMaskAlpha(progress = 1f), 0.0001f)
  }

  @Test
  fun backdropBlurKeepsVisibleContentOutsideTheBlurKernelBoundary() {
    val contentOffset = calculateBackdropContentOffset(maxBlurRadius = 20f)

    assertEquals(60, contentOffset)
    assertEquals(726, calculateBackdropTextureHeight(606, contentOffset))
  }

  @Test
  fun backdropBackgroundIsAlwaysOpaque() {
    assertEquals(0xFF112233.toInt(), opaqueBackdropColor(0x66112233))
  }
}
