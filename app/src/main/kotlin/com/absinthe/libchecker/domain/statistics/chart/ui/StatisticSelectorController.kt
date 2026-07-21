package com.absinthe.libchecker.domain.statistics.chart.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.core.view.doOnNextLayout
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDefinition
import com.absinthe.libchecker.domain.statistics.chart.presentation.StatisticSelectorPlan
import com.absinthe.libchecker.domain.statistics.chart.ui.view.ExpandingView
import com.absinthe.libchecker.utils.extensions.dp
import com.google.android.flexbox.FlexboxLayout

internal class StatisticSelectorController(
  private val context: Context,
  private val container: FlexboxLayout,
  private val chartContainer: View,
  private val onStatisticSelected: (String) -> Unit,
  private val onAddStatistic: () -> Unit
) {
  private val entries = linkedMapOf<String, StatisticEntry>()
  private val motionInterpolator = FastOutSlowInInterpolator()
  private var visibleStatisticIds = emptyList<String>()
  private var targetSelectedStatisticId: String? = null
  private var animationGeneration = 0
  private var reflowAnimator: AnimatorSet? = null

  fun render(plan: StatisticSelectorPlan) {
    val visibleIds = plan.visibleStatistics.map(StatisticDefinition::id)
    if (visibleIds != visibleStatisticIds) {
      rebuild(plan)
      return
    }

    entries.values.zip(plan.visibleStatistics).forEach { (entry, statistic) ->
      entry.statistic = statistic
    }

    val selectedId = plan.selectedStatistic?.id
    if (selectedId == targetSelectedStatisticId) {
      updateContent(selectedId)
      return
    }

    cancelMotion()
    val reflowPlan = createReflowPlan(selectedId)
    targetSelectedStatisticId = selectedId
    if (!container.isLaidOut) {
      applySelection(selectedId)
    } else if (reflowPlan != null) {
      animateAcrossReflow(selectedId, reflowPlan)
    } else {
      animateBoundsToSelection(selectedId)
    }
  }

  private fun rebuild(plan: StatisticSelectorPlan) {
    cancelMotion()
    container.removeAllViews()
    entries.clear()
    visibleStatisticIds = plan.visibleStatistics.map(StatisticDefinition::id)
    targetSelectedStatisticId = plan.selectedStatistic?.id

    plan.visibleStatistics.forEach { statistic ->
      val view = createView().apply {
        tag = statistic.id
        setContent(statistic, targetSelectedStatisticId == statistic.id)
        setExpanded(targetSelectedStatisticId == statistic.id)
        setOnClickListener { onStatisticSelected(statistic.id) }
      }
      entries[statistic.id] = StatisticEntry(statistic, view)
      container.addView(view)
    }

    container.addView(
      createView().apply {
        setActionContent(R.drawable.ic_add, context.getString(R.string.chart_statistics_add))
        setExpanded(false)
        setOnClickListener { onAddStatistic() }
      }
    )
  }

  private fun createView(): ExpandingView {
    return ExpandingView(context).apply {
      layoutParams = FlexboxLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      ).also { params ->
        params.setMargins(CHIP_MARGIN_DP.dp, CHIP_MARGIN_DP.dp, CHIP_MARGIN_DP.dp, CHIP_MARGIN_DP.dp)
      }
    }
  }

  private fun animateBoundsToSelection(selectedId: String?) {
    animationGeneration++
    TransitionManager.beginDelayedTransition(
      container,
      ChangeBounds().apply {
        duration = BOUNDS_DURATION_MILLIS
        interpolator = motionInterpolator
        container.children.forEach { child -> addTarget(child) }
      }
    )
    applySelection(selectedId)
  }

  private fun animateAcrossReflow(
    selectedId: String?,
    reflowPlan: ReflowPlan
  ) {
    val generation = ++animationGeneration
    reflowAnimator?.cancel()
    reflowAnimator = null
    TransitionManager.endTransitions(container)
    resetChildAlpha()

    val fadeOutAnimator = AnimatorSet().apply {
      playTogether(
        reflowPlan.movingViews.map { view ->
          ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0f)
        }
      )
      duration = FADE_OUT_DURATION_MILLIS
      interpolator = motionInterpolator
      addListener(
        object : AnimatorListenerAdapter() {
          override fun onAnimationEnd(animation: Animator) {
            if (generation != animationGeneration) return

            val startHeight = container.height

            if (reflowPlan.stableViews.isNotEmpty()) {
              TransitionManager.beginDelayedTransition(
                container,
                ChangeBounds().apply {
                  duration = BOUNDS_DURATION_MILLIS
                  interpolator = motionInterpolator
                  reflowPlan.stableViews.forEach(::addTarget)
                }
              )
            }

            container.doOnNextLayout {
              if (generation != animationGeneration) return@doOnNextLayout
              animateChartOffset(
                generation = generation,
                startOffset = (startHeight - container.height).toFloat(),
                movingViews = reflowPlan.movingViews
              )
            }
            applySelection(selectedId)
          }
        }
      )
    }
    reflowAnimator = fadeOutAnimator
    fadeOutAnimator.start()
  }

  private fun applySelection(selectedId: String?) {
    entries.forEach { (id, entry) ->
      val selected = id == selectedId
      entry.view.setContent(entry.statistic, selected)
      entry.view.setExpanded(selected)
    }
  }

  private fun animateChartOffset(
    generation: Int,
    startOffset: Float,
    movingViews: List<View>
  ) {
    chartContainer.translationY = startOffset
    reflowAnimator = AnimatorSet().apply {
      play(
        ValueAnimator.ofFloat(startOffset, 0f).apply {
          addUpdateListener { animation ->
            chartContainer.translationY = animation.animatedValue as Float
          }
        }
      )
      duration = BOUNDS_DURATION_MILLIS
      interpolator = motionInterpolator
      addListener(
        object : AnimatorListenerAdapter() {
          override fun onAnimationEnd(animation: Animator) {
            if (generation != animationGeneration) return
            chartContainer.translationY = 0f
            fadeInMovedViews(generation, movingViews)
          }
        }
      )
      start()
    }
  }

  private fun fadeInMovedViews(generation: Int, movingViews: List<View>) {
    reflowAnimator = AnimatorSet().apply {
      playTogether(
        movingViews.map { view ->
          ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f)
        }
      )
      duration = FADE_IN_DURATION_MILLIS
      interpolator = motionInterpolator
      addListener(
        object : AnimatorListenerAdapter() {
          override fun onAnimationEnd(animation: Animator) {
            if (generation == animationGeneration) {
              reflowAnimator = null
            }
          }
        }
      )
      start()
    }
  }

  private fun updateContent(selectedId: String?) {
    entries.forEach { (id, entry) ->
      entry.view.setContent(entry.statistic, id == selectedId)
    }
  }

  private fun createReflowPlan(selectedId: String?): ReflowPlan? {
    val children = container.children.toList()
    val availableWidth = container.width - container.paddingStart - container.paddingEnd
    if (availableWidth <= 0 || children.isEmpty()) return null

    val currentRows = children.toRowIndicesByPosition()
    val targetRows = children.toRowIndicesByWidth(availableWidth, selectedId)
    val movingIndices = calculateChangedRowIndices(currentRows, targetRows).toSet()
    if (movingIndices.isEmpty()) return null

    return ReflowPlan(
      movingViews = children.filterIndexed { index, _ -> index in movingIndices },
      stableViews = children.filterIndexed { index, _ -> index !in movingIndices }
    )
  }

  private fun List<View>.toRowIndicesByPosition(): List<Int> {
    var row = -1
    var previousTop: Int? = null
    return map { child ->
      if (previousTop != child.top) {
        row++
        previousTop = child.top
      }
      row
    }
  }

  private fun List<View>.toRowIndicesByWidth(
    availableWidth: Int,
    selectedId: String?
  ): List<Int> {
    val outerWidths = map { child ->
      val childStatisticId = child.tag as? String
      val contentWidth = (child as? ExpandingView)?.widthForState(
        childStatisticId != null && childStatisticId == selectedId
      )
        ?: child.measuredWidth
      val margins = child.layoutParams as? ViewGroup.MarginLayoutParams
      contentWidth + (margins?.leftMargin ?: 0) + (margins?.rightMargin ?: 0)
    }
    return calculateFlexRowIndices(availableWidth, outerWidths)
  }

  private fun cancelMotion() {
    animationGeneration++
    reflowAnimator?.cancel()
    reflowAnimator = null
    chartContainer.translationY = 0f
    resetChildAlpha()
    TransitionManager.endTransitions(container)
  }

  private fun resetChildAlpha() {
    container.children.forEach { child -> child.alpha = 1f }
  }

  private data class StatisticEntry(
    var statistic: StatisticDefinition,
    val view: ExpandingView
  )

  private data class ReflowPlan(
    val movingViews: List<View>,
    val stableViews: List<View>
  )

  private companion object {
    const val CHIP_MARGIN_DP = 4
    const val BOUNDS_DURATION_MILLIS = 300L
    const val FADE_OUT_DURATION_MILLIS = 90L
    const val FADE_IN_DURATION_MILLIS = 160L
  }
}

internal fun calculateChangedRowIndices(
  currentRows: List<Int>,
  targetRows: List<Int>
): List<Int> {
  require(currentRows.size == targetRows.size)
  return currentRows.indices.filter { index -> currentRows[index] != targetRows[index] }
}

internal fun calculateFlexRowIndices(
  availableWidth: Int,
  outerWidths: List<Int>
): List<Int> {
  var row = 0
  var occupiedWidth = 0
  return outerWidths.map { outerWidth ->
    if (occupiedWidth > 0 && occupiedWidth + outerWidth > availableWidth) {
      row++
      occupiedWidth = 0
    }
    occupiedWidth += outerWidth
    row
  }
}
