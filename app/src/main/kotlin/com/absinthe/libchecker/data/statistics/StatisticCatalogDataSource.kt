package com.absinthe.libchecker.data.statistics

import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDefinition

interface BuiltInStatisticCatalogDataSource {
  suspend fun getStatistics(): List<StatisticDefinition>
}

interface RemoteStatisticCatalogDataSource {
  suspend fun getCachedStatistics(): List<StatisticDefinition>

  suspend fun refreshStatistics(): List<StatisticDefinition>?
}
