package com.absinthe.libchecker.domain.statistics.chart.presentation

import com.absinthe.libchecker.constant.GlobalFeatures
import com.absinthe.libchecker.domain.statistics.chart.model.LOADING_PROGRESS_MAX
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticAvailability
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDefinition

class ChartUiStatePlanner {

  fun planStatistics(
    definitions: List<StatisticDefinition>,
    currentStatisticId: String?,
    featureChartsAvailable: Boolean
  ): StatisticSelectorPlan {
    val visibleStatistics = definitions
      .filter { definition -> definition.isAvailable() }
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

  private fun StatisticDefinition.isAvailable(): Boolean {
    return when (availability) {
      StatisticAvailability.ALWAYS -> true
      StatisticAvailability.PAGE_SIZE_16_KB -> GlobalFeatures.ENABLE_DETECTING_16KB_PAGE_ALIGNMENT
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
