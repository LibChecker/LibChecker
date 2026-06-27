package com.absinthe.libchecker.domain.statistics.chart.ui

import android.view.View
import android.view.ViewGroup
import com.absinthe.libchecker.domain.statistics.chart.model.LOADING_PROGRESS_MAX
import com.absinthe.libchecker.domain.statistics.chart.source.IChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.source.impl.MarketDistributionChartDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class ChartDataRenderer(
  private val scope: CoroutineScope,
  private val onLoadingProgressChanged: (Int) -> Unit,
  private val onDistributionLastUpdateTimeChanged: (String) -> Unit
) {
  private var queryJob: Job? = null

  fun <T : View> render(
    root: ViewGroup,
    currentChartView: View?,
    newChartView: T,
    source: IChartDataSource<T>
  ) {
    queryJob?.cancel()
    queryJob = scope.launch(Dispatchers.Default) {
      source.fillChartView(newChartView) { progress ->
        onLoadingProgressChanged(progress)
      }

      withContext(Dispatchers.Main) {
        if (currentChartView != null) {
          root.removeView(currentChartView)
        }
        root.addView(newChartView)
        if (source.getData().isNotEmpty()) {
          onLoadingProgressChanged(LOADING_PROGRESS_MAX)
        }
        if (source is MarketDistributionChartDataSource) {
          onDistributionLastUpdateTimeChanged(source.lastUpdateTime)
        }
      }
    }
  }
}
