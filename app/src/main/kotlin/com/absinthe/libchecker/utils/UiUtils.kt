package com.absinthe.libchecker.utils

import android.graphics.Color
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
}
