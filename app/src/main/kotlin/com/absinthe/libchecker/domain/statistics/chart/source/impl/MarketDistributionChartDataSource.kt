package com.absinthe.libchecker.domain.statistics.chart.source.impl

import android.content.Context
import com.absinthe.libchecker.api.bean.AndroidDistribution
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.statistics.chart.source.BaseVariableChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.source.OsVersionAxisFormatter
import com.absinthe.libchecker.domain.statistics.chart.source.PercentageFormatter
import com.absinthe.libchecker.domain.statistics.chart.usecase.AndroidDistributionChartData
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import info.appdev.charting.charts.BarChart
import info.appdev.charting.data.BarData
import info.appdev.charting.data.BarDataSet
import info.appdev.charting.data.BarEntryFloat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class MarketDistributionChartDataSource(
  items: List<LCItem>,
  private val getAndroidDistribution: suspend () -> AndroidDistributionChartData?
) : BaseVariableChartDataSource<BarChart>(items) {
  var distribution: List<AndroidDistribution>? = null
    private set
  var lastUpdateTime: String = ""
    private set

  override suspend fun fillChartView(chartView: BarChart, onProgressUpdated: (Int) -> Unit) {
    withContext(Dispatchers.Default) {
      val context = chartView.context ?: return@withContext

      val chartData = getAndroidDistribution() ?: let {
        Timber.e("Failed to get distribution")
        return@withContext
      }
      distribution = chartData.distributions
      lastUpdateTime = chartData.lastUpdateTime
      val dist = chartData.distributions
      val parties = dist.map { it.name }
      val entries: ArrayList<BarEntryFloat> = ArrayList()
      for (i in parties.indices) {
        entries.add(BarEntryFloat(i.toFloat(), dist[i].distributionPercentage.toFloat()))
      }
      val dataSet = BarDataSet(entries, "").apply {
        isDrawIcons = false
        valueFormatter = PercentageFormatter()
      }

      // add a lot of colors
      val colors: ArrayList<Int> = ArrayList()
      (0..dist.size).forEach { _ ->
        colors.add(UiUtils.getRandomColor())
      }

      dataSet.setColors(colors)
      // dataSet.setSelectionShift(0f);
      val data = BarData(dataSet).apply {
        setValueTextSize(10f)
        setValueTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface))
      }

      chartView.apply {
        xAxis.apply {
          valueFormatter = OsVersionAxisFormatter(dist.map { entry -> entry.apiLevel })
          setLabelCount(dist.size, false)
        }
        axisLeft.valueFormatter = PercentageFormatter()
        axisRight.valueFormatter = PercentageFormatter()
        this.data = data
      }

      withContext(Dispatchers.Main) {
        chartView.apply {
          highlightValues(null)
          invalidate()
        }
      }
    }
  }

  override fun getLabelByXValue(context: Context, x: Int): String {
    return distribution?.getOrNull(x)?.name ?: "Unknown"
  }

  override fun getListKeyByXValue(x: Int): Int? {
    return distribution?.getOrNull(x)?.apiLevel
  }
}
