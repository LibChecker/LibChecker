package com.absinthe.libchecker.domain.statistics.chart.ui

import android.os.Looper
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.graphics.toColorInt
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.R
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.statistics.chart.source.impl.FeatureFlagChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.source.impl.PageSize16KBChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.usecase.BuildFeatureFlagChartDataUseCase
import com.absinthe.libchecker.domain.statistics.chart.usecase.BuildFeatureFlagChartDataUseCase.Kind
import com.absinthe.libchecker.domain.statistics.chart.usecase.PageSize16KBChartData
import info.appdev.charting.charts.PieChart
import info.appdev.charting.formatter.PercentFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PieChartDataSourceInstrumentedTest {
  @Test
  fun featureChartsKeepFilteredGroupsLabelsColorsAndSelectionTargets() = runBlocking {
    val cases = listOf(
      Triple(Kind.Kotlin, R.string.string_kotlin_used, "#7E52FF" to "#D9318E"),
      Triple(Kind.JetpackCompose, R.string.string_compose_used, "#37bf6e" to "#073042"),
      Triple(Kind.AppBundle, R.string.app_bundle, "#4285f4" to "#3ddc84")
    )
    for ((kind, label, colors) in cases) {
      val matched = item("matched", kind.featureFlag)
      val unmatched = item("unmatched", 0)
      val items = listOf(matched, unmatched, matched.copy(packageName = "system", isSystem = true))
      val source = FeatureFlagChartDataSource(items, kind) { targets, feature ->
        assertEquals(kind, feature)
        BuildFeatureFlagChartDataUseCase()(BuildFeatureFlagChartDataUseCase.Request(targets, feature, false))
      }
      val chart = newChart()
      source.fillChartView(chart) { error("Feature charts do not report incremental progress") }
      assertEquals(listOf(matched), source.getListByXValue(0))
      assertEquals(listOf(unmatched), source.getListByXValue(1))
      assertEquals(emptyList<LCItem>(), source.getListByXValue(2))
      assertEquals("", source.getLabelByXValue(chart.context, -1))
      assertFalse(source.getChartSourceItems().getValue(0).isGrayIcon)
      assertTrue(source.getChartSourceItems().getValue(1).isGrayIcon)
      withContext(Dispatchers.Main) {
        val data = chart.data!!.dataSet
        assertEquals(2, data.entryCount)
        assertEquals(chart.context.getString(label), data.getEntryForIndex(0)!!.label)
        assertEquals(1f, data.getEntryForIndex(0)!!.y, 0f)
        assertEquals(1f, data.getEntryForIndex(1)!!.y, 0f)
        assertEquals(listOf(colors.first.toColorInt(), colors.second.toColorInt()), data.colors)
        assertTrue(data.valueFormatter is PercentFormatter)
      }
    }
  }

  @Test
  fun heavyChartKeepsThreeGroupsAndMainThreadProgress() = runBlocking {
    val supported = item("supported", 0)
    val progress = mutableListOf<Int>()
    val source = PageSize16KBChartDataSource(listOf(supported)) { _, report ->
      report(50)
      PageSize16KBChartData(listOf(supported), emptyList(), emptyList())
    }
    val chart = newChart()
    source.fillChartView(chart) {
      assertEquals(Looper.getMainLooper(), Looper.myLooper())
      progress += it
    }
    assertEquals(listOf(50), progress)
    assertEquals(listOf(supported), source.getListByXValue(0))
    withContext(Dispatchers.Main) {
      assertEquals(3, chart.data!!.dataSet.entryCount)
      assertEquals(0f, chart.data!!.dataSet.getEntryForIndex(2)!!.y, 0f)
    }
  }

  private suspend fun newChart(): PieChart = withContext(Dispatchers.Main) {
    PieChart(ContextThemeWrapper(InstrumentationRegistry.getInstrumentation().targetContext, R.style.AppTheme))
  }

  private fun item(name: String, features: Int) = LCItem(name, name, "1", 1, 0, 0, false, 0, features, 35, 0)
}
