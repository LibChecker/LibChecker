package com.absinthe.libchecker.features.chart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.AppListItemViewState
import com.absinthe.libchecker.domain.app.AppListRepository
import com.absinthe.libchecker.domain.statistics.ChartDataProvider
import com.absinthe.libchecker.domain.statistics.ChartFeatureInitializationPlan
import com.absinthe.libchecker.domain.statistics.ChartSettingsRepository
import com.absinthe.libchecker.domain.statistics.ObserveChartFeatureInitializationPlansUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

const val LOADING_PROGRESS_INFINITY = -1
const val LOADING_PROGRESS_MAX = 100

class ChartViewModel internal constructor(
  appListRepository: AppListRepository,
  private val chartDataProvider: ChartDataProvider,
  private val chartDataSourceFactory: ChartDataSourceFactory,
  private val chartSettingsRepository: ChartSettingsRepository,
  private val observeChartFeatureInitializationPlans: ObserveChartFeatureInitializationPlansUseCase
) : ViewModel() {
  private val appListItemsState = appListRepository.items
    .map<List<LCItem>, List<LCItem>?> { it }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = null
    )
  val appListItems: Flow<List<LCItem>> = appListItemsState.filterNotNull()
  val featureInitializationPlans: Flow<ChartFeatureInitializationPlan> =
    observeChartFeatureInitializationPlans(appListItemsState)
  val initialFeatureInitializationPending: Boolean
    get() = observeChartFeatureInitializationPlans.initialPending

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
