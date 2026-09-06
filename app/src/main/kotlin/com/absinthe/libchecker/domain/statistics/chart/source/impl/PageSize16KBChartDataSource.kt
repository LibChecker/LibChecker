package com.absinthe.libchecker.domain.statistics.chart.source.impl

import android.content.Context
import com.absinthe.libchecker.R
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.statistics.chart.model.ChartSourceItem
import com.absinthe.libchecker.domain.statistics.chart.source.BaseChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.source.IHeavyWork
import com.absinthe.libchecker.domain.statistics.chart.source.showPieData
import com.absinthe.libchecker.domain.statistics.chart.usecase.PageSize16KBChartData
import com.absinthe.libchecker.utils.OsUtils
import info.appdev.charting.charts.PieChart
import info.appdev.charting.data.PieEntryFloat
import info.appdev.charting.utils.ColorTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PageSize16KBChartDataSource(
  items: List<LCItem>,
  private val buildPageSize16KBChartData: suspend (List<LCItem>, suspend (Int) -> Unit) -> PageSize16KBChartData?
) : BaseChartDataSource<PieChart>(items),
  IHeavyWork {
  override val classifiedMap: HashMap<Int, ChartSourceItem> = HashMap(2)

  override suspend fun fillChartView(chartView: PieChart, onProgressUpdated: (Int) -> Unit) {
    withContext(Dispatchers.Default) {
      val context = chartView.context ?: return@withContext
      val parties = listOf(
        context.resources.getString(R.string.lib_detail_dialog_title_16kb_page_size),
        context.resources.getString(R.string.chart_item_not_support),
        context.resources.getString(R.string.title_statistics_dialog_no_native_libs)
      )
      val entries: ArrayList<PieEntryFloat> = ArrayList()
      val classifiedList = listOf(mutableListOf<LCItem>(), mutableListOf(), mutableListOf())
      classifiedMap.clear()
      val chartData = buildPageSize16KBChartData(items) { progress ->
        withContext(Dispatchers.Main) {
          onProgressUpdated(progress)
        }
      } ?: return@withContext

      classifiedList[SUPPORT_16KB].addAll(chartData.support16KB)
      classifiedList[NOT_SUPPORT_16KB].addAll(chartData.notSupport16KB)
      classifiedList[NO_NATIVE_LIBS].addAll(chartData.noNativeLibs)

      classifiedMap[SUPPORT_16KB] = ChartSourceItem(
        R.drawable.ic_16kb_align,
        false,
        classifiedList[SUPPORT_16KB]
      )
      classifiedMap[NOT_SUPPORT_16KB] = ChartSourceItem(
        R.drawable.ic_16kb_align,
        true,
        classifiedList[NOT_SUPPORT_16KB]
      )
      classifiedMap[NO_NATIVE_LIBS] = ChartSourceItem(
        R.drawable.ic_abi_label_no_libs,
        true,
        classifiedList[NO_NATIVE_LIBS]
      )

      // NOTE: The order of the entries when being added to the entries array determines their position around the center of
      // the chart.
      for (i in parties.indices) {
        entries.add(PieEntryFloat(classifiedList[i].size.toFloat(), parties[i % parties.size]))
      }
      val colors: ArrayList<Int> = ArrayList()

      if (OsUtils.atLeastS()) {
        if (com.absinthe.libraries.utils.utils.UiUtils.isDarkMode()) {
          colors.add(context.getColor(android.R.color.system_accent1_500))
          colors.add(context.getColor(android.R.color.system_accent1_700))
          colors.add(context.getColor(android.R.color.system_accent1_900))
        } else {
          colors.add(context.getColor(android.R.color.system_accent1_200))
          colors.add(context.getColor(android.R.color.system_accent1_400))
          colors.add(context.getColor(android.R.color.system_accent1_600))
        }
      } else {
        for (c in ColorTemplate.MATERIAL_COLORS) colors.add(c)
      }

      chartView.showPieData(entries, colors)
    }
  }

  override fun getLabelByXValue(context: Context, x: Int): String {
    return when (x) {
      SUPPORT_16KB -> context.getString(R.string.lib_detail_dialog_title_16kb_page_size)
      NOT_SUPPORT_16KB -> context.getString(R.string.chart_item_not_support)
      NO_NATIVE_LIBS -> context.getString(R.string.title_statistics_dialog_no_native_libs)
      else -> ""
    }
  }

  companion object {
    const val SUPPORT_16KB = 0
    const val NOT_SUPPORT_16KB = 1
    const val NO_NATIVE_LIBS = 2
  }
}
