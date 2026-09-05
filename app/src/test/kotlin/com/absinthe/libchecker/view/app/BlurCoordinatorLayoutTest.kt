package com.absinthe.libchecker.view.app

import org.junit.Assert.assertEquals
import org.junit.Test

class BlurCoordinatorLayoutTest {

  @Test
  fun softwareCanvasDisablesRenderNodeBlurPath() {
    assertEquals(
      false,
      shouldUseRenderNodeBlur(blurEnabled = true, canvasIsHardwareAccelerated = false)
    )
    assertEquals(
      true,
      shouldUseRenderNodeBlur(blurEnabled = true, canvasIsHardwareAccelerated = true)
    )
    assertEquals(
      false,
      shouldUseRenderNodeBlur(blurEnabled = false, canvasIsHardwareAccelerated = true)
    )
  }

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
  fun appbarMasksOnlyActivateWhenContentUnderlapsBlur() {
    assertEquals(0f, appbarMaskActivation(blurEnabled = false, contentUnderlaps = true))
    assertEquals(0f, appbarMaskActivation(blurEnabled = true, contentUnderlaps = false))
    assertEquals(1f, appbarMaskActivation(blurEnabled = true, contentUnderlaps = true))
  }

  @Test
  fun appbarMaskTransitionKeepsAConstantFadeSpeedWhenReversed() {
    assertEquals(100L, appbarMaskTransitionDuration(start = 0f, target = 1f))
    assertEquals(40L, appbarMaskTransitionDuration(start = 0.7f, target = 0.3f))
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

  @Test
  fun barBackgroundFadesAgainstTheBlurLayer() {
    assertEquals(255, blurBackgroundAlpha(0f))
    assertEquals(128, blurBackgroundAlpha(0.5f))
    assertEquals(0, blurBackgroundAlpha(1f))
  }

  @Test
  fun attachedDividerFadesAsFloatingOutlineAppears() {
    assertEquals(64, fadingNavDividerAlpha(baseAlpha = 64, floatingProgress = 0f))
    assertEquals(32, fadingNavDividerAlpha(baseAlpha = 64, floatingProgress = 0.5f))
    assertEquals(0, fadingNavDividerAlpha(baseAlpha = 64, floatingProgress = 1f))
  }
}
