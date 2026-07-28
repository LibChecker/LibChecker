package com.absinthe.libchecker.domain.statistics.chart.model

data class StatisticCatalogEditorState(
  val selectedStatistics: List<StatisticDefinition> = emptyList(),
  val availableStatistics: List<StatisticDefinition> = emptyList(),
  val isRefreshing: Boolean = false,
  val refreshFailed: Boolean = false
) {
  val addableStatistics: List<StatisticDefinition>
    get() {
      val selectedIds = selectedStatistics.mapTo(mutableSetOf(), StatisticDefinition::id)
      return availableStatistics.filterNot { it.id in selectedIds }
    }
}
