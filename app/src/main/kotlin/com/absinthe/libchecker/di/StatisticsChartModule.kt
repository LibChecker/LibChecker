package com.absinthe.libchecker.di

import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.data.statistics.AndroidStatisticEvidenceRepository
import com.absinthe.libchecker.data.statistics.AssetStatisticCatalogDataSource
import com.absinthe.libchecker.data.statistics.BuiltInStatisticCatalogDataSource
import com.absinthe.libchecker.data.statistics.CachedAndroidDistributionRepository
import com.absinthe.libchecker.data.statistics.DefaultStatisticCatalogRepository
import com.absinthe.libchecker.data.statistics.GlobalChartSettingsRepository
import com.absinthe.libchecker.data.statistics.HttpOfficialStatisticRemoteSource
import com.absinthe.libchecker.data.statistics.OfficialStatisticBundleStore
import com.absinthe.libchecker.data.statistics.OfficialStatisticCatalogDataSource
import com.absinthe.libchecker.data.statistics.OfficialStatisticRemoteSource
import com.absinthe.libchecker.data.statistics.RemoteStatisticCatalogDataSource
import com.absinthe.libchecker.data.statistics.SharedPreferencesStatisticSelectionStore
import com.absinthe.libchecker.data.statistics.StatisticSelectionStore
import com.absinthe.libchecker.domain.statistics.chart.presentation.ChartViewModel
import com.absinthe.libchecker.domain.statistics.chart.repository.AndroidDistributionRepository
import com.absinthe.libchecker.domain.statistics.chart.repository.ChartSettingsRepository
import com.absinthe.libchecker.domain.statistics.chart.repository.StatisticCatalogRepository
import com.absinthe.libchecker.domain.statistics.chart.repository.StatisticEvidenceRepository
import com.absinthe.libchecker.domain.statistics.chart.source.ChartDataProvider
import com.absinthe.libchecker.domain.statistics.chart.source.ChartDataSourceFactory
import com.absinthe.libchecker.domain.statistics.chart.usecase.BuildAbiChartDataUseCase
import com.absinthe.libchecker.domain.statistics.chart.usecase.BuildAndroidVersionLabelDisplayDataUseCase
import com.absinthe.libchecker.domain.statistics.chart.usecase.BuildApiLevelChartDataUseCase
import com.absinthe.libchecker.domain.statistics.chart.usecase.BuildDetailedAbiChartDataUseCase
import com.absinthe.libchecker.domain.statistics.chart.usecase.BuildFacetStatisticDataUseCase
import com.absinthe.libchecker.domain.statistics.chart.usecase.BuildFeatureFlagChartDataUseCase
import com.absinthe.libchecker.domain.statistics.chart.usecase.BuildPageSize16KBChartDataUseCase
import com.absinthe.libchecker.domain.statistics.chart.usecase.BuildPredicateStatisticDataUseCase
import com.absinthe.libchecker.domain.statistics.chart.usecase.GetAndroidDistributionUseCase
import com.absinthe.libchecker.domain.statistics.chart.usecase.ObserveChartFeatureInitializationPlansUseCase
import com.absinthe.libchecker.domain.statistics.chart.usecase.ValidateStatisticCatalogUseCase
import com.absinthe.libchecker.domain.statistics.chart.usecase.ValidateStatisticSvgUseCase
import com.absinthe.libchecker.utils.SPUtils
import java.io.File
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val statisticsChartModule = module {
  single<AndroidDistributionRepository> { CachedAndroidDistributionRepository(androidContext()) }
  single<ChartSettingsRepository> { GlobalChartSettingsRepository() }
  single<BuiltInStatisticCatalogDataSource> {
    AssetStatisticCatalogDataSource(androidContext(), get())
  }
  single<OfficialStatisticRemoteSource> {
    HttpOfficialStatisticRemoteSource(
      client = ApiManager.okHttpClient,
      manifestUrl = ApiManager.chartRulesManifestUrl,
      bundleUrl = ApiManager.chartRulesBundleUrl
    )
  }
  single {
    OfficialStatisticBundleStore(
      rootDirectory = File(androidContext().filesDir, "statistics/official/v1"),
      validateCatalog = get(),
      validateSvg = get()
    )
  }
  single<RemoteStatisticCatalogDataSource> {
    OfficialStatisticCatalogDataSource(
      remoteSource = get(),
      bundleStore = get(),
      appVersionCode = BuildConfig.VERSION_CODE.toLong()
    )
  }
  single<StatisticSelectionStore> { SharedPreferencesStatisticSelectionStore(SPUtils.sp) }
  single<StatisticEvidenceRepository> { AndroidStatisticEvidenceRepository(get()) }
  single<StatisticCatalogRepository> {
    DefaultStatisticCatalogRepository(
      builtIn = get(),
      official = get(),
      selectionStore = get()
    )
  }

  factory { BuildAbiChartDataUseCase() }
  factory { BuildAndroidVersionLabelDisplayDataUseCase() }
  factory { BuildApiLevelChartDataUseCase(get()) }
  factory { BuildDetailedAbiChartDataUseCase(androidContext(), get()) }
  factory { BuildFeatureFlagChartDataUseCase() }
  factory { BuildPageSize16KBChartDataUseCase(get()) }
  factory { BuildPredicateStatisticDataUseCase(get()) }
  factory { BuildFacetStatisticDataUseCase(get()) }
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
      buildPredicateStatisticDataUseCase = get(),
      buildFacetStatisticDataUseCase = get(),
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
