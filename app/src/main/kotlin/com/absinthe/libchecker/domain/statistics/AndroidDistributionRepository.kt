package com.absinthe.libchecker.domain.statistics

import com.absinthe.libchecker.api.bean.AndroidDistribution

interface AndroidDistributionRepository {
  suspend fun getDistribution(): List<AndroidDistribution>?
}
