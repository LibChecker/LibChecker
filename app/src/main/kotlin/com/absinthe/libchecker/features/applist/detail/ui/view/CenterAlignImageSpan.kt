package com.absinthe.libchecker.features.applist.detail.ui.view

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.FontMetricsInt
import android.graphics.drawable.Drawable
import android.text.style.ImageSpan
import androidx.core.graphics.withSave

class CenterAlignImageSpan(drawable: Drawable) : ImageSpan(drawable) {

  override fun getSize(
    paint: Paint,
    text: CharSequence?,
    start: Int,
    end: Int,
    fm: FontMetricsInt?
  ): Int {
    val rect = drawable.bounds
    val drawableHeight = rect.height().toFloat()
    val paintFm = paint.fontMetrics
    val textHeight = paintFm.descent - paintFm.ascent

    if (fm != null) {
      val textCenter = (paintFm.descent + paintFm.ascent) / 2
      fm.top = (textCenter - drawableHeight / 2).toInt()
      fm.ascent = fm.top
      fm.bottom = (drawableHeight + fm.ascent).toInt()
      fm.descent = fm.bottom
    }

    return rect.right
  }

  override fun draw(
    canvas: Canvas,
    text: CharSequence?,
    start: Int,
    end: Int,
    x: Float,
    top: Int,
    y: Int,
    bottom: Int,
    paint: Paint
  ) {
    canvas.withSave {
      val fm = paint.fontMetrics
      val drawableHeight = drawable.bounds.height().toFloat()
      val transY = y + (fm.descent + fm.ascent) / 2 - drawableHeight / 2
      translate(x, transY)
      drawable.draw(this)
    }
  }
}
