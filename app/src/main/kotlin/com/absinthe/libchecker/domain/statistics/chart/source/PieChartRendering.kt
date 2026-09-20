package com.absinthe.libchecker.domain.statistics.chart.source

import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.MPPointF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal suspend fun PieChart.showPieData(entries: List<PieEntry<*>>, colors: List<Int>) {
  val colorOnSurface = context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface)
  val dataSet = PieDataSet(entries, "").apply {
    isDrawIconsEnabled = false
    sliceSpace = 3f
    iconsOffset = MPPointF(0f, 40f)
    selectionShift = 5f
    xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
    yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
    valueLinePart2Length = 0.15f
    valueLineColor = colorOnSurface
    this.colors = colors
  }
  val pieData = PieData(dataSet).apply {
    setValueFormatter(PercentFormatter())
    setValueTextSize(10f)
    setValueTextColor(colorOnSurface)
  }
  withContext(Dispatchers.Main) {
    data = pieData
    entryLabelColor = colorOnSurface
    highlightValues(emptyList())
  }
}
