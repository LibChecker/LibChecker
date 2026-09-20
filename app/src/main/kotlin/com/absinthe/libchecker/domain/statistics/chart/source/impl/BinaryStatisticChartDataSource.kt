package com.absinthe.libchecker.domain.statistics.chart.source.impl

import android.content.Context
import androidx.core.graphics.toColorInt
import com.absinthe.libchecker.R
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.statistics.chart.model.ChartSourceItem
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticIconSpec
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticTitleSpec
import com.absinthe.libchecker.domain.statistics.chart.source.BaseChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.source.IHeavyWork
import com.absinthe.libchecker.domain.statistics.chart.source.showPieData
import com.absinthe.libchecker.domain.statistics.chart.ui.resolve
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class BinaryStatisticChartDataSource(
  items: List<LCItem>,
  private val matchedTitle: StatisticTitleSpec,
  private val unmatchedTitle: StatisticTitleSpec,
  private val icon: StatisticIconSpec,
  private val buildData: suspend (Context, List<LCItem>, suspend (Int) -> Unit) -> BinaryStatisticChartData?
) : BaseChartDataSource<PieChart>(items),
  IHeavyWork {
  override val classifiedMap = HashMap<Int, ChartSourceItem>(2)

  override suspend fun fillChartView(chartView: PieChart, onProgressUpdated: (Int) -> Unit) {
    withContext(Dispatchers.Default) {
      val context = chartView.context ?: return@withContext
      val data = buildData(context, items) { progress ->
        withContext(Dispatchers.Main) {
          onProgressUpdated(progress)
        }
      } ?: return@withContext
      classifiedMap.clear()
      classifiedMap.putAll(data.toChartSourceItems(icon))
      val entries = listOf(matchedTitle, unmatchedTitle).mapIndexed { index, title ->
        PieEntry(classifiedMap.getValue(index).data.size.toFloat(), title.resolve(context))
      }
      chartView.showPieData(entries, listOf("#3ddc84".toColorInt(), "#073042".toColorInt()))
    }
  }

  override fun getLabelByXValue(context: Context, x: Int): String = when (x) {
    0 -> matchedTitle.resolve(context)
    1 -> unmatchedTitle.resolve(context)
    else -> ""
  }
}

internal data class BinaryStatisticChartData(
  val matched: List<LCItem>,
  val unmatched: List<LCItem>,
  val itemChips: Map<String, List<String>> = emptyMap()
) {
  fun toChartSourceItems(icon: StatisticIconSpec): Map<Int, ChartSourceItem> = mapOf(
    0 to ChartSourceItem(R.drawable.ic_chart, false, matched, icon, itemChips),
    1 to ChartSourceItem(R.drawable.ic_chart, true, unmatched, icon)
  )
}
