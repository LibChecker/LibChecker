package com.absinthe.libchecker.features.chart

import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.statistics.BuildApiLevelChartDataUseCase
import com.absinthe.libchecker.domain.statistics.BuildFeatureFlagChartDataUseCase
import com.absinthe.libchecker.features.chart.impl.AABChartDataSource
import com.absinthe.libchecker.features.chart.impl.ABIChartDataSource
import com.absinthe.libchecker.features.chart.impl.ApiLevelChartDataSource
import com.absinthe.libchecker.features.chart.impl.DetailedABIChartDataSource
import com.absinthe.libchecker.features.chart.impl.JetpackComposeChartDataSource
import com.absinthe.libchecker.features.chart.impl.KotlinChartDataSource
import com.absinthe.libchecker.features.chart.impl.MarketDistributionChartDataSource
import com.absinthe.libchecker.features.chart.impl.PageSize16KBChartDataSource
import info.appdev.charting.charts.BarChart
import info.appdev.charting.charts.PieChart

internal class ChartDataSourceFactory(
  private val viewModel: ChartViewModel
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
            DetailedABIChartDataSource(items, viewModel::buildDetailedAbiChartData)
          )
        } else {
          ChartDataSourcePlan.Pie(
            ABIChartDataSource(items, viewModel::buildAbiChartData)
          )
        }
      }

      ChartType.KOTLIN -> {
        ChartDataSourcePlan.Pie(
          KotlinChartDataSource(items) { chartItems ->
            viewModel.buildFeatureFlagChartData(
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
            viewModel::buildApiLevelChartData
          )
        )
      }

      ChartType.MIN_SDK -> {
        ChartDataSourcePlan.Bar(
          ApiLevelChartDataSource(
            items,
            BuildApiLevelChartDataUseCase.Kind.MinSdk,
            viewModel::buildApiLevelChartData
          )
        )
      }

      ChartType.COMPILE_SDK -> {
        ChartDataSourcePlan.Bar(
          ApiLevelChartDataSource(
            items,
            BuildApiLevelChartDataUseCase.Kind.CompileSdk,
            viewModel::buildApiLevelChartData
          )
        )
      }

      ChartType.JETPACK_COMPOSE -> {
        ChartDataSourcePlan.Pie(
          JetpackComposeChartDataSource(items) { chartItems ->
            viewModel.buildFeatureFlagChartData(
              chartItems,
              BuildFeatureFlagChartDataUseCase.Kind.JetpackCompose
            )
          }
        )
      }

      ChartType.MARKET_DISTRIBUTION -> {
        ChartDataSourcePlan.Bar(
          MarketDistributionChartDataSource(items, viewModel::getAndroidDistribution)
        )
      }

      ChartType.AAB -> {
        ChartDataSourcePlan.Pie(
          AABChartDataSource(items) { chartItems ->
            viewModel.buildFeatureFlagChartData(
              chartItems,
              BuildFeatureFlagChartDataUseCase.Kind.AppBundle
            )
          }
        )
      }

      ChartType.SUPPORT_16KB -> {
        ChartDataSourcePlan.Pie(
          PageSize16KBChartDataSource(items, viewModel::buildPageSize16KBChartData)
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
