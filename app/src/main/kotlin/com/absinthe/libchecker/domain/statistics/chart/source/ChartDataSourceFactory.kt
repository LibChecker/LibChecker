package com.absinthe.libchecker.domain.statistics.chart.source

import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.statistics.chart.model.LOADING_PROGRESS_INFINITY
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticCalculationKind
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDefinition
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticNativeOperator
import com.absinthe.libchecker.domain.statistics.chart.source.impl.AABChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.source.impl.ABIChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.source.impl.ApiLevelChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.source.impl.DetailedABIChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.source.impl.FacetStatisticChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.source.impl.JetpackComposeChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.source.impl.KotlinChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.source.impl.MarketDistributionChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.source.impl.PageSize16KBChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.source.impl.PredicateStatisticChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.usecase.BuildApiLevelChartDataUseCase
import com.absinthe.libchecker.domain.statistics.chart.usecase.BuildFeatureFlagChartDataUseCase
import info.appdev.charting.charts.BarChart
import info.appdev.charting.charts.PieChart

internal class ChartDataSourceFactory(
  private val chartDataProvider: ChartDataProvider
) {

  fun create(
    items: List<LCItem>,
    statistic: StatisticDefinition,
    useDetailedAbiChart: Boolean
  ): ChartDataSourcePlan {
    return when (statistic.calculation.kind) {
      StatisticCalculationKind.NATIVE -> createNative(items, statistic, useDetailedAbiChart)

      StatisticCalculationKind.PREDICATE -> ChartDataSourcePlan.Pie(
        PredicateStatisticChartDataSource(
          items = items,
          predicate = checkNotNull(statistic.calculation.predicate),
          icon = statistic.icon,
          buildData = chartDataProvider::buildPredicateStatisticData
        )
      )

      StatisticCalculationKind.FACETS -> ChartDataSourcePlan.Pie(
        FacetStatisticChartDataSource(
          items = items,
          facets = checkNotNull(statistic.calculation.facets),
          icon = statistic.icon,
          buildData = chartDataProvider::buildFacetStatisticData
        )
      )
    }
  }

  private fun createNative(
    items: List<LCItem>,
    statistic: StatisticDefinition,
    useDetailedAbiChart: Boolean
  ): ChartDataSourcePlan {
    return when (statistic.calculation.nativeOperator) {
      StatisticNativeOperator.ABI -> {
        if (useDetailedAbiChart) {
          ChartDataSourcePlan.Bar(
            DetailedABIChartDataSource(items, chartDataProvider::buildDetailedAbiChartData)
          )
        } else {
          ChartDataSourcePlan.Pie(
            ABIChartDataSource(items, chartDataProvider::buildAbiChartData)
          )
        }
      }

      StatisticNativeOperator.KOTLIN -> {
        ChartDataSourcePlan.Pie(
          KotlinChartDataSource(items) { chartItems ->
            chartDataProvider.buildFeatureFlagChartData(
              chartItems,
              BuildFeatureFlagChartDataUseCase.Kind.Kotlin
            )
          }
        )
      }

      StatisticNativeOperator.TARGET_SDK -> {
        ChartDataSourcePlan.Bar(
          ApiLevelChartDataSource(
            items,
            BuildApiLevelChartDataUseCase.Kind.TargetSdk,
            chartDataProvider::buildApiLevelChartData
          )
        )
      }

      StatisticNativeOperator.MIN_SDK -> {
        ChartDataSourcePlan.Bar(
          ApiLevelChartDataSource(
            items,
            BuildApiLevelChartDataUseCase.Kind.MinSdk,
            chartDataProvider::buildApiLevelChartData
          )
        )
      }

      StatisticNativeOperator.COMPILE_SDK -> {
        ChartDataSourcePlan.Bar(
          ApiLevelChartDataSource(
            items,
            BuildApiLevelChartDataUseCase.Kind.CompileSdk,
            chartDataProvider::buildApiLevelChartData
          )
        )
      }

      StatisticNativeOperator.JETPACK_COMPOSE -> {
        ChartDataSourcePlan.Pie(
          JetpackComposeChartDataSource(items) { chartItems ->
            chartDataProvider.buildFeatureFlagChartData(
              chartItems,
              BuildFeatureFlagChartDataUseCase.Kind.JetpackCompose
            )
          }
        )
      }

      StatisticNativeOperator.ANDROID_DISTRIBUTION -> {
        ChartDataSourcePlan.Bar(
          MarketDistributionChartDataSource(items, chartDataProvider::getAndroidDistribution)
        )
      }

      StatisticNativeOperator.APP_BUNDLE -> {
        ChartDataSourcePlan.Pie(
          AABChartDataSource(items) { chartItems ->
            chartDataProvider.buildFeatureFlagChartData(
              chartItems,
              BuildFeatureFlagChartDataUseCase.Kind.AppBundle
            )
          }
        )
      }

      StatisticNativeOperator.PAGE_SIZE_16_KB -> {
        ChartDataSourcePlan.Pie(
          PageSize16KBChartDataSource(items, chartDataProvider::buildPageSize16KBChartData)
        )
      }

      null -> error("Statistic ${statistic.id} does not define a native operator")
    }
  }
}

internal sealed class ChartDataSourcePlan {
  abstract val initialLoadingProgress: Int

  data class Pie(
    val dataSource: IChartDataSource<PieChart>
  ) : ChartDataSourcePlan() {
    override val initialLoadingProgress = dataSource.initialLoadingProgress()
  }

  data class Bar(
    val dataSource: IChartDataSource<BarChart>
  ) : ChartDataSourcePlan() {
    override val initialLoadingProgress = dataSource.initialLoadingProgress()
  }
}

private fun IChartDataSource<*>.initialLoadingProgress(): Int {
  return if (this is IHeavyWork) 0 else LOADING_PROGRESS_INFINITY
}
