package com.absinthe.libchecker.domain.statistics.chart.usecase

import com.absinthe.libchecker.api.bean.AndroidDistribution
import com.absinthe.libchecker.domain.statistics.chart.repository.AndroidDistributionRepository

class GetAndroidDistributionUseCase(
  private val androidDistributionRepository: AndroidDistributionRepository
) {

  suspend operator fun invoke(): AndroidDistributionChartData? {
    val distributions = androidDistributionRepository.getDistribution() ?: return null
    return AndroidDistributionChartData(
      distributions = distributions,
      lastUpdateTime = distributions.lastUpdateTime
    )
  }

  private val List<AndroidDistribution>.lastUpdateTime: String
    get() = firstOrNull()
      ?.descriptionBlocks
      ?.find { it.title.isEmpty() }
      ?.body
      ?.removePrefix("Last updated: ")
      .orEmpty()
}

data class AndroidDistributionChartData(
  val distributions: List<AndroidDistribution>,
  val lastUpdateTime: String
)
