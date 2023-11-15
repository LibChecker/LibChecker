package com.absinthe.libchecker.features.chart

import android.view.View
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.database.entity.LCItem

abstract class BaseChartDataSource<T : View> : IChartDataSource<T> {
  protected abstract val classifiedList: List<MutableList<LCItem>>
  protected val filteredList = if (GlobalValues.isShowSystemApps.value == true) {
    Repositories.lcRepository.allDatabaseItems.value
  } else {
    Repositories.lcRepository.allDatabaseItems.value?.filter { !it.isSystem }
  }

  override fun getListByXValue(x: Int): List<LCItem> {
    return classifiedList[x]
  }
}
