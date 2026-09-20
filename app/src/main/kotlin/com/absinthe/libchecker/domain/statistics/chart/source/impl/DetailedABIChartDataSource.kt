package com.absinthe.libchecker.domain.statistics.chart.source.impl

import android.content.Context
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.statistics.chart.source.ABILabelAxisFormatter
import com.absinthe.libchecker.domain.statistics.chart.source.BaseVariableChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.source.IHeavyWork
import com.absinthe.libchecker.domain.statistics.chart.source.applySizeBarData
import com.absinthe.libchecker.domain.statistics.chart.usecase.DetailedAbiChartData
import com.github.mikephil.charting.charts.BarChart
import java.util.TreeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DetailedABIChartDataSource(
  items: List<LCItem>,
  private val buildDetailedAbiChartData: suspend (List<LCItem>, suspend (Int) -> Unit) -> DetailedAbiChartData?
) : BaseVariableChartDataSource<BarChart>(items),
  IHeavyWork {
  private val classifiedLabels: MutableMap<Int, String> = TreeMap()

  override suspend fun fillChartView(chartView: BarChart, onProgressUpdated: (Int) -> Unit) {
    withContext(Dispatchers.Default) {
      classifiedMap.clear()
      classifiedLabels.clear()
      buildDetailedAbiChartData(items) { progress ->
        withContext(Dispatchers.Main) {
          onProgressUpdated(progress)
        }
      }?.groups?.forEach { group ->
        classifiedMap[group.abi] = group.items.toMutableList()
        classifiedLabels[group.abi] = group.label
      } ?: return@withContext

      chartView.applySizeBarData(classifiedMap.values.map { it.size }, ABILabelAxisFormatter(classifiedLabels.values.toList()))
    }
  }

  override fun getLabelByXValue(context: Context, x: Int): String {
    return classifiedLabels.entries.elementAtOrNull(x)?.value.orEmpty()
  }
}
