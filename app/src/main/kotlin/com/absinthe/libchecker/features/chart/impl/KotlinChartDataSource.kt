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

class KotlinChartDataSource(items: List<LCItem>) : BaseChartDataSource<PieChart>(items) {
  override val classifiedMap: HashMap<Int, ChartSourceItem> = HashMap(2)

  override suspend fun fillChartView(chartView: PieChart) {
    withContext(Dispatchers.Default) {
      val context = chartView.context ?: return@withContext
      val parties = listOf(
        context.resources.getString(R.string.string_kotlin_used),
        context.resources.getString(R.string.string_kotlin_unused)
      )
      val entries: ArrayList<PieEntry> = ArrayList()
      val colorOnSurface = context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface)
      val classifiedList = listOf(mutableListOf<LCItem>(), mutableListOf())

      for (item in filteredList) {
        if ((item.features and Features.KOTLIN_USED) > 0) {
          classifiedList[KOTLIN_USED].add(item)
        } else {
          classifiedList[KOTLIN_UNUSED].add(item)
        }
      }
      classifiedMap[KOTLIN_USED] = ChartSourceItem(
        com.absinthe.lc.rulesbundle.R.drawable.ic_lib_kotlin,
        false,
        classifiedList[KOTLIN_USED]
      )
      classifiedMap[KOTLIN_UNUSED] = ChartSourceItem(
        com.absinthe.lc.rulesbundle.R.drawable.ic_lib_kotlin,
        true,
        classifiedList[KOTLIN_UNUSED]
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
        "#7E52FF".toColorInt(),
        "#D9318E".toColorInt()
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
      KOTLIN_USED -> context.getString(R.string.string_kotlin_used)
      KOTLIN_UNUSED -> context.getString(R.string.string_kotlin_unused)
      else -> ""
    }
  }

  companion object {
    const val KOTLIN_USED = 0
    const val KOTLIN_UNUSED = 1
  }
}
