package com.absinthe.libchecker.data.statistics

import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDefinition
import com.absinthe.libchecker.domain.statistics.chart.repository.StatisticCatalogRepository

class DefaultStatisticCatalogRepository(
  private val builtIn: BuiltInStatisticCatalogDataSource,
  private val official: RemoteStatisticCatalogDataSource,
  private val selectionStore: StatisticSelectionStore
) : StatisticCatalogRepository {

  override suspend fun getSelectedStatistics(): List<StatisticDefinition> {
    val builtInStatistics = builtIn.getStatistics()
    val availableStatistics = merge(builtInStatistics, official.getCachedStatistics())
    val selectedIds = selectionStore.getSelectedStatisticIds()
      ?: builtInStatistics.map(StatisticDefinition::id).also {
        selectionStore.setSelectedStatisticIds(it)
      }
    return resolveSelectedStatistics(selectedIds, availableStatistics)
  }

  override suspend fun getAvailableStatistics(): List<StatisticDefinition> {
    return merge(builtIn.getStatistics(), official.getCachedStatistics())
  }

  override suspend fun refreshAvailableStatistics(): List<StatisticDefinition>? {
    val officialStatistics = official.refreshStatistics() ?: return null
    return merge(builtIn.getStatistics(), officialStatistics)
  }

  override suspend fun setSelectedStatisticIds(ids: List<String>) {
    selectionStore.setSelectedStatisticIds(ids.distinct())
  }

  private fun merge(
    builtInStatistics: List<StatisticDefinition>,
    officialStatistics: List<StatisticDefinition>
  ): List<StatisticDefinition> {
    return (builtInStatistics + officialStatistics).distinctBy { it.id }
  }

  private fun resolveSelectedStatistics(
    selectedIds: List<String>,
    availableStatistics: List<StatisticDefinition>
  ): List<StatisticDefinition> {
    val definitionsById = availableStatistics.associateBy(StatisticDefinition::id)
    return selectedIds.mapNotNull(definitionsById::get)
  }
}
