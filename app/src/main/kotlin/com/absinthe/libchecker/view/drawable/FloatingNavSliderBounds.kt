package com.absinthe.libchecker.view.drawable

data class FloatingNavSliderBounds(
  val left: Float,
  val top: Float,
  val right: Float,
  val bottom: Float
)

fun calculateFloatingNavSliderBounds(
  containerWidth: Float,
  containerHeight: Float,
  position: Float,
  spacing: Float,
  itemCount: Int = 4,
  progress: Float = 1f,
  normalWidth: Float = 0f,
  normalHeight: Float = 0f,
  normalTop: Float = (containerHeight - normalHeight) / 2f,
  normalLeft: Float = Float.NaN,
  vertical: Boolean = false
): FloatingNavSliderBounds {
  if (containerWidth <= 0f || containerHeight <= 0f || itemCount <= 0) {
    return FloatingNavSliderBounds(0f, 0f, 0f, 0f)
  }
  val clampedPos = position.coerceIn(0f, (itemCount - 1).toFloat())
  val clampedProgress = progress.coerceIn(0f, 1f)
  val itemWidth = if (vertical) containerWidth else containerWidth / itemCount
  val itemHeight = if (vertical) containerWidth.coerceAtMost(containerHeight) else containerHeight
  val targetWidth = (itemWidth - 2f * spacing).coerceAtLeast(0f)
  val targetHeight = (itemHeight - 2f * spacing).coerceAtLeast(0f)
  val sliderWidth = lerp(normalWidth, targetWidth, clampedProgress).coerceAtLeast(0f)
  val sliderHeight = lerp(normalHeight, targetHeight, clampedProgress).coerceAtLeast(0f)
  val targetCenterX = if (vertical) containerWidth / 2f else (clampedPos + 0.5f) * itemWidth
  val targetCenterY = if (vertical) {
    val firstCenter = containerWidth / 2f
    val stride = if (itemCount == 1) 0f else (containerHeight - containerWidth).coerceAtLeast(0f) / (itemCount - 1)
    firstCenter + clampedPos * stride
  } else {
    containerHeight / 2f
  }
  val resolvedNormalLeft = normalLeft.takeUnless { it.isNaN() } ?: targetCenterX - normalWidth / 2f
  val left = lerp(resolvedNormalLeft, targetCenterX - targetWidth / 2f, clampedProgress)
  val top = lerp(normalTop, targetCenterY - targetHeight / 2f, clampedProgress)
  return FloatingNavSliderBounds(
    left = left,
    top = top,
    right = (left + sliderWidth).coerceAtLeast(left),
    bottom = (top + sliderHeight).coerceAtLeast(top)
  )
}

private fun lerp(start: Float, end: Float, progress: Float): Float = start + (end - start) * progress
