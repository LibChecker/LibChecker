package com.absinthe.libchecker.domain.statistics.chart.repository

import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDefinition

interface StatisticCatalogRepository {
  suspend fun getStatistics(): List<StatisticDefinition>
}
