package com.absinthe.libchecker.view.statistics

import com.github.mikephil.charting.formatter.ValueFormatter

class OsVersionAxisFormatter(private val apis: List<Int>) : ValueFormatter() {
  override fun getFormattedValue(value: Float): String {
    return "API ${apis[value.toInt()]}"
  }
}
