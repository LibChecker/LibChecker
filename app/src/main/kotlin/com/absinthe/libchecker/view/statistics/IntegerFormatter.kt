package com.absinthe.libchecker.view.statistics

import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.DecimalFormat

class IntegerFormatter : ValueFormatter() {

  private val formatter = DecimalFormat("###,###,##0")

  override fun getFormattedValue(value: Float, axis: AxisBase?): String {
    return formatter.format(value)
  }

  override fun getFormattedValue(value: Float): String {
    return formatter.format(value)
  }
}
