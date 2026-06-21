package com.absinthe.libchecker.features.chart

import android.view.View
import com.absinthe.libchecker.database.entity.LCItem

abstract class BaseChartDataSource<T : View>(val items: List<LCItem>) : IChartDataSource<T> {
  protected abstract val classifiedMap: Map<Int, ChartSourceItem>

  override fun getListByXValue(x: Int): List<LCItem> {
    return classifiedMap[x]?.data.orEmpty()
  }

  override fun getData(): List<LCItem> {
    return items
  }

  fun getChartSourceItems(): Map<Int, ChartSourceItem> {
    return classifiedMap
  }
}
