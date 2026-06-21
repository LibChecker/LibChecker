package com.absinthe.libchecker.features.chart.impl

import android.content.Context
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.statistics.BuildApiLevelChartDataUseCase
import com.absinthe.libchecker.features.chart.BaseVariableChartDataSource
import com.absinthe.libchecker.features.chart.IAndroidSDKChart
import com.absinthe.libchecker.features.chart.IntegerFormatter
import com.absinthe.libchecker.features.chart.OsVersionAxisFormatter
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import info.appdev.charting.charts.BarChart
import info.appdev.charting.data.BarData
import info.appdev.charting.data.BarDataSet
import info.appdev.charting.data.BarEntryFloat
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
      val context = chartView.context ?: return@withContext
      classifiedMap.clear()
      buildApiLevelChartData(items, kind).forEach { (apiLevel, items) ->
        classifiedMap[apiLevel] = items.toMutableList()
      }

      val entries = ArrayList<BarEntryFloat>()
      var index = 0
      classifiedMap.forEach { entry ->
        entries.add(BarEntryFloat(index.toFloat(), entry.value.size.toFloat()))
        index++
      }

      val dataSet = BarDataSet(entries, "").apply {
        isDrawIcons = false
        valueFormatter = IntegerFormatter()
      }

      val colors = ArrayList<Int>()
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
            valueFormatter = OsVersionAxisFormatter(classifiedMap.map { entry -> entry.key })
            setLabelCount(classifiedMap.size, false)
          }
          this.data = data
          highlightValues(null)
          invalidate()
        }
      }
    }
  }

  override fun getListByXValue(x: Int): List<LCItem> {
    return classifiedMap.entries.elementAtOrNull(x)?.value ?: emptyList()
  }

  override fun getListKeyByXValue(x: Int): Int? {
    return classifiedMap.entries.elementAtOrNull(x)?.key
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
