package com.absinthe.libchecker.domain.statistics.chart.ui.view

import android.graphics.Canvas
import android.graphics.Path
import com.absinthe.libchecker.view.drawable.setG2Shape
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.renderer.HorizontalBarChartRenderer

internal class AsymmetricBarChartRenderer(private val barChart: HorizontalBarChart) : HorizontalBarChartRenderer(barChart, barChart.animator, barChart.viewPortHandler) {
  private val outline = Path()
  private val barOutline = Path()
  private val density = barChart.resources.displayMetrics.density

  override fun drawDataSet(c: Canvas, dataSet: IBarDataSet<*>, index: Int) {
    if (dataSet.isStacked) {
      super.drawDataSet(c, dataSet, index)
      return
    }
    outline.rewind()
    // ponytail: bounded statistics only; use reduced bar buffers if dense series are added.
    for (i in 0 until dataSet.entryCount) {
      appendBar(dataSet.getEntryForIndex(i), dataSet)
    }
    val save = c.save()
    c.clipPath(outline)
    super.drawDataSet(c, dataSet, index)
    c.restoreToCount(save)
  }

  override fun drawHighlighted(c: Canvas, indices: List<Highlight>) {
    for (highlight in indices) {
      val dataSet = barChart.barData?.getDataSetByIndex(highlight.dataSetIndex) ?: continue
      val entry = dataSet.getEntryForXValue(highlight.x, highlight.y) ?: continue
      if (dataSet.isStacked) {
        super.drawHighlighted(c, listOf(highlight))
        continue
      }
      outline.rewind()
      appendBar(entry, dataSet)
      val save = c.save()
      c.clipPath(outline)
      super.drawHighlighted(c, listOf(highlight))
      c.restoreToCount(save)
    }
  }

  private fun appendBar(entry: BarEntry<*>, dataSet: IBarDataSet<*>) {
    prepareBarHighlight(
      entry.x,
      entry.y,
      0f,
      (barChart.barData?.barWidth ?: return) / 2f,
      barChart.getTransformer(dataSet.axisDependency)
    )
    barRect.sort()
    barOutline.setG2Shape(
      barRect.left,
      barRect.top,
      barRect.right,
      barRect.bottom,
      2f * density,
      rightCornerRadius = 8f * density
    )
    outline.addPath(barOutline)
  }
}
