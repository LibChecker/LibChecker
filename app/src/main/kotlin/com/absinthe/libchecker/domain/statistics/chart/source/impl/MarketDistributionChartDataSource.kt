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
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
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
      val entries: ArrayList<BarEntry<*>> = ArrayList()
      for (i in parties.indices) {
        entries.add(BarEntry(i.toFloat(), dist[i].distributionPercentage.toFloat()))
      }
      val dataSet = BarDataSet(entries, "").apply {
        isDrawIconsEnabled = false
        valueFormatter = PercentageFormatter()
      }

      // add a lot of colors
      val colors: ArrayList<Int> = ArrayList()
      (0..dist.size).forEach { _ ->
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
            valueFormatter = OsVersionAxisFormatter(dist.map { entry -> entry.apiLevel })
            labelCount = dist.size
            isForceLabelsEnabled = false
          }
          axisLeft.valueFormatter = PercentageFormatter()
          axisRight.valueFormatter = PercentageFormatter()
          this.data = data
          highlightValues(emptyList())
        }
      }
    }
  }

  override fun getLabelByXValue(context: Context, x: Int): String {
    return distribution?.getOrNull(x)?.name ?: "Unknown"
  }

  override fun getListByXValue(x: Int): List<LCItem> {
    return classifiedMap[x] ?: emptyList()
  }

  override fun getListKeyByXValue(x: Int): Int? {
    return distribution?.getOrNull(x)?.apiLevel
  }
}
