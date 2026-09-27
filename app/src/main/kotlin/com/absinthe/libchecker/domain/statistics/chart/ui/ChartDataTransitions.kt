package com.absinthe.libchecker.domain.statistics.chart.ui

import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.BaseDataSet
import com.github.mikephil.charting.data.ChartData
import com.github.mikephil.charting.data.PieEntry

internal fun <T : ChartData<*>> Chart<T>.updateFrom(preparedChart: Chart<T>) {
  val newData = preparedChart.data
  // MPAndroidChart pairs entries by index, not by category. Never morph unrelated buckets.
  val canAnimateChanges = !isEmpty && !preparedChart.isEmpty &&
    entryKeys() == preparedChart.entryKeys()
  if (canAnimateChanges) {
    newData?.dataSets?.forEachIndexed { index, dataSet ->
      if (dataSet is BaseDataSet<*>) {
        dataSet.colors = data!!.dataSets[index].colors
      }
    }
  }
  if (this is BarChart && preparedChart is BarChart) {
    xAxis.valueFormatter = preparedChart.xAxis.valueFormatter
    xAxis.labelCount = preparedChart.xAxis.labelCount
    xAxis.isForceLabelsEnabled = preparedChart.xAxis.isForceLabelsEnabled
    axisLeft.valueFormatter = preparedChart.axisLeft.valueFormatter
    axisRight.valueFormatter = preparedChart.axisRight.valueFormatter
  }
  highlightValues(emptyList())
  if (canAnimateChanges && newData != null) {
    if (animator.phaseX != 1f || animator.phaseY != 1f) {
      stopAnimations()
    }
    animateDataChange(newData, DATA_CHANGE_DURATION_MILLIS, Easing.EaseInOutQuad)
  } else {
    stopAnimations()
    data = newData
    animateEntrance()
  }
}

internal fun Chart<*>.animateEntrance() {
  if (!isEmpty) {
    animateY(if (this is PieChart) 800 else 650, Easing.EaseInOutQuad)
  }
}

private fun Chart<*>.entryKeys(): List<List<Pair<Float, String?>>> {
  return data?.dataSets.orEmpty().map { dataSet ->
    (0 until dataSet.entryCount).map { index ->
      val entry = dataSet.getEntryForIndex(index)
      entry.x to when (entry) {
        is PieEntry<*> -> entry.label
        else -> xAxis.valueFormatter.getFormattedValue(entry.x, xAxis)
      }
    }
  }
}

private const val DATA_CHANGE_DURATION_MILLIS = 450
