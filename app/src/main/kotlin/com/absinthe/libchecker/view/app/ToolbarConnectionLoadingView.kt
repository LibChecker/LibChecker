package com.absinthe.libchecker.view.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.absinthe.libchecker.utils.extensions.dpToDimensionPixelSize
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

class ToolbarConnectionLoadingView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null
) : View(context, attrs) {

  private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = context.getColorByAttr(com.google.android.material.R.attr.colorOnSurfaceVariant)
  }
  private val dotRadius = context.dpToDimensionPixelSize(2.5f)
  private val dotSpacing = context.dpToDimensionPixelSize(8)
  private val travelY = context.dpToDimensionPixelSize(3)
  private val phaseInterpolator = FastOutSlowInInterpolator()
  private var progress = 0f
  private var animator: ValueAnimator? = null

  init {
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    val startX = (width - dotSpacing * 2) / 2f
    val centerY = height / 2f + travelY / 2f

    repeat(DOT_COUNT) { index ->
      val localProgress = (progress - index * DOT_PHASE_OFFSET + 1f) % 1f
      val wave = ((sin(localProgress * PI * 2 - PI / 2) + 1f) / 2f).toFloat()
      val easedWave = phaseInterpolator.getInterpolation(wave)
      val radius = dotRadius * (0.72f + 0.34f * easedWave)
      val alpha = (86 + 169 * easedWave).roundToInt().coerceIn(0, 255)

      dotPaint.alpha = alpha
      canvas.drawCircle(
        startX + dotSpacing * index,
        centerY - travelY * easedWave,
        radius,
        dotPaint
      )
    }
  }

  fun start() {
    if (animator?.isStarted == true) {
      return
    }
    animator = ValueAnimator.ofFloat(0f, 1f).apply {
      duration = DOT_ANIMATION_DURATION
      interpolator = LinearInterpolator()
      repeatCount = ValueAnimator.INFINITE
      addUpdateListener {
        progress = it.animatedValue as Float
        invalidate()
      }
      start()
    }
  }

  fun stop() {
    animator?.cancel()
    animator = null
    progress = 0f
    invalidate()
  }

  override fun onDetachedFromWindow() {
    stop()
    super.onDetachedFromWindow()
  }

  private companion object {
    const val DOT_COUNT = 3
    const val DOT_PHASE_OFFSET = 0.18f
    const val DOT_ANIMATION_DURATION = 900L
  }
}
