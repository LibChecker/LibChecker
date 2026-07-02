package com.absinthe.libchecker.view.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.MainThread
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.extensions.dpToDimension
import com.absinthe.libchecker.utils.extensions.dpToDimensionPixelSize
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

private const val DEFAULT_WIDTH_DP = 280
private const val DEFAULT_HEIGHT_DP = 88
private const val DEFAULT_ICON_SIZE_DP = 56
private const val DEFAULT_ICON_SPACING_DP = 76
private const val DEFAULT_ICON_PADDING_DP = 12
private const val DEFAULT_SIDE_BLUR_DP = 8
private const val DEFAULT_STEP_DURATION = 1200L
private const val DEFAULT_HOLD_FRACTION = 0.42f
private const val DEFAULT_CENTER_SCALE = 1.08f
private const val DEFAULT_SIDE_SCALE = 0.88f
private const val DEFAULT_SIDE_ALPHA = 0.78f
private const val DEFAULT_VISIBLE_SLOTS = 2.15f

class IconCarouselView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

  private val iconViews = mutableListOf<CarouselIconView>()
  private var animator: ValueAnimator? = null

  private var phase = 0f
  private var shouldRun = true
  private var reverse = true

  private var iconSizePx = context.dpToDimensionPixelSize(DEFAULT_ICON_SIZE_DP)
  private var iconSpacingPx = context.dpToDimension(DEFAULT_ICON_SPACING_DP)
  private var iconPaddingPx = context.dpToDimensionPixelSize(DEFAULT_ICON_PADDING_DP)
  private var sideBlurRadiusPx = context.dpToDimension(DEFAULT_SIDE_BLUR_DP)
  private var centerScale = DEFAULT_CENTER_SCALE
  private var sideScale = DEFAULT_SIDE_SCALE
  private var stepDuration = DEFAULT_STEP_DURATION
  private var holdFraction = DEFAULT_HOLD_FRACTION
  private var visibleSlots = DEFAULT_VISIBLE_SLOTS
  private val blurOutsetPx: Int
    get() = (sideBlurRadiusPx * 3f).roundToInt().coerceAtLeast(0)
  private val iconOuterSizePx: Int
    get() = iconSizePx + blurOutsetPx * 2

  init {
    clipChildren = false
    clipToPadding = false

    context.obtainStyledAttributes(attrs, R.styleable.IconCarouselView, defStyleAttr, 0)
      .use { typedArray ->
        shouldRun = typedArray.getBoolean(R.styleable.IconCarouselView_icvAutoStart, true)
        reverse = typedArray.getBoolean(R.styleable.IconCarouselView_icvReverse, true)
        iconSizePx = typedArray.getDimensionPixelSize(
          R.styleable.IconCarouselView_icvIconSize,
          iconSizePx
        )
        iconSpacingPx = typedArray.getDimension(
          R.styleable.IconCarouselView_icvIconSpacing,
          iconSpacingPx
        )
        iconPaddingPx = typedArray.getDimensionPixelSize(
          R.styleable.IconCarouselView_icvIconPadding,
          iconPaddingPx
        )
        centerScale = typedArray.getFloat(
          R.styleable.IconCarouselView_icvCenterScale,
          DEFAULT_CENTER_SCALE
        ).coerceAtLeast(0.1f)
        sideScale = typedArray.getFloat(
          R.styleable.IconCarouselView_icvSideScale,
          DEFAULT_SIDE_SCALE
        ).coerceAtLeast(0.1f)
        stepDuration = typedArray.getInt(
          R.styleable.IconCarouselView_icvStepDuration,
          DEFAULT_STEP_DURATION.toInt()
        ).toLong().coerceAtLeast(300L)
        holdFraction = typedArray.getFloat(
          R.styleable.IconCarouselView_icvHoldFraction,
          DEFAULT_HOLD_FRACTION
        ).coerceIn(0f, 0.85f)
        sideBlurRadiusPx = typedArray.getDimension(
          R.styleable.IconCarouselView_icvSideBlurRadius,
          sideBlurRadiusPx
        )
        visibleSlots = typedArray.getFloat(
          R.styleable.IconCarouselView_icvVisibleSlots,
          DEFAULT_VISIBLE_SLOTS
        ).coerceAtLeast(1f)
      }
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val outerSize = iconOuterSizePx
    val desiredWidth = max(
      context.dpToDimensionPixelSize(DEFAULT_WIDTH_DP),
      (outerSize * centerScale + iconSpacingPx * 2f * visibleSlots.coerceAtMost(2f)).roundToInt()
    ) + paddingLeft + paddingRight
    val desiredHeight = max(
      context.dpToDimensionPixelSize(DEFAULT_HEIGHT_DP),
      (outerSize * centerScale).roundToInt()
    ) + paddingTop + paddingBottom

    val measuredWidth = resolveSize(desiredWidth, widthMeasureSpec)
    val measuredHeight = resolveSize(desiredHeight, heightMeasureSpec)
    setMeasuredDimension(measuredWidth, measuredHeight)

    val childWidthSpec = MeasureSpec.makeMeasureSpec(outerSize, MeasureSpec.EXACTLY)
    val childHeightSpec = MeasureSpec.makeMeasureSpec(outerSize, MeasureSpec.EXACTLY)
    iconViews.forEach { child ->
      child.measure(childWidthSpec, childHeightSpec)
    }
  }

  override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
    val contentWidth = width - paddingLeft - paddingRight
    val contentHeight = height - paddingTop - paddingBottom
    val outerSize = iconOuterSizePx
    val childLeft = paddingLeft + (contentWidth - outerSize) / 2
    val childTop = paddingTop + (contentHeight - outerSize) / 2
    iconViews.forEach { child ->
      child.layout(childLeft, childTop, childLeft + outerSize, childTop + outerSize)
    }
    updateIconTransforms()
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    if (shouldRun) {
      startAnimator()
    } else {
      updateIconTransforms()
    }
  }

  override fun onDetachedFromWindow() {
    stopAnimator()
    super.onDetachedFromWindow()
  }

  override fun onWindowVisibilityChanged(visibility: Int) {
    super.onWindowVisibilityChanged(visibility)
    if (visibility == View.VISIBLE && shouldRun) {
      startAnimator()
    } else if (visibility != View.VISIBLE) {
      stopAnimator()
    }
  }

  @MainThread
  fun setIconDrawables(drawables: List<Drawable>) {
    stopAnimator()
    removeAllViews()
    iconViews.clear()
    phase = 0f

    val tileColor = ColorUtils.setAlphaComponent(
      context.getColorByAttr(com.google.android.material.R.attr.colorSurfaceContainerHighest),
      245
    )
    val outerSize = iconOuterSizePx
    drawables.forEach { drawable ->
      val view = CarouselIconView(
        context = context,
        iconSizePx = iconSizePx,
        iconPaddingPx = iconPaddingPx,
        blurOutsetPx = blurOutsetPx,
        tileColor = tileColor
      ).apply {
        setIcon(drawable.mutate())
      }
      iconViews.add(view)
      addView(
        view,
        LayoutParams(outerSize, outerSize).apply {
          width = outerSize
          height = outerSize
        }
      )
    }

    requestLayout()
    updateIconTransforms()
    if (shouldRun && isAttachedToWindow) {
      startAnimator()
    }
  }

  @MainThread
  fun setIconResources(@DrawableRes vararg iconResIds: Int) {
    val drawables = iconResIds.map { resId ->
      ContextCompat.getDrawable(context, resId)
    }.filterNotNull()
    setIconDrawables(drawables)
  }

  @MainThread
  fun setReverse(value: Boolean) {
    if (reverse == value) return
    reverse = value
    updateIconTransforms()
  }

  @MainThread
  fun setStepDuration(durationMs: Long) {
    val safeDuration = durationMs.coerceAtLeast(300L)
    if (stepDuration == safeDuration) return
    stepDuration = safeDuration
    recreateAnimatorIfNeeded()
  }

  @MainThread
  fun start() {
    if (shouldRun && animator?.isStarted == true) return
    shouldRun = true
    if (isAttachedToWindow) {
      startAnimator()
    }
  }

  @MainThread
  fun stop() {
    shouldRun = false
    stopAnimator()
    updateIconTransforms()
  }

  private fun startAnimator() {
    if (!shouldRun || iconViews.size <= 1) {
      updateIconTransforms()
      return
    }
    ensureAnimator()
    if (animator?.isStarted != true) {
      animator?.start()
    }
  }

  private fun ensureAnimator() {
    val count = iconViews.size
    if (animator != null || count <= 1) return
    animator = ValueAnimator.ofFloat(0f, count.toFloat()).apply {
      duration = stepDuration * count
      interpolator = LinearInterpolator()
      repeatCount = ValueAnimator.INFINITE
      addUpdateListener { animation ->
        phase = animation.animatedValue as Float
        updateIconTransforms()
      }
    }
  }

  private fun stopAnimator() {
    animator?.apply {
      removeAllUpdateListeners()
      cancel()
    }
    animator = null
  }

  private fun recreateAnimatorIfNeeded() {
    val wasRunning = animator?.isStarted == true
    stopAnimator()
    if (wasRunning && shouldRun && isAttachedToWindow) {
      startAnimator()
    }
  }

  private fun updateIconTransforms() {
    val count = iconViews.size
    if (count == 0) return

    val signedPhase = if (reverse) -resolveStepPhase(phase, count) else resolveStepPhase(phase, count)
    iconViews.forEachIndexed { index, child ->
      val offset = circularOffset(index, signedPhase, count)
      val distance = abs(offset)
      val centerWeight = (1f - distance.coerceIn(0f, 1f)).let(::easeOutCubic)
      val edgeWeight = ((visibleSlots - distance) / 0.55f).coerceIn(0f, 1f)
      val scale = sideScale + (centerScale - sideScale) * centerWeight

      child.translationX = offset * iconSpacingPx
      child.translationY = context.dpToDimension(2) * (1f - centerWeight)
      child.scaleX = scale
      child.scaleY = scale
      val sideAlpha = DEFAULT_SIDE_ALPHA * edgeWeight
      child.alpha = sideAlpha + (1f - sideAlpha) * centerWeight
      child.translationZ = context.dpToDimension(8) * centerWeight
      child.isVisible = edgeWeight > 0f || distance <= 1f
      child.setBlurRadius(sideBlurRadiusPx * (1f - centerWeight) * edgeWeight)
    }
  }

  private fun resolveStepPhase(rawPhase: Float, count: Int): Float {
    if (count <= 0) return 0f
    val normalized = normalize(rawPhase, count.toFloat())
    val base = floor(normalized).toInt()
    val local = normalized - base
    val moveProgress = ((local - holdFraction) / (1f - holdFraction)).coerceIn(0f, 1f)
    return base + easeInOutCubic(moveProgress)
  }

  private fun circularOffset(index: Int, currentPhase: Float, count: Int): Float {
    if (count <= 0) return 0f
    val half = count / 2f
    var offset = index - currentPhase
    while (offset > half) {
      offset -= count
    }
    while (offset < -half) {
      offset += count
    }
    return offset
  }

  private fun normalize(value: Float, size: Float): Float {
    var result = value % size
    if (result < 0f) {
      result += size
    }
    return result
  }

  private fun easeInOutCubic(value: Float): Float {
    val t = value.coerceIn(0f, 1f)
    return if (t < 0.5f) {
      4f * t * t * t
    } else {
      1f - (-2f * t + 2f).let { it * it * it } / 2f
    }
  }

  private fun easeOutCubic(value: Float): Float {
    val t = value.coerceIn(0f, 1f)
    val inv = 1f - t
    return 1f - inv * inv * inv
  }

  private class CarouselIconView(
    context: Context,
    iconSizePx: Int,
    iconPaddingPx: Int,
    blurOutsetPx: Int,
    tileColor: Int
  ) : FrameLayout(context) {

    private val tileView = FrameLayout(context)
    private val imageView = AppCompatImageView(context)
    private var blurBucket = -1

    init {
      importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
      clipChildren = false
      clipToPadding = false

      tileView.apply {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        clipToOutline = true
        elevation = context.dpToDimension(3)
        background = GradientDrawable().apply {
          shape = GradientDrawable.RECTANGLE
          cornerRadius = context.dpToDimension(14)
          setColor(tileColor)
        }
      }

      imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
      imageView.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
      tileView.addView(
        imageView,
        LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT).apply {
          setMargins(iconPaddingPx, iconPaddingPx, iconPaddingPx, iconPaddingPx)
        }
      )
      addView(
        tileView,
        LayoutParams(iconSizePx, iconSizePx).apply {
          setMargins(blurOutsetPx, blurOutsetPx, blurOutsetPx, blurOutsetPx)
        }
      )
    }

    fun setIcon(drawable: Drawable) {
      imageView.setImageDrawable(drawable)
    }

    fun setBlurRadius(radius: Float) {
      if (!OsUtils.atLeastS()) return

      val bucket = (radius.coerceAtLeast(0f) * 2f).roundToInt()
      if (blurBucket == bucket) return
      blurBucket = bucket

      setRenderEffect(
        if (bucket <= 0) {
          null
        } else {
          val blurRadius = bucket / 2f
          RenderEffect.createBlurEffect(blurRadius, blurRadius, Shader.TileMode.DECAL)
        }
      )
    }
  }
}
