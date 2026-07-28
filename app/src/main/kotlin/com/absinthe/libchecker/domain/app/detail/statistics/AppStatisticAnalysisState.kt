package com.absinthe.libchecker.domain.app.detail.statistics

sealed interface AppStatisticAnalysisState {
  data object Idle : AppStatisticAnalysisState

  data class Loading(val progress: Int) : AppStatisticAnalysisState

  data class Results(val analyses: List<AppStatisticRuleAnalysis>) : AppStatisticAnalysisState

  data object Empty : AppStatisticAnalysisState

  data object Error : AppStatisticAnalysisState
}
