package com.absinthe.libchecker.domain.statistics.chart.source.impl

import android.content.Context
import androidx.core.graphics.toColorInt
import com.absinthe.libchecker.R
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.statistics.chart.model.ChartSourceItem
import com.absinthe.libchecker.domain.statistics.chart.source.BaseChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.source.showPieData
import com.absinthe.libchecker.domain.statistics.chart.usecase.BuildFeatureFlagChartDataUseCase.Kind
import com.absinthe.libchecker.domain.statistics.chart.usecase.FeatureFlagChartData
import info.appdev.charting.charts.PieChart
import info.appdev.charting.data.PieEntryFloat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FeatureFlagChartDataSource(
  items: List<LCItem>,
  private val kind: Kind,
  private val buildData: suspend (List<LCItem>, Kind) -> FeatureFlagChartData?
) : BaseChartDataSource<PieChart>(items) {
  override val classifiedMap = HashMap<Int, ChartSourceItem>(2)

  private val labels = when (kind) {
    Kind.Kotlin -> listOf(R.string.string_kotlin_used, R.string.string_kotlin_unused)
    Kind.JetpackCompose -> listOf(R.string.string_compose_used, R.string.string_compose_unused)
    Kind.AppBundle -> listOf(R.string.app_bundle, R.string.apk)
  }
  private val icon = when (kind) {
    Kind.Kotlin -> com.absinthe.lc.rulesbundle.R.drawable.ic_lib_kotlin
    Kind.JetpackCompose -> com.absinthe.lc.rulesbundle.R.drawable.ic_lib_jetpack_compose
    Kind.AppBundle -> R.drawable.ic_aab
  }
  private val colors = when (kind) {
    Kind.Kotlin -> listOf("#7E52FF".toColorInt(), "#D9318E".toColorInt())
    Kind.JetpackCompose -> listOf("#37bf6e".toColorInt(), "#073042".toColorInt())
    Kind.AppBundle -> listOf("#4285f4".toColorInt(), "#3ddc84".toColorInt())
  }

  override suspend fun fillChartView(chartView: PieChart, onProgressUpdated: (Int) -> Unit) {
    withContext(Dispatchers.Default) {
      classifiedMap.clear()
      val data = buildData(items, kind) ?: return@withContext
      val groups = listOf(data.matched, data.unmatched)
      groups.forEachIndexed { index, group ->
        classifiedMap[index] = ChartSourceItem(icon, index == 1, group)
      }
      chartView.showPieData(
        groups.mapIndexed { index, group ->
          PieEntryFloat(group.size.toFloat(), getLabelByXValue(chartView.context, index))
        },
        colors
      )
    }
  }

  override fun getLabelByXValue(context: Context, x: Int): String {
    return labels.getOrNull(x)?.let(context::getString).orEmpty()
  }
}
