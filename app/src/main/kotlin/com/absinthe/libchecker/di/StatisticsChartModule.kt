package com.absinthe.libchecker.di

import com.absinthe.libchecker.data.statistics.AssetStatisticCatalogRepository
import com.absinthe.libchecker.data.statistics.CachedAndroidDistributionRepository
import com.absinthe.libchecker.data.statistics.GlobalChartSettingsRepository
import com.absinthe.libchecker.domain.statistics.chart.presentation.ChartViewModel
import com.absinthe.libchecker.domain.statistics.chart.repository.AndroidDistributionRepository
import com.absinthe.libchecker.domain.statistics.chart.repository.ChartSettingsRepository
import com.absinthe.libchecker.domain.statistics.chart.repository.StatisticCatalogRepository
import com.absinthe.libchecker.domain.statistics.chart.source.ChartDataProvider
import com.absinthe.libchecker.domain.statistics.chart.source.ChartDataSourceFactory
import com.absinthe.libchecker.domain.statistics.chart.usecase.BuildAbiChartDataUseCase
import com.absinthe.libchecker.domain.statistics.chart.usecase.BuildAndroidVersionLabelDisplayDataUseCase
import com.absinthe.libchecker.domain.statistics.chart.usecase.BuildApiLevelChartDataUseCase
import com.absinthe.libchecker.domain.statistics.chart.usecase.BuildDetailedAbiChartDataUseCase
import com.absinthe.libchecker.domain.statistics.chart.usecase.BuildFeatureFlagChartDataUseCase
import com.absinthe.libchecker.domain.statistics.chart.usecase.BuildPageSize16KBChartDataUseCase
import com.absinthe.libchecker.domain.statistics.chart.usecase.GetAndroidDistributionUseCase
import com.absinthe.libchecker.domain.statistics.chart.usecase.ObserveChartFeatureInitializationPlansUseCase
import com.absinthe.libchecker.domain.statistics.chart.usecase.ValidateStatisticCatalogUseCase
import com.absinthe.libchecker.domain.statistics.chart.usecase.ValidateStatisticSvgUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val statisticsChartModule = module {
  single<AndroidDistributionRepository> { CachedAndroidDistributionRepository(androidContext()) }
  single<ChartSettingsRepository> { GlobalChartSettingsRepository() }
  single<StatisticCatalogRepository> {
    AssetStatisticCatalogRepository(androidContext(), get(), get())
  }

  factory { BuildAbiChartDataUseCase() }
  factory { BuildAndroidVersionLabelDisplayDataUseCase() }
  factory { BuildApiLevelChartDataUseCase(get()) }
  factory { BuildDetailedAbiChartDataUseCase(androidContext(), get()) }
  factory { BuildFeatureFlagChartDataUseCase() }
  factory { BuildPageSize16KBChartDataUseCase(get()) }
  factory { GetAndroidDistributionUseCase(get()) }
  factory { ObserveChartFeatureInitializationPlansUseCase(get()) }
  factory { ValidateStatisticCatalogUseCase() }
  factory { ValidateStatisticSvgUseCase() }
  factory {
    ChartDataProvider(
      buildAppListItemViewStatesUseCase = get(),
      buildAbiChartDataUseCase = get(),
      buildApiLevelChartDataUseCase = get(),
      buildDetailedAbiChartDataUseCase = get(),
      buildFeatureFlagChartDataUseCase = get(),
      buildPageSize16KBChartDataUseCase = get(),
      getAndroidDistributionUseCase = get(),
      chartSettingsRepository = get()
    )
  }
  factory { ChartDataSourceFactory(get()) }

  viewModel {
    ChartViewModel(
      appListRepository = get(),
      chartDataProvider = get(),
      chartDataSourceFactory = get(),
      statisticCatalogRepository = get(),
      chartSettingsRepository = get(),
      observeChartFeatureInitializationPlans = get(),
      buildAndroidVersionLabelDisplayData = get()
    )
  }
}
