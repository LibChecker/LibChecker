package com.absinthe.libchecker.utils.extensions

import android.text.StaticLayout

/**
 * Calculated the widest line in a [StaticLayout].
 */
internal fun StaticLayout.textWidth(): Int {
  var width = 0f
  for (i in 0 until lineCount) {
    width = width.coerceAtLeast(getLineWidth(i))
  }
  return width.toInt()
}

/**
 * Linearly interpolate between two values.
 */
internal fun lerp(a: Float, b: Float, t: Float): Float {
  return a + (b - a) * t
}
