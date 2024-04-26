package com.absinthe.libchecker.features.chart

import android.content.Context
import android.view.View
import com.absinthe.libchecker.database.entity.LCItem

sealed interface IChartDataSource<T : View> {
  suspend fun fillChartView(chartView: T)
  fun getListByXValue(x: Int): List<LCItem>
  fun getLabelByXValue(context: Context, x: Int): String
  fun getData(): List<LCItem>
}
