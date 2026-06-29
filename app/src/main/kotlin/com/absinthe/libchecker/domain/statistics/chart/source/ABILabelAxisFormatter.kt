package com.absinthe.libchecker.domain.statistics.chart.source

import info.appdev.charting.components.AxisBase
import info.appdev.charting.formatter.IAxisValueFormatter

class ABILabelAxisFormatter(private val labels: List<String>) : IAxisValueFormatter {

  override fun getFormattedValue(value: Float, axis: AxisBase?): String {
    return labels.getOrElse(value.toInt()) { "" }
  }
}
