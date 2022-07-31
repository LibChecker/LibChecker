package com.absinthe.libchecker.view.statistics

import com.github.mikephil.charting.formatter.ValueFormatter

class OsVersionAxisFormatter(private val apis: List<Int>) : ValueFormatter() {
  override fun getFormattedValue(value: Float): String {
    if (value < 0) {
      return "API ?"
    }
    return "API ${apis[value.toInt()]}"
  }
}
