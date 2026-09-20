package com.absinthe.libchecker.domain.statistics.chart.source

import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal suspend fun BarChart.applySizeBarData(
  barSizes: List<Int>,
  axisFormatter: IAxisValueFormatter
) {
  val entries = barSizes.mapIndexed { index, size ->
    BarEntry(index.toFloat(), size.toFloat())
  }
  val dataSet = BarDataSet(entries, "").apply {
    isDrawIconsEnabled = false
    valueFormatter = IntegerFormatter()
  }
  val colors = ArrayList<Int>(barSizes.size + 1)
  repeat(barSizes.size + 1) {
    colors.add(UiUtils.getRandomColor())
  }
  dataSet.colors = colors
  val barData = BarData(dataSet).apply {
    setValueTextSize(10f)
    setValueTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface))
  }
  withContext(Dispatchers.Main) {
    xAxis.apply {
      valueFormatter = axisFormatter
      labelCount = barSizes.size
      isForceLabelsEnabled = false
    }
    data = barData
    highlightValues(emptyList())
  }
}
