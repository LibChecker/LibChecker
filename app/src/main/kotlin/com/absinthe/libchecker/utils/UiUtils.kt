package com.absinthe.libchecker.utils

import android.graphics.Color
import androidx.annotation.ColorInt
import com.absinthe.libraries.utils.utils.UiUtils.isDarkMode

object UiUtils {
  fun getRandomColor(): Int {
    val range = if (isDarkMode()) {
      (68..136)
    } else {
      (132..200)
    }
    val r = range.random()
    val g = range.random()
    val b = range.random()

    return Color.parseColor(String.format("#%02x%02x%02x", r, g, b))
  }

  fun isDarkColor(@ColorInt color: Int): Boolean {
    val r = Color.red(color)
    val g = Color.green(color)
    val b = Color.blue(color)
    val brightness = (r * 299 + g * 587 + b * 114) / 1000
    return brightness >= 192
  }
}
