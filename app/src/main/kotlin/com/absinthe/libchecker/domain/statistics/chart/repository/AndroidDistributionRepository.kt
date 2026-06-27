package com.absinthe.libchecker.domain.statistics.chart.repository

import com.absinthe.libchecker.api.bean.AndroidDistribution

interface AndroidDistributionRepository {
  suspend fun getDistribution(): List<AndroidDistribution>?
}
