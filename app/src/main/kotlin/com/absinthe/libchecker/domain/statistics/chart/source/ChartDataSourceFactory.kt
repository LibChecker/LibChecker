package com.absinthe.libchecker.domain.statistics.chart.source

import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.statistics.BuildApiLevelChartDataUseCase
import com.absinthe.libchecker.domain.statistics.BuildFeatureFlagChartDataUseCase
import com.absinthe.libchecker.domain.statistics.ChartDataProvider
import com.absinthe.libchecker.domain.statistics.chart.model.ChartType
import com.absinthe.libchecker.domain.statistics.chart.model.LOADING_PROGRESS_INFINITY
import com.absinthe.libchecker.domain.statistics.chart.source.impl.AABChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.source.impl.ABIChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.source.impl.ApiLevelChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.source.impl.DetailedABIChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.source.impl.JetpackComposeChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.source.impl.KotlinChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.source.impl.MarketDistributionChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.source.impl.PageSize16KBChartDataSource
import info.appdev.charting.charts.BarChart
import info.appdev.charting.charts.PieChart

internal class ChartDataSourceFactory(
  private val chartDataProvider: ChartDataProvider
) {

  fun create(
    items: List<LCItem>,
    chartType: ChartType,
    useDetailedAbiChart: Boolean
  ): ChartDataSourcePlan {
    return when (chartType) {
      ChartType.ABI -> {
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

      ChartType.KOTLIN -> {
        ChartDataSourcePlan.Pie(
          KotlinChartDataSource(items) { chartItems ->
            chartDataProvider.buildFeatureFlagChartData(
              chartItems,
              BuildFeatureFlagChartDataUseCase.Kind.Kotlin
            )
          }
        )
      }

      ChartType.TARGET_SDK -> {
        ChartDataSourcePlan.Bar(
          ApiLevelChartDataSource(
            items,
            BuildApiLevelChartDataUseCase.Kind.TargetSdk,
            chartDataProvider::buildApiLevelChartData
          )
        )
      }

      ChartType.MIN_SDK -> {
        ChartDataSourcePlan.Bar(
          ApiLevelChartDataSource(
            items,
            BuildApiLevelChartDataUseCase.Kind.MinSdk,
            chartDataProvider::buildApiLevelChartData
          )
        )
      }

      ChartType.COMPILE_SDK -> {
        ChartDataSourcePlan.Bar(
          ApiLevelChartDataSource(
            items,
            BuildApiLevelChartDataUseCase.Kind.CompileSdk,
            chartDataProvider::buildApiLevelChartData
          )
        )
      }

      ChartType.JETPACK_COMPOSE -> {
        ChartDataSourcePlan.Pie(
          JetpackComposeChartDataSource(items) { chartItems ->
            chartDataProvider.buildFeatureFlagChartData(
              chartItems,
              BuildFeatureFlagChartDataUseCase.Kind.JetpackCompose
            )
          }
        )
      }

      ChartType.MARKET_DISTRIBUTION -> {
        ChartDataSourcePlan.Bar(
          MarketDistributionChartDataSource(items, chartDataProvider::getAndroidDistribution)
        )
      }

      ChartType.AAB -> {
        ChartDataSourcePlan.Pie(
          AABChartDataSource(items) { chartItems ->
            chartDataProvider.buildFeatureFlagChartData(
              chartItems,
              BuildFeatureFlagChartDataUseCase.Kind.AppBundle
            )
          }
        )
      }

      ChartType.SUPPORT_16KB -> {
        ChartDataSourcePlan.Pie(
          PageSize16KBChartDataSource(items, chartDataProvider::buildPageSize16KBChartData)
        )
      }
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
