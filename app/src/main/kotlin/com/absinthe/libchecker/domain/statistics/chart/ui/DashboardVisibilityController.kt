package com.absinthe.libchecker.domain.statistics.chart.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToLong

internal class DashboardVisibilityController(
  private val dashboard: ViewGroup,
  private val content: View
) {
  private val motionInterpolator = FastOutSlowInInterpolator()
  private var targetVisible: Boolean? = null
  private var animationGeneration = 0
  private var animator: ValueAnimator? = null
  private var currentVisibleHeight: Int? = null

  fun setVisible(visible: Boolean, animate: Boolean) {
    if (targetVisible == visible) return

    targetVisible = visible
    val generation = ++animationGeneration
    animator?.cancel()
    animator = null

    if (!animate || !dashboard.isLaidOut) {
      settle(visible)
      return
    }

    if (visible) {
      animateVisible(generation)
    } else {
      animateHidden(generation)
    }
  }

  fun cancel() {
    animationGeneration++
    animator?.cancel()
    animator = null
    settle(targetVisible ?: dashboard.isVisible)
  }

  private fun animateVisible(generation: Int) {
    val targetHeight = measureExpandedHeight()
    if (targetHeight <= 0) {
      settle(true)
      return
    }

    val wasVisible = dashboard.isVisible
    val startVisibleHeight = currentVisibleHeight ?: if (wasVisible) dashboard.height else 0
    val layoutHeight = max(startVisibleHeight, targetHeight)
    val needsLayout = !wasVisible || dashboard.height != layoutHeight
    dashboard.layoutParams = dashboard.layoutParams.apply { height = layoutHeight }
    content.translationY = (startVisibleHeight - layoutHeight).toFloat()
    dashboard.alpha = 1f
    dashboard.isVisible = true
    dashboard.clipBounds = Rect(0, 0, dashboard.width, startVisibleHeight)
    if (!needsLayout) {
      startAnimator(generation, true, startVisibleHeight, targetHeight, layoutHeight)
    } else {
      dashboard.doOnNextLayout {
        if (generation != animationGeneration) return@doOnNextLayout
        startAnimator(generation, true, startVisibleHeight, targetHeight, layoutHeight)
      }
    }
  }

  private fun animateHidden(generation: Int) {
    if (!dashboard.isVisible || dashboard.height == 0) {
      settle(false)
      return
    }

    val layoutHeight = dashboard.height
    val startVisibleHeight = currentVisibleHeight ?: layoutHeight
    startAnimator(generation, false, startVisibleHeight, 0, layoutHeight)
  }

  private fun startAnimator(
    generation: Int,
    visible: Boolean,
    startVisibleHeight: Int,
    endVisibleHeight: Int,
    expandedHeight: Int
  ) {
    animator = ValueAnimator.ofInt(startVisibleHeight, endVisibleHeight).apply {
      duration = (
        BOUNDS_DURATION_MILLIS *
          abs(endVisibleHeight - startVisibleHeight).toFloat() /
          expandedHeight.coerceAtLeast(1)
        ).roundToLong()
      interpolator = motionInterpolator
      addUpdateListener { animation ->
        updateVisualBoundary(animation.animatedValue as Int, expandedHeight)
      }
      addListener(
        object : AnimatorListenerAdapter() {
          override fun onAnimationEnd(animation: Animator) {
            if (generation != animationGeneration) return
            animator = null
            settle(visible)
          }
        }
      )
      start()
    }
  }

  private fun updateVisualBoundary(visibleHeight: Int, expandedHeight: Int) {
    currentVisibleHeight = visibleHeight
    dashboard.clipBounds = Rect(0, 0, dashboard.width, visibleHeight)
    content.translationY = (visibleHeight - expandedHeight).toFloat()
  }

  private fun measureExpandedHeight(): Int {
    val parentWidth = (dashboard.parent as? View)?.width ?: dashboard.width
    if (parentWidth <= 0) return dashboard.measuredHeight

    dashboard.measure(
      View.MeasureSpec.makeMeasureSpec(parentWidth, View.MeasureSpec.EXACTLY),
      View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    )
    return dashboard.measuredHeight
  }

  private fun settle(visible: Boolean) {
    currentVisibleHeight = if (visible) measureExpandedHeight() else 0
    content.translationY = 0f
    dashboard.clipBounds = null
    dashboard.alpha = 1f
    dashboard.isVisible = visible
    dashboard.layoutParams = dashboard.layoutParams.apply {
      height = ViewGroup.LayoutParams.WRAP_CONTENT
    }
  }

  private companion object {
    const val BOUNDS_DURATION_MILLIS = 300L
  }
}
