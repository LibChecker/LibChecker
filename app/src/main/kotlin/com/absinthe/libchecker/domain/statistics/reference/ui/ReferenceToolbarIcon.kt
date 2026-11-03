package com.absinthe.libchecker.domain.statistics.reference.ui

import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import kotlin.math.roundToInt

internal fun Drawable.boundedReferenceIcon(maxSize: Int): Drawable {
  val width = intrinsicWidth.takeIf { it > 0 } ?: maxSize
  val height = intrinsicHeight.takeIf { it > 0 } ?: maxSize
  val scale = minOf(1f, maxSize.toFloat() / maxOf(width, height))
  // Toolbar measures its logo from intrinsic dimensions, not drawable bounds.
  return LayerDrawable(arrayOf(this)).apply {
    setLayerSize(0, (width * scale).roundToInt().coerceAtLeast(1), (height * scale).roundToInt().coerceAtLeast(1))
  }
}
