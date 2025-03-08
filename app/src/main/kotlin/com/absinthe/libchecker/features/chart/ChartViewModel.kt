package com.absinthe.libchecker.features.chart

import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.features.chart.impl.MarketDistributionChartDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val LOADING_PROGRESS_INFINITY = -1
const val LOADING_PROGRESS_MAX = 100

class ChartViewModel : ViewModel() {
  private var queryJob: Job? = null

  private val _loadingProgress = MutableStateFlow(LOADING_PROGRESS_MAX)
  val loadingProgress = _loadingProgress.asStateFlow()

  private val _distributionLastUpdateTime = MutableStateFlow("")
  val distributionLastUpdateTime = _distributionLastUpdateTime.asStateFlow()

  private val _detailAbiSwitch = MutableStateFlow(GlobalValues.isDetailedAbiChart)
  val detailAbiSwitch = _detailAbiSwitch.asStateFlow()

  private val _detailAbiSwitchVisibility = MutableStateFlow(true)
  val detailAbiSwitchVisibility = _detailAbiSwitchVisibility.asStateFlow()

  fun setLoadingProgress(progress: Int) {
    _loadingProgress.value = progress
  }

  fun setDetailAbiSwitch(isDetailedAbiChart: Boolean) {
    GlobalValues.isDetailedAbiChart = isDetailedAbiChart
    _detailAbiSwitch.value = isDetailedAbiChart
  }

  fun setDetailAbiSwitchVisibility(isVisible: Boolean) {
    _detailAbiSwitchVisibility.value = isVisible
  }

  fun <T : View> applyChartData(
    root: ViewGroup,
    currentChartView: View?,
    newChartView: T,
    source: IChartDataSource<T>
  ) {
    queryJob?.cancel()
    queryJob = viewModelScope.launch(Dispatchers.Default) {
      source.fillChartView(newChartView) {
        setLoadingProgress(it)
      }

      withContext(Dispatchers.Main) {
        if (currentChartView != null) {
          root.removeView(currentChartView)
        }
        root.addView(newChartView)
        if (source.getData().isNotEmpty()) {
          setLoadingProgress(LOADING_PROGRESS_MAX)
        }
        if (source is MarketDistributionChartDataSource) {
          _distributionLastUpdateTime.value = source.distribution
            ?.get(0)
            ?.descriptionBlocks
            ?.find { it.title.isEmpty() }
            ?.body
            ?.removePrefix("Last updated: ")
            .orEmpty()
        }
      }
    }
  }
}
