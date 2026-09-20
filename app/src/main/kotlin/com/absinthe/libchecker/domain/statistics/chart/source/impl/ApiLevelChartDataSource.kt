package com.absinthe.libchecker.domain.statistics.chart.source.impl

import android.content.Context
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.statistics.chart.source.BaseVariableChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.source.IAndroidSDKChart
import com.absinthe.libchecker.domain.statistics.chart.source.OsVersionAxisFormatter
import com.absinthe.libchecker.domain.statistics.chart.source.applySizeBarData
import com.absinthe.libchecker.domain.statistics.chart.usecase.BuildApiLevelChartDataUseCase
import com.github.mikephil.charting.charts.BarChart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ApiLevelChartDataSource(
  items: List<LCItem>,
  private val kind: BuildApiLevelChartDataUseCase.Kind,
  private val buildApiLevelChartData: suspend (List<LCItem>, BuildApiLevelChartDataUseCase.Kind) -> Map<Int, List<LCItem>>
) : BaseVariableChartDataSource<BarChart>(items),
  IAndroidSDKChart {

  override suspend fun fillChartView(chartView: BarChart, onProgressUpdated: (Int) -> Unit) {
    withContext(Dispatchers.Default) {
      classifiedMap.clear()
      buildApiLevelChartData(items, kind).forEach { (apiLevel, items) ->
        classifiedMap[apiLevel] = items.toMutableList()
      }

      chartView.applySizeBarData(classifiedMap.values.map { it.size }, OsVersionAxisFormatter(classifiedMap.keys.toList()))
    }
  }

  override fun getLabelByXValue(context: Context, x: Int): String {
    return "${kind.label} ${classifiedMap.entries.elementAtOrNull(x)?.key ?: "?"}"
  }
}

private val BuildApiLevelChartDataUseCase.Kind.label: String
  get() = when (this) {
    BuildApiLevelChartDataUseCase.Kind.TargetSdk -> "Target SDK"
    BuildApiLevelChartDataUseCase.Kind.MinSdk -> "Min SDK"
    BuildApiLevelChartDataUseCase.Kind.CompileSdk -> "Compile SDK"
  }
