package com.absinthe.libchecker.features.home.ui.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isGone
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.dpToDimensionPixelSize
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.view.AViewGroup
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

class HomeToolbarTitleView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null
) : AViewGroup(context, attrs) {

  private val loadingGap = 6.dp

  private val titleView = AppCompatTextView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    setSingleLine()
    ellipsize = TextUtils.TruncateAt.END
    includeFontPadding = false
    setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceTitleLarge))
    setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface))
  }

  private val loadingView = ToolbarConnectionLoadingView(context).apply {
    layoutParams = LayoutParams(34.dp, 18.dp)
    alpha = 0f
    scaleX = 0.78f
    scaleY = 0.78f
    isGone = true
    contentDescription = context.getString(R.string.loading)
  }

  private var loadingRequested = false
  private val visibilityInterpolator = FastOutSlowInInterpolator()

  init {
    addView(titleView)
    addView(loadingView)
  }

  fun setTitle(title: CharSequence) {
    titleView.text = title
    requestLayout()
  }

  fun setLoading(loading: Boolean) {
    if (loadingRequested == loading) {
      return
    }
    loadingRequested = loading
    loadingView.animate().cancel()

    if (loading) {
      loadingView.isGone = false
      loadingView.start()
      requestLayout()
      loadingView.animate()
        .alpha(1f)
        .scaleX(1f)
        .scaleY(1f)
        .setDuration(180L)
        .setInterpolator(visibilityInterpolator)
        .start()
    } else {
      loadingView.animate()
        .alpha(0f)
        .scaleX(0.78f)
        .scaleY(0.78f)
        .setDuration(150L)
        .setInterpolator(visibilityInterpolator)
        .withEndAction {
          loadingView.stop()
          loadingView.isGone = true
          requestLayout()
        }
        .start()
    }
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val widthMode = MeasureSpec.getMode(widthMeasureSpec)
    val heightMode = MeasureSpec.getMode(heightMeasureSpec)
    val availableWidth = when (widthMode) {
      MeasureSpec.UNSPECIFIED -> Int.MAX_VALUE
      else -> MeasureSpec.getSize(widthMeasureSpec)
    }
    val availableHeight = when (heightMode) {
      MeasureSpec.UNSPECIFIED -> Int.MAX_VALUE
      else -> MeasureSpec.getSize(heightMeasureSpec)
    }

    val loadingVisible = !loadingView.isGone
    var loadingWidth = 0
    var loadingHeight = 0
    if (loadingVisible) {
      loadingView.measure(
        loadingView.layoutParams.width.toExactlyMeasureSpec(),
        loadingView.layoutParams.height.toExactlyMeasureSpec()
      )
      loadingWidth = loadingView.measuredWidth
      loadingHeight = loadingView.measuredHeight
    }

    val gap = if (loadingVisible) loadingGap else 0
    val titleMaxWidth = (availableWidth - paddingStart - paddingEnd - loadingWidth - gap)
      .coerceAtLeast(0)
    titleView.measure(
      titleMaxWidth.toAtMostMeasureSpec(),
      (availableHeight - paddingTop - paddingBottom).coerceAtLeast(0).toAtMostMeasureSpec()
    )

    val contentWidth = titleView.measuredWidth + gap + loadingWidth
    val contentHeight = max(titleView.measuredHeight, loadingHeight)
    val desiredWidth = paddingStart + paddingEnd + contentWidth
    val desiredHeight = paddingTop + paddingBottom + contentHeight

    setMeasuredDimension(
      resolveSize(desiredWidth, widthMeasureSpec),
      resolveSize(desiredHeight, heightMeasureSpec)
    )
  }

  override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
    val loadingVisible = !loadingView.isGone
    val gap = if (loadingVisible) loadingGap else 0
    val centerY = paddingTop + (height - paddingTop - paddingBottom) / 2
    val titleTop = centerY - titleView.measuredHeight / 2
    val loadingTop = centerY - loadingView.measuredHeight / 2
    val isRtl = layoutDirection == View.LAYOUT_DIRECTION_RTL

    if (isRtl) {
      var x = paddingStart
      if (loadingVisible) {
        loadingView.layout(x, loadingTop, true)
        x += loadingView.measuredWidth + gap
      }
      titleView.layout(x, titleTop, true)
    } else {
      var x = paddingStart
      titleView.layout(x, titleTop)
      x += titleView.measuredWidth + gap
      if (loadingVisible) {
        loadingView.layout(x, loadingTop)
      }
    }
  }

  override fun onDetachedFromWindow() {
    loadingView.stop()
    super.onDetachedFromWindow()
  }

  private class ToolbarConnectionLoadingView @JvmOverloads constructor(
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
  }

  private companion object {
    const val DOT_COUNT = 3
    const val DOT_PHASE_OFFSET = 0.18f
    const val DOT_ANIMATION_DURATION = 900L
  }
}
