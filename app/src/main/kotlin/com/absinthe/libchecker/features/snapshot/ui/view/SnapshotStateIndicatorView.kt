package com.absinthe.libchecker.features.snapshot.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Outline
import android.graphics.Paint
import android.view.View
import android.view.ViewOutlineProvider
import androidx.annotation.ColorRes
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.getColor
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class SnapshotStateIndicatorView(context: Context) : View(context) {
  var enableRoundCorner: Boolean = true
  var added: Boolean = false
    set(value) {
      if (field == value) return
      field = value
      if (isUpdatingFromCounts) {
        return
      }
      useCounts = false
      addedCount = if (value) 1 else 0
      invalidate()
    }
  var removed: Boolean = false
    set(value) {
      if (field == value) return
      field = value
      if (isUpdatingFromCounts) {
        return
      }
      useCounts = false
      removedCount = if (value) 1 else 0
      invalidate()
    }
  var changed: Boolean = false
    set(value) {
      if (field == value) return
      field = value
      if (isUpdatingFromCounts) {
        return
      }
      useCounts = false
      changedCount = if (value) 1 else 0
      invalidate()
    }
  var moved: Boolean = false
    set(value) {
      if (field == value) return
      field = value
      if (isUpdatingFromCounts) {
        return
      }
      useCounts = false
      movedCount = if (value) 1 else 0
      invalidate()
    }

  // Tracks whether the indicator should honor proportional counts or fall back to equal segments.
  private var useCounts: Boolean = false
  private var isUpdatingFromCounts: Boolean = false
  private var addedCount: Int = 0
  private var removedCount: Int = 0
  private var changedCount: Int = 0
  private var movedCount: Int = 0
  private var p: Paint = Paint().apply {
    isAntiAlias = true
    isDither = true
  }
  private val gradientPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    isDither = true
  }
  private val transitionLengthPx = 8f * resources.displayMetrics.density
  private var drawOverPosition: Float = 0f
  private var previousSegmentColor: Int? = null
  private var previousSegmentHeight: Float = 0f
  private var demoAnimating: Boolean = false
  private var demoWeights: FloatArray? = null
  private val demoCurrentWeights = DoubleArray(4)
  private val demoTargetWeights = DoubleArray(4)
  private val demoAnimationRunnable = object : Runnable {
    override fun run() {
      if (!demoAnimating) {
        return
      }
      // Ease current weights toward their random targets for fluid transitions.
      var allClose = true
      for (i in demoCurrentWeights.indices) {
        val delta = demoTargetWeights[i] - demoCurrentWeights[i]
        if (abs(delta) > DEMO_TARGET_THRESHOLD) {
          demoCurrentWeights[i] += delta * DEMO_EASING
          allClose = false
        } else {
          demoCurrentWeights[i] = demoTargetWeights[i]
        }
      }
      if (allClose) {
        refreshDemoTargets()
      }
      val weightSum = demoCurrentWeights.sum()
      if (weightSum <= 0.0) {
        postDelayed(this, DEMO_FRAME_DELAY_MS)
        return
      }
      val normalized = FloatArray(4) { index ->
        (demoCurrentWeights[index] / weightSum).toFloat()
      }
      val weightsArray = demoWeights ?: FloatArray(4).also { demoWeights = it }
      for (i in normalized.indices) {
        weightsArray[i] = normalized[i]
      }
      invalidate()
      postDelayed(this, DEMO_FRAME_DELAY_MS)
    }
  }

  private fun initializeDemoWeights() {
    for (i in demoCurrentWeights.indices) {
      demoCurrentWeights[i] = randomWeight()
    }
    val weightSum = demoCurrentWeights.sum().coerceAtLeast(1e-6)
    val weightsArray = demoWeights ?: FloatArray(4).also { demoWeights = it }
    for (i in demoCurrentWeights.indices) {
      weightsArray[i] = (demoCurrentWeights[i] / weightSum).toFloat()
    }
    invalidate()
    refreshDemoTargets()
  }

  private fun refreshDemoTargets() {
    for (i in demoTargetWeights.indices) {
      var next = randomWeight()
      var retry = 0
      while (abs(next - demoCurrentWeights[i]) < DEMO_TARGET_THRESHOLD && retry < DEMO_MAX_RETRY) {
        next = randomWeight()
        retry++
      }
      demoTargetWeights[i] = next
    }
  }

  fun setSnapshotStateCounts(
    addedCount: Int,
    removedCount: Int,
    changedCount: Int,
    movedCount: Int
  ) {
    demoWeights = null
    useCounts = true
    this.addedCount = addedCount.coerceAtLeast(0)
    this.removedCount = removedCount.coerceAtLeast(0)
    this.changedCount = changedCount.coerceAtLeast(0)
    this.movedCount = movedCount.coerceAtLeast(0)

    isUpdatingFromCounts = true
    added = this.addedCount > 0
    removed = this.removedCount > 0
    changed = this.changedCount > 0
    moved = this.movedCount > 0
    isUpdatingFromCounts = false

    invalidate()
  }

  private fun effectiveCount(count: Int, isActive: Boolean): Float {
    if (!isActive) {
      return 0f
    }
    if (useCounts) {
      return count.toFloat()
    }
    return 1f
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    if (enableRoundCorner) {
      outlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View?, outline: Outline?) {
          outline?.setRoundRect(0, 0, measuredWidth, measuredHeight, (measuredWidth / 2).toFloat())
        }
      }
      clipToOutline = true
    }
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    val segmentList = mutableListOf<StateSegment>()

    val demoSnapshot = demoWeights
    if (demoSnapshot != null) {
      demoSnapshot.forEachIndexed { index, fraction ->
        if (fraction > DEMO_MIN_SEGMENT_FRACTION) {
          val color = DEMO_COLORS.getOrNull(index) ?: return@forEachIndexed
          segmentList += StateSegment(color, fraction)
        }
      }
    } else {
      val addedEffective = effectiveCount(addedCount, added)
      if (addedEffective > 0f) {
        segmentList += StateSegment(R.color.material_green_300, addedEffective)
      }
      val removedEffective = effectiveCount(removedCount, removed)
      if (removedEffective > 0f) {
        segmentList += StateSegment(R.color.material_red_300, removedEffective)
      }
      val changedEffective = effectiveCount(changedCount, changed)
      if (changedEffective > 0f) {
        segmentList += StateSegment(R.color.material_yellow_300, changedEffective)
      }
      val movedEffective = effectiveCount(movedCount, moved)
      if (movedEffective > 0f) {
        segmentList += StateSegment(R.color.material_blue_300, movedEffective)
      }
    }

    val total = segmentList.sumOf { it.value.toDouble() }
    if (total <= 0.0) {
      return
    }
    drawOverPosition = 0f
    previousSegmentColor = null
    previousSegmentHeight = 0f
    val totalHeight = measuredHeight.toFloat()
    var remainingHeight = totalHeight
    val totalFloat = total.toFloat().coerceAtLeast(Float.MIN_VALUE)

    segmentList.forEachIndexed { index, segment ->
      val isLastSegment = index == segmentList.lastIndex
      val segmentHeight = if (isLastSegment) {
        remainingHeight
      } else {
        val height = totalHeight * (segment.value / totalFloat)
        val adjustedHeight = height.coerceAtMost(remainingHeight)
        remainingHeight = (remainingHeight - adjustedHeight).coerceAtLeast(0f)
        adjustedHeight
      }
      drawSegment(segment.color, segmentHeight, canvas)
    }
  }

  fun startDemoAnimation() {
    if (demoAnimating) {
      return
    }
    demoAnimating = true
    useCounts = false
    initializeDemoWeights()
    post(demoAnimationRunnable)
  }

  fun stopDemoAnimation() {
    if (!demoAnimating) {
      return
    }
    demoAnimating = false
    removeCallbacks(demoAnimationRunnable)
    if (demoWeights != null) {
      demoWeights = null
      invalidate()
    }
  }

  override fun onDetachedFromWindow() {
    stopDemoAnimation()
    super.onDetachedFromWindow()
  }

  private fun drawSegment(@ColorRes color: Int, height: Float, canvas: Canvas) {
    if (height <= 0f) {
      return
    }
    val width = measuredWidth.toFloat()
    val top = drawOverPosition
    val bottom = top + height
    val resolvedColor = color.getColor(context)

    p.color = resolvedColor
    canvas.drawRect(0f, top, width, bottom, p)

    val prevHeight = previousSegmentHeight
    previousSegmentColor?.let { prevColor ->
      if (transitionLengthPx > 0f && prevColor != resolvedColor) {
        val halfTransition = transitionLengthPx / 2f
        val adjustedStart = max((top - halfTransition).coerceAtLeast(0f), top - prevHeight)
        val adjustedEnd = min(bottom, top + halfTransition)
        val effectiveHeight = adjustedEnd - adjustedStart
        if (effectiveHeight > 0f) {
          gradientPaint.shader = LinearGradient(
            0f,
            adjustedStart,
            0f,
            adjustedEnd,
            prevColor,
            resolvedColor,
            android.graphics.Shader.TileMode.CLAMP
          )
          canvas.drawRect(0f, adjustedStart, width, adjustedEnd, gradientPaint)
          gradientPaint.shader = null
        }
      }
    }

    drawOverPosition += height
    previousSegmentColor = resolvedColor
    previousSegmentHeight = height
  }

  private data class StateSegment(@ColorRes val color: Int, val value: Float)

  private fun randomWeight(): Double {
    return Random.nextDouble(DEMO_MIN_WEIGHT, DEMO_MAX_WEIGHT)
  }

  private companion object {
    private val DEMO_COLORS = intArrayOf(
      R.color.material_green_300,
      R.color.material_red_300,
      R.color.material_yellow_300,
      R.color.material_blue_300
    )
    private const val DEMO_FRAME_DELAY_MS = 50L
    private const val DEMO_EASING = 0.015
    private const val DEMO_TARGET_THRESHOLD = 0.015
    private const val DEMO_MIN_WEIGHT = 0.25
    private const val DEMO_MAX_WEIGHT = 1.2
    private const val DEMO_MAX_RETRY = 8
    private const val DEMO_MIN_SEGMENT_FRACTION = 0.001f
  }
}
