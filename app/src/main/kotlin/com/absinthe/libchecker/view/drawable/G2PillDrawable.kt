package com.absinthe.libchecker.view.drawable

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import kotlin.math.min

class G2PillDrawable(
  @ColorInt fillColor: Int,
  @ColorInt strokeColor: Int? = null,
  private var strokeWidth: Float = 0f,
  private val cornerSmoothing: Float? = null,
  private var cornerProgress: Float = 1f
) : Drawable() {
  private val path = Path()
  private var fillColor = fillColor
  private var strokeColor = strokeColor
  private var drawableAlpha = 255
  private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = fillColor
    style = Paint.Style.FILL
  }
  private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = strokeColor ?: Color.TRANSPARENT
    alpha = strokeColor?.let(::modulatedAlpha) ?: 0
    style = Paint.Style.STROKE
    strokeWidth = this@G2PillDrawable.strokeWidth
  }

  fun setFillColor(@ColorInt color: Int) {
    if (fillColor != color) {
      fillColor = color
      fillPaint.color = color
      fillPaint.alpha = modulatedAlpha(color)
      invalidateSelf()
    }
  }

  fun setStroke(width: Float, @ColorInt color: Int?) {
    if (strokeWidth == width && strokeColor == color) return
    strokeWidth = width
    strokeColor = color
    strokePaint.color = color ?: Color.TRANSPARENT
    strokePaint.alpha = color?.let(::modulatedAlpha) ?: 0
    strokePaint.strokeWidth = width
    onBoundsChange(bounds)
    invalidateSelf()
  }

  fun setCornerProgress(progress: Float) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    if (cornerProgress == clampedProgress) return
    cornerProgress = clampedProgress
    onBoundsChange(bounds)
    invalidateSelf()
  }

  override fun onBoundsChange(bounds: Rect) {
    super.onBoundsChange(bounds)
    val inset = strokeWidth / 2f
    val left = bounds.left + inset
    val top = bounds.top + inset
    val right = bounds.right - inset
    val bottom = bounds.bottom - inset
    path.setG2Shape(
      left,
      top,
      right,
      bottom,
      (bottom - top) / 2f * cornerProgress,
      cornerSmoothing = cornerSmoothing
    )
  }

  override fun draw(canvas: Canvas) {
    canvas.drawPath(path, fillPaint)
    if (strokeColor != null) {
      canvas.drawPath(path, strokePaint)
    }
  }

  override fun setAlpha(alpha: Int) {
    val clampedAlpha = alpha.coerceIn(0, 255)
    if (drawableAlpha == clampedAlpha) return
    drawableAlpha = clampedAlpha
    fillPaint.alpha = modulatedAlpha(fillColor)
    strokePaint.alpha = strokeColor?.let(::modulatedAlpha) ?: 0
    invalidateSelf()
  }

  private fun modulatedAlpha(@ColorInt color: Int): Int = Color.alpha(color) * drawableAlpha / 255

  override fun setColorFilter(colorFilter: ColorFilter?) {
    fillPaint.colorFilter = colorFilter
    strokePaint.colorFilter = colorFilter
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
