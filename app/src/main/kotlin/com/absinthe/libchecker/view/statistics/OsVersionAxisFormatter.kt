package com.absinthe.libchecker.view.statistics

import com.github.mikephil.charting.formatter.ValueFormatter

class OsVersionAxisFormatter(private val apiList: List<Int>) : ValueFormatter() {
    override fun getFormattedValue(value: Float): String {
        return "API ${apiList[value.toInt()]}"
    }
}
