package com.absinthe.libchecker.features.chart

import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.formatter.IValueFormatter
import com.github.mikephil.charting.utils.ViewPortHandler
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
