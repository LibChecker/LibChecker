package com.absinthe.libchecker.domain.statistics.chart.presentation

import com.absinthe.libchecker.domain.statistics.chart.model.ChartType
import com.absinthe.libchecker.domain.statistics.chart.model.LOADING_PROGRESS_INFINITY
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
    chartLoadingProgress: Int,
    featureInitializationPending: Boolean
  ): ChartProgressPlan {
    val progress = when {
      chartLoadingProgress < LOADING_PROGRESS_MAX -> chartLoadingProgress
      featureInitializationPending -> LOADING_PROGRESS_INFINITY
      else -> LOADING_PROGRESS_MAX
    }

    return if (progress < LOADING_PROGRESS_MAX) {
      ChartProgressPlan(
        isVisible = true,
        isIndeterminate = progress == LOADING_PROGRESS_INFINITY,
        progress = progress.coerceAtLeast(0)
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
