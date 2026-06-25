package com.absinthe.libchecker.features.chart

import androidx.lifecycle.ViewModel
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.AppListItemViewState
import com.absinthe.libchecker.domain.app.AppListRepository
import com.absinthe.libchecker.domain.statistics.ChartSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

const val LOADING_PROGRESS_INFINITY = -1
const val LOADING_PROGRESS_MAX = 100

class ChartViewModel internal constructor(
  appListRepository: AppListRepository,
  private val chartDataProvider: ChartDataProvider,
  private val chartDataSourceFactory: ChartDataSourceFactory,
  private val chartSettingsRepository: ChartSettingsRepository
) : ViewModel() {
  val appListItems: Flow<List<LCItem>> = appListRepository.items

  private val _loadingProgress = MutableStateFlow(LOADING_PROGRESS_MAX)
  val loadingProgress = _loadingProgress.asStateFlow()

  private val _distributionLastUpdateTime = MutableStateFlow("")
  val distributionLastUpdateTime = _distributionLastUpdateTime.asStateFlow()

  private val _detailAbiSwitch = MutableStateFlow(chartSettingsRepository.isDetailedAbiChart)
  val detailAbiSwitch = _detailAbiSwitch.asStateFlow()
  val isDetailedAbiChart: Boolean
    get() = _detailAbiSwitch.value

  private val _detailAbiSwitchVisibility = MutableStateFlow(true)
  val detailAbiSwitchVisibility = _detailAbiSwitchVisibility.asStateFlow()

  fun setLoadingProgress(progress: Int) {
    _loadingProgress.value = progress
  }

  fun setDistributionLastUpdateTime(time: String) {
    _distributionLastUpdateTime.value = time
  }

  fun setDetailAbiSwitch(isDetailedAbiChart: Boolean) {
    chartSettingsRepository.isDetailedAbiChart = isDetailedAbiChart
    _detailAbiSwitch.value = isDetailedAbiChart
  }

  fun setDetailAbiSwitchVisibility(isVisible: Boolean) {
    _detailAbiSwitchVisibility.value = isVisible
  }

  internal fun createChartDataSourcePlan(
    items: List<LCItem>,
    chartType: ChartType
  ): ChartDataSourcePlan {
    return chartDataSourceFactory.create(
      items = items,
      chartType = chartType,
      useDetailedAbiChart = isDetailedAbiChart
    )
  }

  suspend fun buildAppListItemViewStates(items: List<LCItem>): Map<String, AppListItemViewState> {
    return chartDataProvider.buildAppListItemViewStates(items)
  }
}
