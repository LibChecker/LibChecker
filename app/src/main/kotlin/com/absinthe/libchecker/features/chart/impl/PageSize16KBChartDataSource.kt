package com.absinthe.libchecker.features.chart.impl

import android.content.Context
import com.absinthe.libchecker.R
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.features.chart.BaseChartDataSource
import com.absinthe.libchecker.features.chart.ChartSourceItem
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.is16KBAligned
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.github.mikephil.charting.utils.MPPointF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import timber.log.Timber

class PageSize16KBChartDataSource(items: List<LCItem>) : BaseChartDataSource<PieChart>(items) {
  override val classifiedMap: HashMap<Int, ChartSourceItem> = HashMap(2)

  override suspend fun fillChartView(chartView: PieChart) {
    withContext(Dispatchers.Default) {
      val context = chartView.context ?: return@withContext
      val parties = listOf(
        context.resources.getString(R.string.lib_detail_dialog_title_16kb_page_size),
        context.resources.getString(R.string.chart_item_not_support),
        context.resources.getString(R.string.title_statistics_dialog_no_native_libs)
      )
      val entries: ArrayList<PieEntry> = ArrayList()
      val colorOnSurface = context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface)
      val classifiedList = listOf(mutableListOf<LCItem>(), mutableListOf(), mutableListOf())

      for (item in filteredList) {
        if (!isActive) {
          return@withContext
        }
        try {
          val pi = PackageUtils.getPackageInfo(item.packageName)
          if (PackageUtils.hasNoNativeLibs(item.abi.toInt())) {
            classifiedList[NO_NATIVE_LIBS].add(item)
          } else if (pi.is16KBAligned()) {
            classifiedList[SUPPORT_16KB].add(item)
          } else {
            classifiedList[NOT_SUPPORT_16KB].add(item)
          }
        } catch (e: Exception) {
          Timber.e(e)
        }
      }

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
      val legendList = mutableListOf<String>()
      for (i in parties.indices) {
        entries.add(PieEntry(classifiedList[i].size.toFloat(), parties[i % parties.size]))
        legendList.add(parties[i % parties.size])
      }
      val dataSet = PieDataSet(entries, "").apply {
        setDrawIcons(false)
        sliceSpace = 3f
        iconsOffset = MPPointF(0f, 40f)
        selectionShift = 5f
        xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        valueLineColor = context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface)
      }

      // add a lot of colors
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

      dataSet.colors = colors
      // dataSet.setSelectionShift(0f);
      val data = PieData(dataSet).apply {
        setValueFormatter(PercentFormatter())
        setValueTextSize(10f)
        setValueTextColor(colorOnSurface)
      }

      withContext(Dispatchers.Main) {
        chartView.apply {
          this.data = data
          setEntryLabelColor(colorOnSurface)
          highlightValues(null)
          invalidate()
        }
      }
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
