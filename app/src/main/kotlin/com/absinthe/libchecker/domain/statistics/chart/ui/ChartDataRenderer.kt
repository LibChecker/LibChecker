package com.absinthe.libchecker.domain.statistics.chart.ui

import android.view.View
import android.view.ViewGroup
import com.absinthe.libchecker.domain.statistics.chart.model.LOADING_PROGRESS_MAX
import com.github.mikephil.charting.charts.Chart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class ChartDataRenderer(
  private val scope: CoroutineScope,
  private val chartHost: ViewGroup,
  private val onLoadingProgressChanged: (Int) -> Unit
) {
  private val computationMutex = Mutex()
  private var queryJob: Job? = null
  private var renderGeneration = 0

  fun showInitialChart(chartView: View) {
    (chartView as? Chart<*>)?.isLoading = true
    chartHost.removeAllViews()
    chartHost.addView(chartView)
  }

  fun <T : View> render(
    newChartView: T,
    fillChart: suspend (T, (Int) -> Unit) -> Unit,
    commitChart: (T) -> T = { it },
    onCommitted: (T) -> Unit
  ) {
    val generation = ++renderGeneration
    queryJob?.cancel()
    (chartHost.getChildAt(0) as? Chart<*>)?.isLoading = true
    queryJob = scope.launch(Dispatchers.Default) {
      computationMutex.withLock {
        fillChart(newChartView) { progress ->
          if (generation == renderGeneration && progress < LOADING_PROGRESS_MAX) {
            onLoadingProgressChanged(progress)
          }
        }
      }

      withContext(Dispatchers.Main) {
        if (generation != renderGeneration) {
          return@withContext
        }
        val committedChart = commitChart(newChartView)
        if (chartHost.getChildAt(0) !== committedChart) {
          chartHost.removeAllViews()
          chartHost.addView(committedChart)
        }
        (committedChart as? Chart<*>)?.isLoading = false
        onCommitted(committedChart)
        onLoadingProgressChanged(LOADING_PROGRESS_MAX)
      }
    }
  }
}
