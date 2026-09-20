package com.absinthe.libchecker.domain.statistics.chart.source

import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.statistics.chart.model.LOADING_PROGRESS_INFINITY
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticCalculationKind
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDefinition
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticNativeOperator
import com.absinthe.libchecker.domain.statistics.chart.source.impl.ABIChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.source.impl.ApiLevelChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.source.impl.BinaryStatisticChartData
import com.absinthe.libchecker.domain.statistics.chart.source.impl.BinaryStatisticChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.source.impl.DetailedABIChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.source.impl.DetailedKotlinChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.source.impl.FeatureFlagChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.source.impl.MarketDistributionChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.source.impl.PageSize16KBChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.ui.resolve
import com.absinthe.libchecker.domain.statistics.chart.ui.summaryTitle
import com.absinthe.libchecker.domain.statistics.chart.usecase.BuildApiLevelChartDataUseCase
import com.absinthe.libchecker.domain.statistics.chart.usecase.BuildFeatureFlagChartDataUseCase
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart

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

      StatisticCalculationKind.PREDICATE -> {
        val predicate = checkNotNull(statistic.calculation.predicate)
        ChartDataSourcePlan.Pie(
          BinaryStatisticChartDataSource(items, predicate.matchedTitle, predicate.unmatchedTitle, statistic.icon) { _, sourceItems, progress ->
            chartDataProvider.buildPredicateStatisticData(sourceItems, predicate, progress)?.let {
              BinaryStatisticChartData(it.matched, it.unmatched)
            }
          }
        )
      }

      StatisticCalculationKind.FACETS -> {
        val facets = checkNotNull(statistic.calculation.facets)
        ChartDataSourcePlan.Pie(
          BinaryStatisticChartDataSource(items, facets.matchedTitle, facets.unmatchedTitle, statistic.icon) { context, sourceItems, progress ->
            chartDataProvider.buildFacetStatisticData(sourceItems, facets, progress)?.let { data ->
              val titles = facets.items.associateBy(keySelector = { it.id }, valueTransform = { it.summaryTitle.resolve(context) })
              BinaryStatisticChartData(
                data.matched,
                data.unmatched,
                data.matchedFacetIds.mapValues { (_, ids) -> ids.mapNotNull(titles::get) }
              )
            }
          }
        )
      }
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
        if (useDetailedAbiChart) {
          ChartDataSourcePlan.Bar(
            DetailedKotlinChartDataSource(items, chartDataProvider::buildDetailedKotlinChartData)
          )
        } else {
          ChartDataSourcePlan.Pie(
            FeatureFlagChartDataSource(
              items,
              BuildFeatureFlagChartDataUseCase.Kind.Kotlin,
              chartDataProvider::buildFeatureFlagChartData
            )
          )
        }
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
          FeatureFlagChartDataSource(
            items,
            BuildFeatureFlagChartDataUseCase.Kind.JetpackCompose,
            chartDataProvider::buildFeatureFlagChartData
          )
        )
      }

      StatisticNativeOperator.ANDROID_DISTRIBUTION -> {
        ChartDataSourcePlan.Bar(
          MarketDistributionChartDataSource(items, chartDataProvider::getAndroidDistribution)
        )
      }

      StatisticNativeOperator.APP_BUNDLE -> {
        ChartDataSourcePlan.Pie(
          FeatureFlagChartDataSource(
            items,
            BuildFeatureFlagChartDataUseCase.Kind.AppBundle,
            chartDataProvider::buildFeatureFlagChartData
          )
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
