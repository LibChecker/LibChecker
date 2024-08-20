package com.absinthe.libchecker.features.chart.impl

import android.content.Context
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.features.chart.ABILabelAxisFormatter
import com.absinthe.libchecker.features.chart.BaseVariableChartDataSource
import com.absinthe.libchecker.features.chart.IntegerFormatter
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import timber.log.Timber

class DetailedABIChartDataSource(items: List<LCItem>) : BaseVariableChartDataSource<BarChart>(items) {
  override suspend fun fillChartView(chartView: BarChart) {
    withContext(Dispatchers.Default) {
      val context = chartView.context ?: return@withContext
      val entries: ArrayList<BarEntry> = ArrayList()

      for (item in filteredList) {
        if (!isActive) {
          return@withContext
        }
        try {
          val pi = PackageUtils.getPackageInfo(item.packageName)
          val abiSet = PackageUtils.getAbiSet(
            file = File(pi.applicationInfo!!.sourceDir),
            packageInfo = pi,
            isApk = false,
            ignoreArch = true
          )
          abiSet.forEach {
            if (classifiedMap[it] == null) {
              classifiedMap[it] = mutableListOf()
            }
            classifiedMap[it]?.add(item)
          }
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
            valueFormatter = ABILabelAxisFormatter(context, classifiedMap.map { entry -> entry.key })
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
    val abi = classifiedMap.entries.elementAtOrNull(x)?.key ?: Constants.ERROR
    return PackageUtils.getAbiString(context, abi, showExtraInfo = false)
  }
}
