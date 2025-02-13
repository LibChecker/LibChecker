package com.absinthe.libchecker.features.chart.impl

import android.content.Context
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants.MULTI_ARCH
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.features.chart.BaseChartDataSource
import com.absinthe.libchecker.features.chart.ChartSourceItem
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.ABI_32_BIT
import com.absinthe.libchecker.utils.extensions.ABI_64_BIT
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.github.mikephil.charting.utils.MPPointF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ABIChartDataSource(items: List<LCItem>) : BaseChartDataSource<PieChart>(items) {
  override val classifiedMap: HashMap<Int, ChartSourceItem> = HashMap(3)

  override suspend fun fillChartView(chartView: PieChart) {
    withContext(Dispatchers.Default) {
      val context = chartView.context ?: return@withContext
      val parties = listOf(
        context.resources.getString(R.string.string_64_bit),
        context.resources.getString(R.string.string_32_bit),
        context.resources.getString(R.string.no_libs)
      )
      val entries: ArrayList<PieEntry> = ArrayList()
      val colorOnSurface = context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface)
      val classifiedList = listOf(
        mutableListOf<LCItem>(),
        mutableListOf(),
        mutableListOf()
      )

      for (item in filteredList) {
        if (PackageUtils.hasNoNativeLibs(item.abi.toInt())) {
          classifiedList[NO_LIBS].add(item)
        } else {
          when (item.abi % MULTI_ARCH) {
            in ABI_64_BIT -> classifiedList[IS_64_BIT].add(item)
            in ABI_32_BIT -> classifiedList[IS_32_BIT].add(item)
            else -> classifiedList[NO_LIBS].add(item)
          }
        }
      }

      classifiedMap[IS_64_BIT] = ChartSourceItem(R.drawable.ic_abi_label_64bit, false, classifiedList[IS_64_BIT])
      classifiedMap[IS_32_BIT] = ChartSourceItem(R.drawable.ic_abi_label_32bit, false, classifiedList[IS_32_BIT])
      classifiedMap[NO_LIBS] = ChartSourceItem(R.drawable.ic_abi_label_no_libs, false, classifiedList[NO_LIBS])

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
        valueLineColor = colorOnSurface
      }

      // add a lot of colors
      val colors: ArrayList<Int> = ArrayList()

      if (OsUtils.atLeastS()) {
        if (com.absinthe.libraries.utils.utils.UiUtils.isDarkMode()) {
          colors.add(context.getColor(android.R.color.system_accent1_700))
          colors.add(context.getColor(android.R.color.system_accent1_800))
          colors.add(context.getColor(android.R.color.system_accent1_900))
        } else {
          colors.add(context.getColor(android.R.color.system_accent1_200))
          colors.add(context.getColor(android.R.color.system_accent1_300))
          colors.add(context.getColor(android.R.color.system_accent1_400))
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
      IS_64_BIT -> context.getString(
        R.string.title_statistics_dialog,
        context.getString(R.string.string_64_bit)
      )

      IS_32_BIT -> context.getString(
        R.string.title_statistics_dialog,
        context.getString(R.string.string_32_bit)
      )

      NO_LIBS -> context.getString(R.string.title_statistics_dialog_no_native_libs)

      else -> ""
    }
  }

  companion object {
    const val IS_64_BIT = 0
    const val IS_32_BIT = 1
    const val NO_LIBS = 2
  }
}
