package com.absinthe.libchecker.features.chart.impl

import android.content.Context
import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.api.bean.AndroidDistribution
import com.absinthe.libchecker.api.request.AndroidDistributionRequest
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.features.chart.BaseVariableChartDataSource
import com.absinthe.libchecker.features.chart.OsVersionAxisFormatter
import com.absinthe.libchecker.features.chart.PercentageFormatter
import com.absinthe.libchecker.utils.DateUtils
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.toPercentage
import com.absinthe.libchecker.utils.fromJson
import com.absinthe.libchecker.utils.toJson
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class MarketDistributionChartDataSource(items: List<LCItem>) : BaseVariableChartDataSource<BarChart>(items) {
  var distribution: List<AndroidDistribution>? = null
    private set

  override suspend fun fillChartView(chartView: BarChart) {
    withContext(Dispatchers.Default) {
      val context = chartView.context ?: return@withContext

      distribution = getDistribution(context) ?: let {
        Timber.e("Failed to get distribution")
        return@withContext
      }
      val dist = distribution ?: return@withContext
      val parties = dist.map { it.name }
      val entries: ArrayList<BarEntry> = ArrayList()
      val legendList = mutableListOf<String>()
      for (i in parties.indices) {
        entries.add(BarEntry(i.toFloat(), dist[i].distributionPercentage.toFloat()))
        legendList.add(dist[i].distributionPercentage.toFloat().toPercentage())
      }
      val dataSet = BarDataSet(entries, "").apply {
        setDrawIcons(false)
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
    if (classifiedMap.isEmpty()) {
      return "Load Failed"
    }
    return distribution?.get(x)?.name ?: "Unknown"
  }

  override fun getListKeyByXValue(x: Int): Int? {
    return distribution?.get(x)?.apiLevel
  }

  private suspend fun getDistribution(context: Context): List<AndroidDistribution>? = withContext(Dispatchers.IO) {
    runCatching {
      val localFile = File(File(context.filesDir, "rules"), "android_distribution.json")
      if (!localFile.exists() || !DateUtils.isTimestampThisMonth(GlobalValues.distributionUpdateTimestamp)) {
        val request: AndroidDistributionRequest = ApiManager.create()
        val response = request.requestDistribution()
        localFile.parentFile?.mkdirs()
        if (localFile.exists().not()) {
          localFile.createNewFile()
        }
        localFile.writeText(response.toJson().orEmpty())
        GlobalValues.distributionUpdateTimestamp = System.currentTimeMillis()
        return@withContext response
      }

      val json = localFile.readText()
      return@withContext json.fromJson<List<AndroidDistribution>>(
        List::class.java,
        AndroidDistribution::class.java
      )
    }.onFailure {
      Timber.e(it)
    }.getOrNull()
  }
}
