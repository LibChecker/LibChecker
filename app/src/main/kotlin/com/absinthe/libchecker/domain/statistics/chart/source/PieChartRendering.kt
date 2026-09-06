package com.absinthe.libchecker.domain.statistics.chart.source

import com.absinthe.libchecker.utils.extensions.getColorByAttr
import info.appdev.charting.charts.PieChart
import info.appdev.charting.data.PieData
import info.appdev.charting.data.PieDataSet
import info.appdev.charting.data.PieEntryFloat
import info.appdev.charting.formatter.PercentFormatter
import info.appdev.charting.utils.PointF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal suspend fun PieChart.showPieData(entries: List<PieEntryFloat>, colors: List<Int>) {
  val colorOnSurface = context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface)
  val dataSet = PieDataSet(entries.toMutableList(), "").apply {
    isDrawIcons = false
    sliceSpace = 3f
    iconsOffset = PointF(0f, 40f)
    selectionShift = 5f
    xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
    yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
    valueLineColor = colorOnSurface
    setColors(colors.toMutableList())
  }
  val pieData = PieData(dataSet).apply {
    setValueFormatter(PercentFormatter())
    setValueTextSize(10f)
    setValueTextColor(colorOnSurface)
  }
  withContext(Dispatchers.Main) {
    data = pieData
    setEntryLabelColor(colorOnSurface)
    highlightValues(null)
    invalidate()
  }
}
