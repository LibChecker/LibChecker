package com.absinthe.libchecker.domain.statistics.chart.presentation

import com.absinthe.libchecker.domain.statistics.chart.model.LOADING_PROGRESS_MAX
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDefinition

class ChartUiStatePlanner {

  fun planStatistics(
    definitions: List<StatisticDefinition>,
    currentStatisticId: String?,
    featureChartsAvailable: Boolean
  ): StatisticSelectorPlan {
    val visibleStatistics = definitions
      .filter { definition -> featureChartsAvailable || !definition.requiresFeatureInitialization }
    return StatisticSelectorPlan(
      visibleStatistics = visibleStatistics,
      selectedStatistic = visibleStatistics.find { it.id == currentStatisticId }
        ?: visibleStatistics.firstOrNull()
    )
  }

  fun planProgress(
    chartLoadingProgress: Int
  ): ChartProgressPlan {
    return if (chartLoadingProgress < LOADING_PROGRESS_MAX) {
      ChartProgressPlan(
        isVisible = true,
        isIndeterminate = chartLoadingProgress < 0,
        progress = chartLoadingProgress.coerceAtLeast(0)
      )
    } else {
      ChartProgressPlan(
        isVisible = false,
        isIndeterminate = false,
        progress = LOADING_PROGRESS_MAX
      )
    }
  }
}

data class StatisticSelectorPlan(
  val visibleStatistics: List<StatisticDefinition>,
  val selectedStatistic: StatisticDefinition?
)

data class ChartProgressPlan(
  val isVisible: Boolean,
  val isIndeterminate: Boolean,
  val progress: Int
)
