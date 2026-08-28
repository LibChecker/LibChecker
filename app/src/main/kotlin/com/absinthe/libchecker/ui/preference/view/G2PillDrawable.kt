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
    val width = right - left
    val height = bottom - top
    path.rewind()
    if (width <= 0f || height <= 0f) {
      return
    }

    // Keep a circular 1:1 end envelope while easing curvature to zero at the straight edges.
    // The cached two-cubic quarter curves stay within 0.07% of a circle without flattening its tip.
    val radius = height * 0.5f
    val firstControl = radius * G2_STRAIGHT_FIRST_CONTROL
    val secondControl = radius * G2_STRAIGHT_SECOND_CONTROL
    val diagonal = radius * G2_DIAGONAL
    val diagonalInset = radius - diagonal
    val outerControlX = radius * G2_OUTER_CONTROL_X
    val outerControlY = radius * G2_OUTER_CONTROL_Y
    val sideControlY = radius * G2_SIDE_CONTROL_Y
    val centerY = top + radius

    path.moveTo(left + radius, top)
    path.lineTo(right - radius, top)
    path.cubicTo(
      right - radius + firstControl,
      top,
      right - radius + secondControl,
      top,
      right - radius + diagonal,
      top + diagonalInset
    )
    path.cubicTo(
      right - radius + outerControlX,
      top + outerControlY,
      right,
      top + sideControlY,
      right,
      centerY
    )
    path.cubicTo(
      right,
      bottom - sideControlY,
      right - radius + outerControlX,
      bottom - outerControlY,
      right - radius + diagonal,
      bottom - diagonalInset
    )
    path.cubicTo(
      right - radius + secondControl,
      bottom,
      right - radius + firstControl,
      bottom,
      right - radius,
      bottom
    )
    path.lineTo(left + radius, bottom)
    path.cubicTo(
      left + radius - firstControl,
      bottom,
      left + radius - secondControl,
      bottom,
      left + diagonalInset,
      bottom - diagonalInset
    )
    path.cubicTo(
      left + radius - outerControlX,
      bottom - outerControlY,
      left,
      bottom - sideControlY,
      left,
      centerY
    )
    path.cubicTo(
      left,
      top + sideControlY,
      left + radius - outerControlX,
      top + outerControlY,
      left + diagonalInset,
      top + diagonalInset
    )
    path.cubicTo(
      left + radius - secondControl,
      top,
      left + radius - firstControl,
      top,
      left + radius,
      top
    )
    path.close()
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

  private companion object {
    const val G2_STRAIGHT_FIRST_CONTROL = 0.05893696f
    const val G2_STRAIGHT_SECOND_CONTROL = 0.41421357f
    const val G2_DIAGONAL = 0.70710677f
    const val G2_OUTER_CONTROL_X = 0.92621285f
    const val G2_OUTER_CONTROL_Y = 0.5119993f
    const val G2_SIDE_CONTROL_Y = 0.7845774f
  }
}

internal inline fun setConvexPathOrFallback(
  setConvexPath: () -> Unit,
  setFallback: () -> Unit
) {
  runCatching(setConvexPath).getOrElse {
    if (it is IllegalArgumentException) setFallback() else throw it
  }
}
