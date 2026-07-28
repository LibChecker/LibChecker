package com.absinthe.libchecker.view.span

import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.ReplacementSpan
import androidx.annotation.ColorInt
import kotlin.math.ceil
import kotlin.math.max

class DiffHighlightSpan(
  @ColorInt private val color: Int?
) : ReplacementSpan() {

  override fun getSize(
    paint: Paint,
    text: CharSequence,
    start: Int,
    end: Int,
    fm: Paint.FontMetricsInt?
  ): Int {
    fm?.let {
      val source = paint.fontMetricsInt
      val extraBottomSpace = extraBottomSpace(paint)
      it.top = source.top
      it.ascent = source.ascent
      it.descent = source.descent + extraBottomSpace
      it.bottom = source.bottom + extraBottomSpace
      it.leading = source.leading
    }
    return ceil(paint.measureText(text, start, end).toDouble()).toInt()
  }

  override fun draw(
    canvas: Canvas,
    text: CharSequence,
    start: Int,
    end: Int,
    x: Float,
    top: Int,
    y: Int,
    bottom: Int,
    paint: Paint
  ) {
    val originalColor = paint.color
    val originalStyle = paint.style
    color?.let { paint.color = it }
    paint.style = Paint.Style.FILL

    canvas.drawText(text, start, end, x, y.toFloat(), paint)
    val textWidth = paint.measureText(text, start, end)
    val radius = dotRadius(paint)
    val dotY = y + paint.fontMetrics.descent + dotGap(paint) + radius
    canvas.drawCircle(x + textWidth / 2f, dotY, radius, paint)

    paint.color = originalColor
    paint.style = originalStyle
  }

  private fun extraBottomSpace(paint: Paint): Int {
    return ceil((dotGap(paint) + dotRadius(paint) * 2f).toDouble()).toInt()
  }

  private fun dotRadius(paint: Paint): Float = max(MIN_DOT_RADIUS, paint.textSize * DOT_RADIUS_RATIO)

  private fun dotGap(paint: Paint): Float = paint.textSize * DOT_GAP_RATIO

  private companion object {
    const val MIN_DOT_RADIUS = 1f
    const val DOT_RADIUS_RATIO = 0.045f
    const val DOT_GAP_RATIO = 0.02f
  }
}
