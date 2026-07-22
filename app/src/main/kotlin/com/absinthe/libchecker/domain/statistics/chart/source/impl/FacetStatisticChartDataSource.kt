package com.absinthe.libchecker.domain.statistics.chart.source.impl

import android.content.Context
import androidx.core.graphics.toColorInt
import com.absinthe.libchecker.R
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.statistics.chart.model.ChartSourceItem
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticFacetsSpec
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticIconSpec
import com.absinthe.libchecker.domain.statistics.chart.source.BaseChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.source.IHeavyWork
import com.absinthe.libchecker.domain.statistics.chart.ui.resolve
import com.absinthe.libchecker.domain.statistics.chart.ui.summaryTitle
import com.absinthe.libchecker.domain.statistics.chart.usecase.FacetStatisticData
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import info.appdev.charting.charts.PieChart
import info.appdev.charting.data.PieData
import info.appdev.charting.data.PieDataSet
import info.appdev.charting.data.PieEntryFloat
import info.appdev.charting.formatter.PercentFormatter
import info.appdev.charting.utils.PointF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FacetStatisticChartDataSource(
  items: List<LCItem>,
  private val facets: StatisticFacetsSpec,
  private val icon: StatisticIconSpec,
  private val buildData: suspend (
    List<LCItem>,
    StatisticFacetsSpec,
    suspend (Int) -> Unit
  ) -> FacetStatisticData?
) : BaseChartDataSource<PieChart>(items),
  IHeavyWork {
  override val classifiedMap = HashMap<Int, ChartSourceItem>(2)

  override suspend fun fillChartView(chartView: PieChart, onProgressUpdated: (Int) -> Unit) {
    withContext(Dispatchers.Default) {
      val context = chartView.context ?: return@withContext
      val data = buildData(items, facets) { progress ->
        withContext(Dispatchers.Main) {
          onProgressUpdated(progress)
        }
      } ?: return@withContext
      val labels = listOf(
        facets.matchedTitle.resolve(context),
        facets.unmatchedTitle.resolve(context)
      )
      val facetTitles = facets.items.associate { facet ->
        facet.id to facet.summaryTitle.resolve(context)
      }
      val itemChips = data.matchedFacetIds.mapValues { (_, facetIds) ->
        facetIds.mapNotNull(facetTitles::get)
      }
      val classified = listOf(data.matched, data.unmatched)
      classifiedMap.clear()
      classifiedMap[MATCHED] = ChartSourceItem(
        iconRes = R.drawable.ic_chart,
        isGrayIcon = false,
        data = data.matched,
        statisticIcon = icon,
        itemChips = itemChips
      )
      classifiedMap[UNMATCHED] = ChartSourceItem(
        iconRes = R.drawable.ic_chart,
        isGrayIcon = true,
        data = data.unmatched,
        statisticIcon = icon
      )

      val entries = ArrayList<PieEntryFloat>(2)
      labels.forEachIndexed { index, label ->
        entries += PieEntryFloat(classified[index].size.toFloat(), label)
      }
      val colorOnSurface = context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface)
      val dataSet = PieDataSet(entries, "").apply {
        isDrawIcons = false
        sliceSpace = 3f
        iconsOffset = PointF(0f, 40f)
        selectionShift = 5f
        xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        valueLineColor = colorOnSurface
        setColors("#3ddc84".toColorInt(), "#073042".toColorInt())
      }
      val pieData = PieData(dataSet).apply {
        setValueFormatter(PercentFormatter())
        setValueTextSize(10f)
        setValueTextColor(colorOnSurface)
      }

      withContext(Dispatchers.Main) {
        chartView.apply {
          this.data = pieData
          setEntryLabelColor(colorOnSurface)
          highlightValues(null)
          invalidate()
        }
      }
    }
  }

  override fun getLabelByXValue(context: Context, x: Int): String {
    return when (x) {
      MATCHED -> facets.matchedTitle.resolve(context)
      UNMATCHED -> facets.unmatchedTitle.resolve(context)
      else -> ""
    }
  }

  private companion object {
    const val MATCHED = 0
    const val UNMATCHED = 1
  }
}
