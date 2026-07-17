package com.absinthe.libchecker.domain.snapshot.detail.ui.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.model.SnapshotPackageIconSource
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.view.AViewGroup
import com.google.android.material.R as MaterialR
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sqrt

class SnapshotCollapsedToolbarView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null
) : AViewGroup(context, attrs) {

  private val iconView = AppCompatImageView(context).apply {
    layoutParams = LayoutParams(ICON_SIZE, ICON_SIZE)
    scaleType = ImageView.ScaleType.CENTER_INSIDE
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    setImageResource(R.drawable.ic_icon_blueprint)
    addView(this)
  }

  private val titleView = AppCompatTextView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    setSingleLine()
    ellipsize = TextUtils.TruncateAt.END
    includeFontPadding = false
    setTextAppearance(context.getResourceIdByAttr(MaterialR.attr.textAppearanceTitleMedium))
    setTextColor(context.getColorByAttr(MaterialR.attr.colorOnSurface))
    setTypeface(typeface, Typeface.BOLD)
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    addView(this)
  }

  private val visibilityInterpolator = FastOutSlowInInterpolator()
  private var revealAnimator: ValueAnimator? = null
  private var revealProgress = 0f
  private var revealRequested = false
  private var blurBucket = Int.MIN_VALUE

  init {
    clipChildren = false
    clipToPadding = false
    setPadding(BLUR_OUTSET, BLUR_OUTSET, BLUR_OUTSET, BLUR_OUTSET)
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    updateStartTranslation()
    applyRevealProgress(0f)
  }

  fun bindAppName(appName: CharSequence) {
    titleView.text = appName
    contentDescription = appName
    requestLayout()
  }

  fun setIconImage(bitmap: Bitmap?) {
    if (bitmap == null) {
      setFallbackIcon()
    } else {
      iconView.load(bitmap)
    }
  }

  fun setIconSource(iconSource: SnapshotPackageIconSource?) {
    when (iconSource) {
      is SnapshotPackageIconSource.InstalledPackage -> iconView.load(iconSource.packageInfo)

      SnapshotPackageIconSource.Fallback,
      null -> setFallbackIcon()
    }
  }

  fun setFallbackIcon() {
    iconView.setImageResource(R.drawable.ic_icon_blueprint)
  }

  fun updateCollapseFraction(collapseFraction: Float) {
    val shouldReveal = resolveSnapshotCollapsedToolbarVisibility(
      collapseFraction = collapseFraction,
      currentlyRevealed = revealRequested
    )
    if (shouldReveal == revealRequested) return

    revealRequested = shouldReveal
    animateRevealProgress(if (shouldReveal) 1f else 0f)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val availableWidth = when (MeasureSpec.getMode(widthMeasureSpec)) {
      MeasureSpec.UNSPECIFIED -> Int.MAX_VALUE
      else -> MeasureSpec.getSize(widthMeasureSpec)
    }
    val availableHeight = when (MeasureSpec.getMode(heightMeasureSpec)) {
      MeasureSpec.UNSPECIFIED -> Int.MAX_VALUE
      else -> MeasureSpec.getSize(heightMeasureSpec)
    }

    iconView.measure(ICON_SIZE.toExactlyMeasureSpec(), ICON_SIZE.toExactlyMeasureSpec())
    val titleMaxWidth = (availableWidth - paddingStart - paddingEnd - ICON_SIZE - CONTENT_GAP)
      .coerceAtLeast(0)
    titleView.measure(
      titleMaxWidth.toAtMostMeasureSpec(),
      (availableHeight - paddingTop - paddingBottom).coerceAtLeast(0).toAtMostMeasureSpec()
    )

    val contentWidth = iconView.measuredWidth + CONTENT_GAP + titleView.measuredWidth
    val contentHeight = maxOf(iconView.measuredHeight, titleView.measuredHeight)
    setMeasuredDimension(
      resolveSize(paddingStart + contentWidth + paddingEnd, widthMeasureSpec),
      resolveSize(paddingTop + contentHeight + paddingBottom, heightMeasureSpec)
    )
  }

  override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
    val centerY = paddingTop + (height - paddingTop - paddingBottom) / 2
    val iconTop = centerY - iconView.measuredHeight / 2
    val titleTop = centerY - titleView.measuredHeight / 2

    if (layoutDirection == View.LAYOUT_DIRECTION_RTL) {
      var nextRight = width - paddingEnd
      iconView.layout(nextRight - iconView.measuredWidth, iconTop, nextRight, iconTop + iconView.measuredHeight)
      nextRight -= iconView.measuredWidth + CONTENT_GAP
      titleView.layout(
        nextRight - titleView.measuredWidth,
        titleTop,
        nextRight,
        titleTop + titleView.measuredHeight
      )
    } else {
      var nextLeft = paddingStart
      iconView.layout(nextLeft, iconTop, nextLeft + iconView.measuredWidth, iconTop + iconView.measuredHeight)
      nextLeft += iconView.measuredWidth + CONTENT_GAP
      titleView.layout(
        nextLeft,
        titleTop,
        nextLeft + titleView.measuredWidth,
        titleTop + titleView.measuredHeight
      )
    }
  }

  override fun onDetachedFromWindow() {
    revealAnimator?.cancel()
    revealAnimator = null
    clearBlur()
    super.onDetachedFromWindow()
  }

  override fun onRtlPropertiesChanged(layoutDirection: Int) {
    super.onRtlPropertiesChanged(layoutDirection)
    updateStartTranslation()
  }

  private fun animateRevealProgress(targetProgress: Float) {
    revealAnimator?.cancel()
    val remainingDistance = abs(targetProgress - revealProgress)
    if (remainingDistance == 0f) return

    revealAnimator = ValueAnimator.ofFloat(revealProgress, targetProgress).apply {
      duration = (REVEAL_DURATION_MILLIS * remainingDistance).roundToLong().coerceAtLeast(1L)
      interpolator = visibilityInterpolator
      addUpdateListener {
        applyRevealProgress(it.animatedValue as Float)
      }
      start()
    }
  }

  private fun applyRevealProgress(progress: Float) {
    revealProgress = progress.coerceIn(0f, 1f)
    val motion = calculateSnapshotCollapsedToolbarMotion(revealProgress)
    visibility = if (revealProgress > 0f) VISIBLE else INVISIBLE
    alpha = motion.alpha
    translationY = ENTRY_TRANSLATION * motion.translationFraction
    applyBlur(MAX_BLUR_RADIUS * motion.blurFraction)
  }

  private fun applyBlur(radius: Float) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

    val bucket = radius.coerceAtLeast(0f).roundToInt()
    if (bucket == blurBucket) return
    blurBucket = bucket
    setRenderEffect(
      if (bucket == 0) {
        null
      } else {
        RenderEffect.createBlurEffect(
          bucket.toFloat(),
          bucket.toFloat(),
          Shader.TileMode.CLAMP
        )
      }
    )
  }

  private fun clearBlur() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    blurBucket = 0
    setRenderEffect(null)
  }

  private fun updateStartTranslation() {
    translationX = if (layoutDirection == View.LAYOUT_DIRECTION_RTL) {
      START_ALIGNMENT_OFFSET
    } else {
      -START_ALIGNMENT_OFFSET
    }
  }

  private companion object {
    val ICON_SIZE = 28.dp
    val CONTENT_GAP = 10.dp
    val ENTRY_TRANSLATION = 6.dp.toFloat()
    val MAX_BLUR_RADIUS = 18.dp.toFloat()
    val BLUR_OUTSET = 18.dp
    val START_ALIGNMENT_OFFSET = 24.dp.toFloat()
    const val REVEAL_DURATION_MILLIS = 360L
  }
}

internal data class SnapshotCollapsedToolbarMotion(
  val alpha: Float,
  val translationFraction: Float,
  val blurFraction: Float
)

internal fun calculateSnapshotCollapsedToolbarMotion(progress: Float): SnapshotCollapsedToolbarMotion {
  val clampedProgress = progress.coerceIn(0f, 1f)
  val hiddenFraction = 1f - clampedProgress
  val blurFraction = sqrt(hiddenFraction)
  return SnapshotCollapsedToolbarMotion(
    alpha = (clampedProgress * 1.5f).coerceAtMost(1f),
    translationFraction = blurFraction,
    blurFraction = blurFraction
  )
}

internal fun resolveSnapshotCollapsedToolbarVisibility(
  collapseFraction: Float,
  currentlyRevealed: Boolean
): Boolean {
  return when {
    collapseFraction >= COLLAPSED_TOOLBAR_REVEAL_THRESHOLD -> true
    collapseFraction <= COLLAPSED_TOOLBAR_HIDE_THRESHOLD -> false
    else -> currentlyRevealed
  }
}

private const val COLLAPSED_TOOLBAR_REVEAL_THRESHOLD = 0.68f
private const val COLLAPSED_TOOLBAR_HIDE_THRESHOLD = 0.52f
