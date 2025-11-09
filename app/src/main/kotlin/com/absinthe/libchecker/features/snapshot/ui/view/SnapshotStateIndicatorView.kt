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
import kotlin.math.max
import kotlin.math.min

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

  fun setSnapshotStateCounts(
    addedCount: Int,
    removedCount: Int,
    changedCount: Int,
    movedCount: Int
  ) {
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

  private fun effectiveCount(count: Int, isActive: Boolean): Int {
    if (!isActive) {
      return 0
    }
    if (useCounts) {
      return count
    }
    return 1
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

    val addedEffective = effectiveCount(addedCount, added)
    if (addedEffective > 0) {
      segmentList += StateSegment(R.color.material_green_300, addedEffective)
    }
    val removedEffective = effectiveCount(removedCount, removed)
    if (removedEffective > 0) {
      segmentList += StateSegment(R.color.material_red_300, removedEffective)
    }
    val changedEffective = effectiveCount(changedCount, changed)
    if (changedEffective > 0) {
      segmentList += StateSegment(R.color.material_yellow_300, changedEffective)
    }
    val movedEffective = effectiveCount(movedCount, moved)
    if (movedEffective > 0) {
      segmentList += StateSegment(R.color.material_blue_300, movedEffective)
    }

    val total = segmentList.sumOf { it.count }
    if (total == 0) {
      return
    }
    drawOverPosition = 0f
    previousSegmentColor = null
    previousSegmentHeight = 0f
    val totalHeight = measuredHeight.toFloat()
    var remainingHeight = totalHeight

    segmentList.forEachIndexed { index, segment ->
      val isLastSegment = index == segmentList.lastIndex
      val segmentHeight = if (isLastSegment) {
        remainingHeight
      } else {
        val height = totalHeight * (segment.count.toFloat() / total)
        val adjustedHeight = height.coerceAtMost(remainingHeight)
        remainingHeight = (remainingHeight - adjustedHeight).coerceAtLeast(0f)
        adjustedHeight
      }
      drawSegment(segment.color, segmentHeight, canvas)
    }
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

  private data class StateSegment(@ColorRes val color: Int, val count: Int)
}
