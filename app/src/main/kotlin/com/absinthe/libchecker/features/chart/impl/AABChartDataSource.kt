package com.absinthe.libchecker.features.chart.impl

import android.content.Context
import androidx.core.graphics.toColorInt
import com.absinthe.libchecker.R
import com.absinthe.libchecker.database.entity.Features
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.features.chart.BaseChartDataSource
import com.absinthe.libchecker.features.chart.ChartSourceItem
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.MPPointF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AABChartDataSource(items: List<LCItem>) : BaseChartDataSource<PieChart>(items) {
  override val classifiedMap: HashMap<Int, ChartSourceItem> = HashMap(2)

  override suspend fun fillChartView(chartView: PieChart) {
    withContext(Dispatchers.Default) {
      val context = chartView.context ?: return@withContext
      val parties = listOf(
        context.resources.getString(R.string.app_bundle),
        context.resources.getString(R.string.apk)
      )
      val entries: ArrayList<PieEntry> = ArrayList()
      val colorOnSurface = context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface)
      val classifiedList = listOf(mutableListOf<LCItem>(), mutableListOf())

      for (item in filteredList) {
        if ((item.features and Features.SPLIT_APKS) > 0) {
          classifiedList[AAB].add(item)
        } else {
          classifiedList[APK].add(item)
        }
      }
      classifiedMap[AAB] = ChartSourceItem(
        R.drawable.ic_aab,
        false,
        classifiedList[AAB]
      )
      classifiedMap[APK] = ChartSourceItem(
        R.drawable.ic_aab,
        true,
        classifiedList[APK]
      )

      // NOTE: The order of the entries when being added to the entries array determines their position around the center of
      // the chart.
      val legendList = mutableListOf<String>()
      for (i in parties.indices) {
        entries.add(PieEntry(classifiedList[i].size.toFloat(), parties[i % parties.size]))
        legendList.add(parties[i % parties.size])
      }
      val dataSet = PieDataSet(entries, "").apply {
        setDrawIcons(false)
        sliceSpace = 3f
        iconsOffset = MPPointF(0f, 40f)
        selectionShift = 5f
        xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        valueLineColor = context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface)
      }

      // add a lot of colors
      val colors = arrayListOf(
        "#4285f4".toColorInt(),
        "#3ddc84".toColorInt()
      )

      dataSet.colors = colors
      // dataSet.setSelectionShift(0f);
      val data = PieData(dataSet).apply {
        setValueFormatter(PercentFormatter())
        setValueTextSize(10f)
        setValueTextColor(colorOnSurface)
      }

      withContext(Dispatchers.Main) {
        chartView.apply {
          this.data = data
          setEntryLabelColor(colorOnSurface)
          highlightValues(null)
          invalidate()
        }
      }
    }
  }

  override fun getLabelByXValue(context: Context, x: Int): String {
    return when (x) {
      AAB -> context.getString(R.string.app_bundle)
      APK -> context.getString(R.string.apk)
      else -> ""
    }
  }

  companion object {
    const val AAB = 0
    const val APK = 1
  }
}
