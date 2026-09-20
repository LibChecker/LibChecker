package com.absinthe.libchecker.domain.statistics.chart.source

import android.os.Looper
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.R
import com.absinthe.libchecker.api.bean.AndroidDistribution
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticIconSpec
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticTitleSpec
import com.absinthe.libchecker.domain.statistics.chart.source.impl.BinaryStatisticChartData
import com.absinthe.libchecker.domain.statistics.chart.source.impl.BinaryStatisticChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.source.impl.DetailedABIChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.source.impl.MarketDistributionChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.usecase.AndroidDistributionChartData
import com.absinthe.libchecker.domain.statistics.chart.usecase.DetailedAbiChartData
import com.absinthe.libchecker.domain.statistics.chart.usecase.DetailedAbiChartGroup
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedChartDataSourceInstrumentedTest {
  @Test
  fun marketDistributionRefreshesOnMainThreadAndFormatsPercentages() = runBlocking {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val context = ContextThemeWrapper(instrumentation.targetContext, R.style.AppTheme)
    lateinit var chart: BarChart
    instrumentation.runOnMainSync {
      chart = object : BarChart(context) {
        override fun notifyDataSetChanged() {
          assertEquals(Looper.getMainLooper(), Looper.myLooper())
          super.notifyDataSetChanged()
        }
      }
    }
    val source = MarketDistributionChartDataSource(emptyList()) {
      AndroidDistributionChartData(
        listOf(AndroidDistribution("Android 16", "16", 36, 0.25, "", emptyList())),
        "2026-09-20"
      )
    }
    source.fillChartView(chart) {}
    instrumentation.runOnMainSync {
      val dataSet = chart.data!!.getDataSetByIndex(0)!!
      val entry = dataSet.getEntryForIndex(0)
      assertEquals(0.25f, entry.y, 0f)
      val expected = java.text.DecimalFormat("###,###,##0.00%").format(0.25)
      assertEquals(expected, dataSet.valueFormatter.getFormattedValue(entry.y, entry, 0, chart.viewPortHandler))
      assertEquals(expected, chart.axisLeft.valueFormatter.getFormattedValue(entry.y, chart.axisLeft))
      assertEquals("0", chart.axisRight.valueFormatter.getFormattedValue(0f, chart.axisRight))
    }
  }

  @Test
  fun preservesOrderingSelectionProgressAndClearsEmptyData() = runBlocking {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val context = ContextThemeWrapper(instrumentation.targetContext, R.style.AppTheme)
    lateinit var bar: BarChart
    lateinit var pie: PieChart
    instrumentation.runOnMainSync {
      bar = BarChart(context)
      pie = PieChart(context)
    }
    val first = LCItem("first", "First", "", 0, 0, 0, false, 0, 0, 0, 0)
    val second = first.copy(packageName = "second")
    var groups = listOf(
      DetailedAbiChartGroup(9, "Nine", listOf(second)),
      DetailedAbiChartGroup(2, "Two", listOf(first, second))
    )
    val source = DetailedABIChartDataSource(listOf(first, second)) { _, progress ->
      progress(50)
      DetailedAbiChartData(groups)
    }
    source.fillChartView(bar) {
      assertEquals(Looper.getMainLooper(), Looper.myLooper())
      assertEquals(50, it)
    }
    assertEquals(listOf(first, second), source.getListByXValue(0))
    assertEquals(2, source.getListKeyByXValue(0))
    assertEquals("Two", source.getLabelByXValue(context, 0))
    instrumentation.runOnMainSync {
      assertEquals(2, bar.data!!.entryCount)
      assertEquals(2f, bar.data!!.getDataSetByIndex(0)!!.getEntryForIndex(0).y, 0f)
      assertEquals("Two", bar.xAxis.valueFormatter.getFormattedValue(0f, bar.xAxis))
    }
    groups = emptyList()
    source.fillChartView(bar) {}
    assertTrue(source.getListByXValue(0).isEmpty())
    instrumentation.runOnMainSync { assertEquals(0, bar.data!!.entryCount) }

    val icon = StatisticIconSpec()
    val chips = mapOf("first" to listOf("Facet A", "Facet B"))
    var binaryData = BinaryStatisticChartData(listOf(first), listOf(second), chips)
    val binary = BinaryStatisticChartDataSource(
      listOf(first, second),
      StatisticTitleSpec(translations = mapOf("en" to "Matched")),
      StatisticTitleSpec(translations = mapOf("en" to "Unmatched")),
      icon
    ) { _, _, progress ->
      progress(75)
      binaryData
    }
    binary.fillChartView(pie) {
      assertEquals(Looper.getMainLooper(), Looper.myLooper())
      assertEquals(75, it)
    }
    assertEquals(listOf(first), binary.getListByXValue(0))
    assertEquals(listOf(second), binary.getListByXValue(1))
    assertEquals(chips, binary.getItemChipsByXValue(0))
    assertTrue(binary.getItemChipsByXValue(1).isEmpty())
    assertFalse(binary.getChartSourceItems().getValue(0).isGrayIcon)
    assertTrue(binary.getChartSourceItems().getValue(1).isGrayIcon)
    assertEquals(icon, binary.getChartSourceItems().getValue(0).statisticIcon)
    binaryData = BinaryStatisticChartData(emptyList(), emptyList())
    binary.fillChartView(pie) {}
    assertTrue(binary.getListByXValue(0).isEmpty())
    assertTrue(binary.getItemChipsByXValue(0).isEmpty())
    instrumentation.runOnMainSync { assertEquals(2, pie.data!!.entryCount) }
  }
}
