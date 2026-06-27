package com.absinthe.libchecker.domain.statistics.chart.source

import info.appdev.charting.components.AxisBase
import info.appdev.charting.formatter.IAxisValueFormatter

class OsVersionAxisFormatter(private val apis: List<Int>) : IAxisValueFormatter {

  override fun getFormattedValue(value: Float, axis: AxisBase?): String {
    if (value < 0) {
      return "API ?"
    }
    return "API ${apis[value.toInt()]}"
  }
}
