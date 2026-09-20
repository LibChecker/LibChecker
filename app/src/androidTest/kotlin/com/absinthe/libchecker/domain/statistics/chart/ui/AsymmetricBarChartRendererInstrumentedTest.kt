package com.absinthe.libchecker.domain.statistics.chart.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.statistics.chart.ui.view.AsymmetricBarChartRenderer
import com.absinthe.libchecker.view.drawable.setG2Shape
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.highlight.Highlight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AsymmetricBarChartRendererInstrumentedTest {
  @Test
  fun rightCornersAreLargerAndHighlightStaysInsideTheSameOutline() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.runOnMainSync {
      val context = ContextThemeWrapper(instrumentation.targetContext, R.style.AppTheme)
      val chart = HorizontalBarChart(context)
      val renderer = AsymmetricBarChartRenderer(chart)
      chart.renderer = renderer
      chart.axisLeft.axisMinimum = 0f
      chart.axisLeft.axisMaximum = 120f
      chart.axisRight.axisMinimum = 0f
      chart.axisRight.axisMaximum = 120f
      chart.isFitBarsEnabled = true
      val entry = BarEntry(0f, 100f)
      chart.data = BarData(BarDataSet(listOf(entry), "").apply { color = Color.RED })
      chart.layout(0, 0, 800, 400)
      val bounds = RectF()
      chart.getBarBounds(entry, bounds)
      val density = context.resources.displayMetrics.density
      val bitmap = Bitmap.createBitmap(800, 400, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(bitmap)
      fun checkCorners() {
        val left = (bounds.left + 3f * density).toInt()
        val right = (bounds.right - 3f * density).toInt()
        for (y in listOf(bounds.top + density, bounds.bottom - density)) {
          assertTrue(Color.alpha(bitmap.getPixel(left, y.toInt())) > 0)
          assertEquals(0, Color.alpha(bitmap.getPixel(right, y.toInt())))
        }
        assertTrue(Color.alpha(bitmap.getPixel(bounds.centerX().toInt(), bounds.centerY().toInt())) > 0)
      }
      renderer.drawData(canvas)
      checkCorners()
      val expected = Bitmap.createBitmap(800, 400, Bitmap.Config.ARGB_8888)
      val g2 = Path().apply {
        setG2Shape(bounds.left, bounds.top, bounds.right, bounds.bottom, 2f * density, rightCornerRadius = 8f * density)
      }
      Canvas(expected).apply {
        clipPath(g2)
        drawRect(bounds, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.RED })
      }
      assertTrue("Bar must use the shared G2 outline", bitmap.sameAs(expected))
      expected.recycle()
      bitmap.eraseColor(Color.TRANSPARENT)
      renderer.drawHighlighted(canvas, listOf(Highlight(0f, 100f, 0)))
      checkCorners()
      bitmap.recycle()
    }
  }
}
