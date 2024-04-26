package com.absinthe.libchecker.features.chart

import android.view.View
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.database.entity.LCItem

abstract class BaseChartDataSource<T : View>(val items: List<LCItem>) : IChartDataSource<T> {
  protected abstract val classifiedList: List<MutableList<LCItem>>

  protected val filteredList = if (GlobalValues.isShowSystemApps) {
    items
  } else {
    items.filter { !it.isSystem }
  }

  override fun getListByXValue(x: Int): List<LCItem> {
    return classifiedList[x]
  }

  override fun getData(): List<LCItem> {
    return items
  }
}
