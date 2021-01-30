package com.absinthe.libchecker.view

import com.github.mikephil.charting.formatter.ValueFormatter

class OsVersionAxisFormatter : ValueFormatter() {
    override fun getFormattedValue(value: Float): String {
        return "API ${value.toInt()}"
    }
}