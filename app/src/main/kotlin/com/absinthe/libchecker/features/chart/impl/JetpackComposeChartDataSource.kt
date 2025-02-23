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

class JetpackComposeChartDataSource(items: List<LCItem>) : BaseChartDataSource<PieChart>(items) {
  override val classifiedMap: HashMap<Int, ChartSourceItem> = HashMap(2)

  override suspend fun fillChartView(chartView: PieChart) {
    withContext(Dispatchers.Default) {
      val context = chartView.context ?: return@withContext
      val parties = listOf(
        context.resources.getString(R.string.string_compose_used),
        context.resources.getString(R.string.string_compose_unused)
      )
      val entries: ArrayList<PieEntry> = ArrayList()
      val colorOnSurface = context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface)
      val classifiedList = listOf(mutableListOf<LCItem>(), mutableListOf())

      for (item in filteredList) {
        if ((item.features and Features.JETPACK_COMPOSE) > 0) {
          classifiedList[COMPOSE_USED].add(item)
        } else {
          classifiedList[COMPOSE_UNUSED].add(item)
        }
      }

      classifiedMap[COMPOSE_USED] = ChartSourceItem(
        com.absinthe.lc.rulesbundle.R.drawable.ic_lib_jetpack_compose,
        false,
        classifiedList[COMPOSE_USED]
      )
      classifiedMap[COMPOSE_UNUSED] = ChartSourceItem(
        com.absinthe.lc.rulesbundle.R.drawable.ic_lib_jetpack_compose,
        true,
        classifiedList[COMPOSE_UNUSED]
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
        "#37bf6e".toColorInt(),
        "#073042".toColorInt()
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
      COMPOSE_USED -> context.getString(R.string.string_compose_used)
      COMPOSE_UNUSED -> context.getString(R.string.string_compose_unused)
      else -> ""
    }
  }

  companion object {
    const val COMPOSE_USED = 0
    const val COMPOSE_UNUSED = 1
  }
}
