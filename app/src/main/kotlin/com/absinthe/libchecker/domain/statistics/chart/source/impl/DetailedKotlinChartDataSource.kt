package com.absinthe.libchecker.domain.statistics.chart.source.impl

import android.content.Context
import com.absinthe.libchecker.R
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.statistics.chart.source.ABILabelAxisFormatter
import com.absinthe.libchecker.domain.statistics.chart.source.BaseVariableChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.source.IHeavyWork
import com.absinthe.libchecker.domain.statistics.chart.source.IntegerFormatter
import com.absinthe.libchecker.domain.statistics.chart.usecase.BuildDetailedKotlinChartDataUseCase
import com.absinthe.libchecker.domain.statistics.chart.usecase.KotlinVersionChartGroup
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import info.appdev.charting.charts.BarChart
import info.appdev.charting.data.BarData
import info.appdev.charting.data.BarDataSet
import info.appdev.charting.data.BarEntryFloat
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
      val entries: ArrayList<BarEntryFloat> = ArrayList()
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

      var index = 0
      classifiedMap.forEach { entry ->
        entries.add(BarEntryFloat(index.toFloat(), entry.value.size.toFloat()))
        index++
      }
      val dataSet = BarDataSet(entries, "").apply {
        isDrawIcons = false
        valueFormatter = IntegerFormatter()
      }

      val colors: ArrayList<Int> = ArrayList()
      (0..classifiedMap.size).forEach { _ ->
        colors.add(UiUtils.getRandomColor())
      }

      dataSet.setColors(colors)
      val data = BarData(dataSet).apply {
        setValueTextSize(10f)
        setValueTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface))
      }

      withContext(Dispatchers.Main) {
        chartView.apply {
          xAxis.apply {
            valueFormatter = ABILabelAxisFormatter(classifiedLabels.map { entry -> entry.value })
            setLabelCount(classifiedMap.size, false)
          }
          this.data = data
          highlightValues(null)
          invalidate()
        }
      }
    }
  }

  override fun getLabelByXValue(context: Context, x: Int): String {
    return classifiedLabels.entries.elementAtOrNull(x)?.value.orEmpty()
  }
}
