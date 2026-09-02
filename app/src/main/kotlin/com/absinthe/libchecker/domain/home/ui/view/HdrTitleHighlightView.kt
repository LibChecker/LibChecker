package com.absinthe.libchecker.domain.home.ui.view

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Gainmap
import android.util.AttributeSet
import android.view.animation.LinearInterpolator
import androidx.appcompat.widget.AppCompatTextView
import com.absinthe.libchecker.utils.OsUtils
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Toolbar title that renders all of its text through a synthetic HDR gainmap,
 * so every glyph can exceed the SDR white point on HDR-capable screens.
 * Requires API 35+, because the host window has to cap its requested HDR
 * headroom (`Window.setDesiredHdrHeadroom`) to keep the display from
 * renegotiating its brightness curve. Below that it falls back to the plain
 * TextView rendering.
 */
class HdrTitleHighlightView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null
) : AppCompatTextView(context, attrs) {

  private var hdrBitmap: Bitmap? = null
  private var hdrBitmapDirty = true
  private var hdrTransitionAnimator: ValueAnimator? = null
  private var hdrHeadroomChangedListener: ((Float) -> Unit)? = null
  internal var hdrHighlightEnabled = false
    private set
  internal var hdrHighlightProgress = 0f
    private set

  private val isGainmapSupported get() = OsUtils.atLeastV()

  override fun onDraw(canvas: Canvas) {
    if (!isGainmapSupported || hdrHighlightProgress <= 0f) {
      super.onDraw(canvas)
      return
    }
    val bitmap = obtainHdrBitmap()
    if (bitmap == null) {
      super.onDraw(canvas)
    } else {
      canvas.drawBitmap(bitmap, 0f, 0f, null)
    }
  }

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)
    markHdrBitmapDirty()
  }

  override fun onTextChanged(text: CharSequence, start: Int, lengthBefore: Int, lengthAfter: Int) {
    super.onTextChanged(text, start, lengthBefore, lengthAfter)
    markHdrBitmapDirty()
  }

  override fun setTextColor(color: Int) {
    super.setTextColor(color)
    markHdrBitmapDirty()
  }

  override fun setTextColor(colors: ColorStateList) {
    super.setTextColor(colors)
    markHdrBitmapDirty()
  }

  override fun onDetachedFromWindow() {
    hdrTransitionAnimator?.cancel()
    hdrTransitionAnimator = null
    updateHdrHighlightProgress(0f)
    hdrBitmap?.recycle()
    hdrBitmap = null
    hdrBitmapDirty = true
    super.onDetachedFromWindow()
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    setHdrHighlightEnabled(hdrHighlightEnabled, animate = false)
  }

  fun setHdrHighlightEnabled(enabled: Boolean, animate: Boolean = true) {
    val targetProgress = if (enabled) 1f else 0f
    if (hdrHighlightEnabled == enabled) {
      if (hdrHighlightProgress == targetProgress) {
        invalidateHdrRenderState()
        return
      }
      if (hdrTransitionAnimator?.isRunning == true) return
    }
    hdrHighlightEnabled = enabled
    hdrTransitionAnimator?.cancel()
    hdrTransitionAnimator = null
    if (!isGainmapSupported || !isAttachedToWindow) {
      updateHdrHighlightProgress(0f)
      invalidateHdrRenderState()
      return
    }
    if (!animate || !isLaidOut) {
      updateHdrHighlightProgress(targetProgress)
      invalidateHdrRenderState()
      return
    }
    hdrTransitionAnimator = ValueAnimator.ofFloat(hdrHighlightProgress, targetProgress).apply {
      duration = (
        abs(targetProgress - hdrHighlightProgress) * HDR_TRANSITION_DURATION_MS
        ).roundToLong().coerceAtLeast(1L)
      interpolator = LinearInterpolator()
      addUpdateListener { animator ->
        updateHdrHighlightProgress(animator.animatedValue as Float)
        invalidateHdrRenderState()
      }
      start()
    }
  }

  fun setHdrHeadroomChangedListener(listener: ((Float) -> Unit)?) {
    hdrHeadroomChangedListener = listener
    listener?.invoke(hdrContentBoost(hdrHighlightProgress))
  }

  private fun updateHdrHighlightProgress(progress: Float) {
    hdrHighlightProgress = progress
    hdrHeadroomChangedListener?.invoke(hdrContentBoost(progress))
  }

  private fun invalidateHdrRenderState() {
    invalidate()
  }

  private fun markHdrBitmapDirty() {
    if (!isGainmapSupported) {
      return
    }
    hdrBitmapDirty = true
    invalidate()
  }

  private fun obtainHdrBitmap(): Bitmap? {
    if (hdrBitmapDirty) {
      val rebuilt = buildHdrBitmap()
      if (rebuilt != null) {
        hdrBitmap?.recycle()
        hdrBitmap = rebuilt
        hdrBitmapDirty = false
      }
    }
    return hdrBitmap
  }

  private fun buildHdrBitmap(): Bitmap? {
    val layout = layout ?: return null
    val width = measuredWidth
    val height = measuredHeight
    if (width <= 0 || height <= 0) {
      return null
    }

    val base = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(base)
    canvas.save()
    canvas.translate(paddingLeft.toFloat(), extendedPaddingTop.toFloat())
    layout.paint.color = currentTextColor
    layout.draw(canvas)
    canvas.restore()
    base.gainmap = createUniformGainmap(width, height)
    return base
  }

  internal fun createUniformGainmap(width: Int, height: Int): Gainmap {
    val contents = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    contents.eraseColor(Color.WHITE)
    return Gainmap(contents).apply {
      setRatioMin(SDR_RATIO, SDR_RATIO, SDR_RATIO)
      setGamma(SDR_RATIO, SDR_RATIO, SDR_RATIO)
      minDisplayRatioForHdrTransition = SDR_RATIO
      applyGainmapBoost(this, MAX_CONTENT_BOOST)
    }
  }

  private fun applyGainmapBoost(gainmap: Gainmap, boost: Float) {
    gainmap.setRatioMax(boost, boost, boost)
    gainmap.displayRatioForFullHdr = boost
  }

  companion object {
    private const val SDR_RATIO = 1f

    /**
     * Uniform gainmap boost applied to every title glyph, and the matching HDR
     * headroom requested by the host window.
     */
    const val MAX_CONTENT_BOOST = 3.0f
    private const val HDR_TRANSITION_DURATION_MS = 100f
  }
}

internal fun hdrContentBoost(progress: Float): Float {
  val clampedProgress = progress.coerceIn(0f, 1f)
  return 1f + (HdrTitleHighlightView.MAX_CONTENT_BOOST - 1f) * clampedProgress
}
