package com.absinthe.libchecker.domain.statistics.chart.source.impl

import android.content.Context
import com.absinthe.libchecker.R
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.statistics.chart.source.ABILabelAxisFormatter
import com.absinthe.libchecker.domain.statistics.chart.source.BaseVariableChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.source.IHeavyWork
import com.absinthe.libchecker.domain.statistics.chart.source.applySizeBarData
import com.absinthe.libchecker.domain.statistics.chart.usecase.BuildDetailedKotlinChartDataUseCase
import com.absinthe.libchecker.domain.statistics.chart.usecase.KotlinVersionChartGroup
import com.github.mikephil.charting.charts.BarChart
import java.util.TreeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DetailedKotlinChartDataSource(
  items: List<LCItem>,
  private val buildDetailedKotlinChartData: suspend (List<LCItem>, suspend (Int) -> Unit) -> List<KotlinVersionChartGroup>
) : BaseVariableChartDataSource<BarChart>(items),
  IHeavyWork {
  private val classifiedLabels: MutableMap<Int, String> = TreeMap()

  override suspend fun fillChartView(chartView: BarChart, onProgressUpdated: (Int) -> Unit) {
    withContext(Dispatchers.Default) {
      val context = chartView.context ?: return@withContext
      classifiedMap.clear()
      classifiedLabels.clear()
      buildDetailedKotlinChartData(items) { progress ->
        withContext(Dispatchers.Main) {
          onProgressUpdated(progress)
        }
      }.forEachIndexed { index, group ->
        classifiedMap[index] = group.items.toMutableList()
        classifiedLabels[index] = when (group.version) {
          null -> context.getString(R.string.unknown)
          BuildDetailedKotlinChartDataUseCase.UNUSED -> context.getString(R.string.string_kotlin_unused)
          else -> group.version
        }
      }

      chartView.applySizeBarData(classifiedMap.values.map { it.size }, ABILabelAxisFormatter(classifiedLabels.values.toList()))
    }
  }

  override fun getLabelByXValue(context: Context, x: Int): String {
    return classifiedLabels.entries.elementAtOrNull(x)?.value.orEmpty()
  }
}
