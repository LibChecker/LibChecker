package com.absinthe.libchecker.features.chart

import info.appdev.charting.components.AxisBase
import info.appdev.charting.data.Entry
import info.appdev.charting.formatter.IAxisValueFormatter
import info.appdev.charting.formatter.IValueFormatter
import info.appdev.charting.utils.ViewPortHandler
import java.text.DecimalFormat

class PercentageFormatter :
  IAxisValueFormatter,
  IValueFormatter {

  private val formatter = DecimalFormat("###,###,##0.00%")

  override fun getFormattedValue(value: Float, axis: AxisBase?): String {
    if (value == 0f) {
      return "0"
    }
    return formatter.format(value)
  }

  override fun getFormattedValue(
    value: Float,
    entry: Entry?,
    dataSetIndex: Int,
    viewPortHandler: ViewPortHandler?
  ): String {
    if (value == 0f) {
      return "0"
    }
    return formatter.format(value)
  }
}
