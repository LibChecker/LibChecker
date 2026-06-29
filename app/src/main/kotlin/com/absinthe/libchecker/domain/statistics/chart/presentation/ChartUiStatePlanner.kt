package com.absinthe.libchecker.domain.statistics.chart.presentation

import com.absinthe.libchecker.domain.statistics.chart.model.ChartType
import com.absinthe.libchecker.domain.statistics.chart.model.LOADING_PROGRESS_MAX

class ChartUiStatePlanner {

  fun planChartTypes(
    currentChartType: ChartType,
    featureChartsAvailable: Boolean
  ): ChartTypeSelectorPlan {
    val visibleTypes = ChartType.availableTypes()
      .filter { featureChartsAvailable || !it.requiresFeatureInitialization }
    return ChartTypeSelectorPlan(
      visibleTypes = visibleTypes,
      selectedType = currentChartType.takeIf { it in visibleTypes }
        ?: visibleTypes.firstOrNull()
        ?: ChartType.ABI
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

data class ChartTypeSelectorPlan(
  val visibleTypes: List<ChartType>,
  val selectedType: ChartType
)

data class ChartProgressPlan(
  val isVisible: Boolean,
  val isIndeterminate: Boolean,
  val progress: Int
)
