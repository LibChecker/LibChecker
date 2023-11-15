package com.absinthe.libchecker.features.chart

import android.view.View
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.database.entity.LCItem
import java.util.TreeMap

abstract class BaseVariableChartDataSource<T : View> : IChartDataSource<T> {
  protected val classifiedMap: MutableMap<Int, MutableList<LCItem>> = TreeMap()
  protected val filteredList = if (GlobalValues.isShowSystemApps.value == true) {
    Repositories.lcRepository.allDatabaseItems.value
  } else {
    Repositories.lcRepository.allDatabaseItems.value?.filter { !it.isSystem }
  }

  override fun getListByXValue(x: Int): List<LCItem> {
    return classifiedMap[x] ?: emptyList()
  }
}
