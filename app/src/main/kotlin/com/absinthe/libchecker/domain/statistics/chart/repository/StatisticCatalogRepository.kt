package com.absinthe.libchecker.domain.statistics.chart.repository

import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDefinition

interface StatisticCatalogRepository {
  suspend fun getSelectedStatistics(): List<StatisticDefinition>

  suspend fun getAvailableStatistics(): List<StatisticDefinition>

  suspend fun refreshAvailableStatistics(): List<StatisticDefinition>?

  suspend fun setSelectedStatisticIds(ids: List<String>)
}
