package com.absinthe.libchecker.domain.statistics.chart.source

import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.list.model.AppListItemViewState
import com.absinthe.libchecker.domain.app.list.usecase.BuildAppListItemViewStatesUseCase
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticPredicateSpec
import com.absinthe.libchecker.domain.statistics.chart.repository.ChartSettingsRepository
import com.absinthe.libchecker.domain.statistics.chart.usecase.AbiChartData
import com.absinthe.libchecker.domain.statistics.chart.usecase.AndroidDistributionChartData
import com.absinthe.libchecker.domain.statistics.chart.usecase.BuildAbiChartDataUseCase
import com.absinthe.libchecker.domain.statistics.chart.usecase.BuildApiLevelChartDataUseCase
import com.absinthe.libchecker.domain.statistics.chart.usecase.BuildDetailedAbiChartDataUseCase
import com.absinthe.libchecker.domain.statistics.chart.usecase.BuildFeatureFlagChartDataUseCase
import com.absinthe.libchecker.domain.statistics.chart.usecase.BuildPageSize16KBChartDataUseCase
import com.absinthe.libchecker.domain.statistics.chart.usecase.BuildPredicateStatisticDataUseCase
import com.absinthe.libchecker.domain.statistics.chart.usecase.DetailedAbiChartData
import com.absinthe.libchecker.domain.statistics.chart.usecase.FeatureFlagChartData
import com.absinthe.libchecker.domain.statistics.chart.usecase.GetAndroidDistributionUseCase
import com.absinthe.libchecker.domain.statistics.chart.usecase.PageSize16KBChartData
import com.absinthe.libchecker.domain.statistics.chart.usecase.PredicateStatisticData

class ChartDataProvider(
  private val buildAppListItemViewStatesUseCase: BuildAppListItemViewStatesUseCase,
  private val buildAbiChartDataUseCase: BuildAbiChartDataUseCase,
  private val buildApiLevelChartDataUseCase: BuildApiLevelChartDataUseCase,
  private val buildDetailedAbiChartDataUseCase: BuildDetailedAbiChartDataUseCase,
  private val buildFeatureFlagChartDataUseCase: BuildFeatureFlagChartDataUseCase,
  private val buildPageSize16KBChartDataUseCase: BuildPageSize16KBChartDataUseCase,
  private val buildPredicateStatisticDataUseCase: BuildPredicateStatisticDataUseCase,
  private val getAndroidDistributionUseCase: GetAndroidDistributionUseCase,
  private val chartSettingsRepository: ChartSettingsRepository
) {

  suspend fun buildAppListItemViewStates(items: List<LCItem>): Map<String, AppListItemViewState> {
    return buildAppListItemViewStatesUseCase(
      BuildAppListItemViewStatesUseCase.Request(
        items = items,
        options = chartSettingsRepository.appListDisplayOptions
      )
    )
  }

  suspend fun buildAbiChartData(items: List<LCItem>): AbiChartData? {
    return buildAbiChartDataUseCase(
      BuildAbiChartDataUseCase.Request(
        items = items,
        showSystemApps = chartSettingsRepository.showSystemApps
      )
    )
  }

  suspend fun buildApiLevelChartData(
    items: List<LCItem>,
    kind: BuildApiLevelChartDataUseCase.Kind
  ): Map<Int, List<LCItem>> {
    return buildApiLevelChartDataUseCase(
      BuildApiLevelChartDataUseCase.Request(
        items = items,
        kind = kind,
        showSystemApps = chartSettingsRepository.showSystemApps
      )
    )
  }

  suspend fun buildDetailedAbiChartData(
    items: List<LCItem>,
    onProgress: suspend (Int) -> Unit
  ): DetailedAbiChartData? {
    return buildDetailedAbiChartDataUseCase(
      BuildDetailedAbiChartDataUseCase.Request(
        items = items,
        showSystemApps = chartSettingsRepository.showSystemApps
      ),
      onProgress
    )
  }

  suspend fun buildFeatureFlagChartData(
    items: List<LCItem>,
    kind: BuildFeatureFlagChartDataUseCase.Kind
  ): FeatureFlagChartData? {
    return buildFeatureFlagChartDataUseCase(
      BuildFeatureFlagChartDataUseCase.Request(
        items = items,
        kind = kind,
        showSystemApps = chartSettingsRepository.showSystemApps
      )
    )
  }

  suspend fun buildPageSize16KBChartData(
    items: List<LCItem>,
    onProgress: suspend (Int) -> Unit
  ): PageSize16KBChartData? {
    return buildPageSize16KBChartDataUseCase(
      BuildPageSize16KBChartDataUseCase.Request(
        items = items,
        showSystemApps = chartSettingsRepository.showSystemApps
      ),
      onProgress
    )
  }

  suspend fun getAndroidDistribution(): AndroidDistributionChartData? {
    return getAndroidDistributionUseCase()
  }

  suspend fun buildPredicateStatisticData(
    items: List<LCItem>,
    predicate: StatisticPredicateSpec,
    onProgress: suspend (Int) -> Unit
  ): PredicateStatisticData? {
    return buildPredicateStatisticDataUseCase(
      BuildPredicateStatisticDataUseCase.Request(
        items = items,
        predicate = predicate,
        showSystemApps = chartSettingsRepository.showSystemApps
      ),
      onProgress
    )
  }
}
