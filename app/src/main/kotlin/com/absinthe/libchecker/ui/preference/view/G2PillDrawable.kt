package com.absinthe.libchecker.ui.preference.view

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import com.absinthe.libchecker.view.drawable.setG2Shape
import kotlin.math.min

class G2PillDrawable(
  @ColorInt fillColor: Int,
  @ColorInt strokeColor: Int? = null,
  private val strokeWidth: Float = 0f
) : Drawable() {

  private val path = Path()
  private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = fillColor
    style = Paint.Style.FILL
  }
  private val strokePaint = strokeColor?.let { color ->
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.color = color
      style = Paint.Style.STROKE
      strokeWidth = this@G2PillDrawable.strokeWidth
    }
  }

  override fun onBoundsChange(bounds: Rect) {
    super.onBoundsChange(bounds)
    val inset = strokeWidth / 2f
    val left = bounds.left + inset
    val top = bounds.top + inset
    val right = bounds.right - inset
    val bottom = bounds.bottom - inset
    path.setG2Shape(left, top, right, bottom, (bottom - top) / 2f)
  }

  override fun draw(canvas: Canvas) {
    canvas.drawPath(path, fillPaint)
    strokePaint?.let {
      canvas.drawPath(path, it)
    }
  }

  override fun setAlpha(alpha: Int) {
    fillPaint.alpha = alpha
    strokePaint?.alpha = alpha
    invalidateSelf()
  }

  override fun setColorFilter(colorFilter: ColorFilter?) {
    fillPaint.colorFilter = colorFilter
    strokePaint?.colorFilter = colorFilter
    invalidateSelf()
  }

  @Suppress("DEPRECATION")
  override fun getOutline(outline: Outline) {
    val outlineBounds = bounds
    if (outlineBounds.isEmpty) {
      outline.setEmpty()
    } else {
      setConvexPathOrFallback(
        setConvexPath = { outline.setConvexPath(path) },
        setFallback = {
          val radius = min(outlineBounds.width(), outlineBounds.height()) / 2f
          outline.setRoundRect(outlineBounds, radius)
        }
      )
    }
  }

  @Deprecated("Deprecated in Java")
  override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

internal inline fun setConvexPathOrFallback(
  setConvexPath: () -> Unit,
  setFallback: () -> Unit
) {
  runCatching(setConvexPath).getOrElse {
    if (it is IllegalArgumentException) setFallback() else throw it
  }
}
