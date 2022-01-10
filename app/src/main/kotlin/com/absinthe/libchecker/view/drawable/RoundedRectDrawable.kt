package com.absinthe.libchecker.view.drawable

import android.graphics.drawable.PaintDrawable

class RoundedRectDrawable(color: Int, radius: Float) : PaintDrawable(color) {
  init {
    setCornerRadius(radius)
  }
}
