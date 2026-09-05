package com.absinthe.libchecker.domain.statistics.chart.ui

import android.view.View
import android.view.ViewGroup
import com.absinthe.libchecker.domain.statistics.chart.model.LOADING_PROGRESS_MAX
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
    chartHost.removeAllViews()
    chartHost.addView(chartView)
  }

  fun <T : View> render(
    newChartView: T,
    fillChart: suspend (T, (Int) -> Unit) -> Unit,
    onCommitted: (T) -> Unit
  ) {
    val generation = ++renderGeneration
    queryJob?.cancel()
    queryJob = scope.launch(Dispatchers.Default) {
      var terminalProgressReported = false
      computationMutex.withLock {
        fillChart(newChartView) { progress ->
          if (generation == renderGeneration) {
            if (progress >= LOADING_PROGRESS_MAX) {
              terminalProgressReported = true
            } else {
              onLoadingProgressChanged(progress)
            }
          }
        }
      }

      withContext(Dispatchers.Main) {
        if (generation != renderGeneration) {
          return@withContext
        }
        chartHost.removeAllViews()
        chartHost.addView(newChartView)
        onCommitted(newChartView)
        if (terminalProgressReported) {
          onLoadingProgressChanged(LOADING_PROGRESS_MAX)
        }
      }
    }
  }
}
