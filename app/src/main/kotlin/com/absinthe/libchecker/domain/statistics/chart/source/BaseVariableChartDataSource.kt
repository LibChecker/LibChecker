package com.absinthe.libchecker.domain.statistics.chart.source

import android.view.View
import com.absinthe.libchecker.database.entity.LCItem
import java.util.TreeMap

abstract class BaseVariableChartDataSource<T : View>(val items: List<LCItem>) : IChartDataSource<T> {
  protected val classifiedMap: MutableMap<Int, MutableList<LCItem>> = TreeMap()

  override fun getListByXValue(x: Int): List<LCItem> {
    return classifiedMap[x] ?: emptyList()
  }

  abstract fun getListKeyByXValue(x: Int): Int?

  override fun getData(): List<LCItem> {
    return items
  }
}
