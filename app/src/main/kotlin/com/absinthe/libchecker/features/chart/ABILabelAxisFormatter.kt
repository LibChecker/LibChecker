package com.absinthe.libchecker.features.chart

import android.content.Context
import com.absinthe.libchecker.utils.PackageUtils
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.formatter.IAxisValueFormatter

class ABILabelAxisFormatter(private val context: Context, private val abis: List<Int>) : IAxisValueFormatter {

  override fun getFormattedValue(value: Float, axis: AxisBase?): String {
    return PackageUtils.getAbiString(context, abis[value.toInt()], false)
  }
}
