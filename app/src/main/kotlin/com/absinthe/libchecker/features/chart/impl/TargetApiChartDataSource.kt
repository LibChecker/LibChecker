package com.absinthe.libchecker.features.chart.impl

import android.content.Context
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.features.chart.BaseVariableChartDataSource
import com.absinthe.libchecker.features.chart.IAndroidSDKChart
import com.absinthe.libchecker.features.chart.IntegerFormatter
import com.absinthe.libchecker.features.chart.OsVersionAxisFormatter
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import timber.log.Timber

class TargetApiChartDataSource(items: List<LCItem>) :
  BaseVariableChartDataSource<BarChart>(items),
  IAndroidSDKChart {
  override suspend fun fillChartView(chartView: BarChart) {
    withContext(Dispatchers.Default) {
      val context = chartView.context ?: return@withContext
      val entries: ArrayList<BarEntry> = ArrayList()

      var targetApi: Int
      for (item in filteredList) {
        if (!isActive) {
          return@withContext
        }
        try {
          targetApi =
            PackageUtils.getPackageInfo(item.packageName).applicationInfo!!.targetSdkVersion
          if (classifiedMap[targetApi] == null) {
            classifiedMap[targetApi] = mutableListOf()
          }
          classifiedMap[targetApi]?.add(item)
        } catch (e: Exception) {
          Timber.e(e)
        }
      }

      val legendList = mutableListOf<String>()
      var index = 0
      classifiedMap.forEach { entry ->
        entries.add(BarEntry(index.toFloat(), entry.value.size.toFloat()))
        legendList.add(entry.key.toString())
        index++
      }
      val dataSet = BarDataSet(entries, "").apply {
        setDrawIcons(false)
        valueFormatter = IntegerFormatter()
      }

      // add a lot of colors
      val colors: ArrayList<Int> = ArrayList()
      (0..classifiedMap.size).forEach { _ ->
        colors.add(UiUtils.getRandomColor())
      }

      dataSet.colors = colors
      // dataSet.setSelectionShift(0f);
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
    return "Target SDK ${classifiedMap.entries.elementAtOrNull(x)?.key ?: "?"}"
  }
}
