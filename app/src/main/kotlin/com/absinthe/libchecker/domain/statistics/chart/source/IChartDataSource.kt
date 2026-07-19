package com.absinthe.libchecker.domain.statistics.chart.source

import android.content.Context
import android.view.View
import com.absinthe.libchecker.database.entity.LCItem

sealed interface IChartDataSource<T : View> {
  suspend fun fillChartView(chartView: T, onProgressUpdated: (Int) -> Unit)
  fun getListByXValue(x: Int): List<LCItem>
  fun getLabelByXValue(context: Context, x: Int): String
  fun getData(): List<LCItem>
  fun getItemChipsByXValue(x: Int): Map<String, List<String>> = emptyMap()
}
