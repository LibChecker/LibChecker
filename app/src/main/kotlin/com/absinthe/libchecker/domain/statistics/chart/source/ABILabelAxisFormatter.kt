package com.absinthe.libchecker.domain.statistics.chart.source

import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.formatter.IAxisValueFormatter

class ABILabelAxisFormatter(private val labels: List<String>) : IAxisValueFormatter {

  override fun getFormattedValue(value: Float, axis: AxisBase): String {
    return labels.getOrElse(value.toInt()) { "" }
  }
}
