package com.absinthe.libchecker.domain.statistics.chart.ui

import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.domain.statistics.chart.source.ABILabelAxisFormatter
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChartDataTransitionsInstrumentedTest {
  @Test
  fun pieRefreshKeepsColorsRotationAndFinishesAtTheNewValues() = onMain {
    val chart = pieChart(listOf(3f, 7f), listOf(Color.RED, Color.BLUE))
    chart.rotationAngle = 45f
    val prepared = pieChart(listOf(8f, 2f), listOf(Color.GREEN, Color.YELLOW))

    chart.updateFrom(prepared)
    chart.stopAnimations()

    assertSame(prepared.data, chart.data)
    assertEquals(listOf(Color.RED, Color.BLUE), chart.data!!.dataSet!!.colors)
    assertEquals(45f, chart.rotationAngle, 0f)
    assertEquals(8f, chart.data!!.dataSet!!.getEntryForIndex(0).y, 0f)
    assertEquals(2f, chart.data!!.dataSet!!.getEntryForIndex(1).y, 0f)
    assertFalse(chart.isAnimating)
  }

  @Test
  fun changedBarCategoriesNeverAnimateFromAnUnrelatedBucket() = onMain {
    val chart = barChart(listOf("API 34", "API 35"), listOf(10f, 20f))
    val prepared = barChart(listOf("API 35", "API 36"), listOf(30f, 40f))

    chart.updateFrom(prepared)

    // A positional data-change animation would temporarily put 10 and 20 under the new labels.
    assertEquals(30f, chart.data!!.getDataSetByIndex(0)!!.getEntryForIndex(0).y, 0f)
    assertEquals(40f, chart.data!!.getDataSetByIndex(0)!!.getEntryForIndex(1).y, 0f)
    assertEquals("API 35", chart.xAxis.valueFormatter.getFormattedValue(0f, chart.xAxis))
    chart.stopAnimations()
  }

  @Test
  fun emptyRefreshStopsAnimationsAndCanRecoverWithData() = onMain {
    val chart = pieChart(listOf(3f, 7f), listOf(Color.RED, Color.BLUE))
    chart.animateEntrance()
    chart.updateFrom(pieChart(emptyList(), emptyList()))
    assertTrue(chart.isEmpty)
    assertFalse(chart.isAnimating)

    val prepared = pieChart(listOf(4f, 6f), listOf(Color.RED, Color.BLUE))
    chart.updateFrom(prepared)
    chart.stopAnimations()
    assertFalse(chart.isEmpty)
    assertSame(prepared.data, chart.data)
    assertEquals(4f, chart.data!!.dataSet!!.getEntryForIndex(0).y, 0f)
  }

  private fun pieChart(values: List<Float>, colors: List<Int>): PieChart {
    return PieChart(InstrumentationRegistry.getInstrumentation().targetContext).apply {
      data = PieData(
        PieDataSet(values.mapIndexed { index, value -> PieEntry(value, "Category $index") }, "").apply {
          this.colors = colors
        }
      )
    }
  }

  private fun barChart(labels: List<String>, values: List<Float>): HorizontalBarChart {
    return HorizontalBarChart(InstrumentationRegistry.getInstrumentation().targetContext).apply {
      xAxis.valueFormatter = ABILabelAxisFormatter(labels)
      data = BarData(BarDataSet(values.mapIndexed { index, value -> BarEntry(index.toFloat(), value) }, ""))
    }
  }

  private fun onMain(block: () -> Unit) {
    InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
  }
}
