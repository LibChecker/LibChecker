package com.absinthe.libchecker.domain.home.ui

import com.absinthe.libchecker.view.drawable.calculateFloatingNavSliderBounds
import org.junit.Assert.assertEquals
import org.junit.Test

class FloatingNavGeometryTest {

  private val maxHorizontalMargin = 48
  private val systemBarBottomInset = 72

  @Test
  fun `geometry interpolates from attached to floating state`() {
    listOf(
      0f to FloatingNavGeometry(0, 0, systemBarBottomInset),
      0.5f to FloatingNavGeometry(24, 36, 36),
      1f to FloatingNavGeometry(maxHorizontalMargin, systemBarBottomInset, 0)
    ).forEach { (progress, expected) ->
      val actual = calculateFloatingNavGeometry(progress, maxHorizontalMargin, systemBarBottomInset)
      assertEquals("progress=$progress", expected, actual)
      assertEquals(systemBarBottomInset, actual.bottomMargin + actual.bottomPadding)
    }
  }

  @Test
  fun `zero inset produces zero bottom margin and padding`() {
    listOf(0f, 0.5f, 1f).forEach { progress ->
      val geometry = calculateFloatingNavGeometry(
        progress = progress,
        maxHorizontalMargin = maxHorizontalMargin,
        systemBarBottomInset = 0
      )
      assertEquals(0, geometry.bottomMargin)
      assertEquals(0, geometry.bottomPadding)
    }
  }

  @Test
  fun `progress is clamped to 0 and 1`() {
    listOf(
      -0.5f to FloatingNavGeometry(0, 0, systemBarBottomInset),
      1.5f to FloatingNavGeometry(maxHorizontalMargin, systemBarBottomInset, 0)
    ).forEach { (progress, expected) ->
      assertEquals(expected, calculateFloatingNavGeometry(progress, maxHorizontalMargin, systemBarBottomInset))
    }
  }

  @Test
  fun `floating rail height follows expressive item tokens and available space`() {
    assertEquals(288, calculateFloatingNavigationRailHeight(4, 68, 4, 72, 400))
    assertEquals(240, calculateFloatingNavigationRailHeight(4, 68, 4, 72, 240))
  }

  @Test
  fun `slider bounds at tab 0 has uniform distance to container edges`() {
    val containerWidth = 380f
    val containerHeight = 66f
    val spacing = 4f
    val bounds = calculateFloatingNavSliderBounds(
      containerWidth = containerWidth,
      containerHeight = containerHeight,
      position = 0f,
      spacing = spacing,
      itemCount = 4
    )

    assertEquals(spacing, bounds.left, 0.001f)
    assertEquals(spacing, bounds.top, 0.001f)
    assertEquals(containerHeight - spacing, bounds.bottom, 0.001f)
  }

  @Test
  fun `slider bounds at tab 3 has uniform distance to right edge`() {
    val containerWidth = 380f
    val containerHeight = 66f
    val spacing = 4f
    val bounds = calculateFloatingNavSliderBounds(
      containerWidth = containerWidth,
      containerHeight = containerHeight,
      position = 3f,
      spacing = spacing,
      itemCount = 4
    )

    assertEquals(containerWidth - spacing, bounds.right, 0.001f)
    assertEquals(spacing, bounds.top, 0.001f)
    assertEquals(containerHeight - spacing, bounds.bottom, 0.001f)
  }

  @Test
  fun `slider bounds between adjacent tabs have uniform 2x spacing gap`() {
    val containerWidth = 380f
    val containerHeight = 66f
    val spacing = 4f
    val bounds = (0..3).map {
      calculateFloatingNavSliderBounds(containerWidth, containerHeight, it.toFloat(), spacing, 4)
    }
    bounds.zipWithNext().forEach { (left, right) ->
      assertEquals(2f * spacing, right.left - left.right, 0.001f)
    }
  }

  @Test
  fun `vertical slider stays concentric with both rail caps`() {
    val first = calculateFloatingNavSliderBounds(72f, 288f, 0f, 4f, 4, vertical = true)
    val last = calculateFloatingNavSliderBounds(72f, 288f, 3f, 4f, 4, vertical = true)

    assertEquals(4f, first.left, 0.001f)
    assertEquals(4f, first.top, 0.001f)
    assertEquals(4f, 72f - first.right, 0.001f)
    assertEquals(first.right - first.left, first.bottom - first.top, 0.001f)
    assertEquals(4f, 288f - last.bottom, 0.001f)
  }

  @Test
  fun `slider morphs from native indicator into floating segment`() {
    val bounds = calculateFloatingNavSliderBounds(
      containerWidth = 400f,
      containerHeight = 80f,
      position = 0f,
      spacing = 4f,
      progress = 0.5f,
      normalWidth = 64f,
      normalHeight = 32f,
      normalTop = 12f
    )

    assertEquals(11f, bounds.left, 0.001f)
    assertEquals(89f, bounds.right, 0.001f)
    assertEquals(8f, bounds.top, 0.001f)
    assertEquals(60f, bounds.bottom, 0.001f)
  }
}
