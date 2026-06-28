package com.absinthe.libchecker.domain.statistics.chart.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.constant.AndroidVersions
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.AppListRepository
import com.absinthe.libchecker.domain.app.list.model.AppListItemViewState
import com.absinthe.libchecker.domain.statistics.chart.model.ChartType
import com.absinthe.libchecker.domain.statistics.chart.model.LOADING_PROGRESS_MAX
import com.absinthe.libchecker.domain.statistics.chart.repository.ChartSettingsRepository
import com.absinthe.libchecker.domain.statistics.chart.source.BaseVariableChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.source.ChartDataProvider
import com.absinthe.libchecker.domain.statistics.chart.source.ChartDataSourceFactory
import com.absinthe.libchecker.domain.statistics.chart.source.ChartDataSourcePlan
import com.absinthe.libchecker.domain.statistics.chart.source.IAndroidSDKChart
import com.absinthe.libchecker.domain.statistics.chart.source.IChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.usecase.ChartFeatureInitializationPlan
import com.absinthe.libchecker.domain.statistics.chart.usecase.ObserveChartFeatureInitializationPlansUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ChartViewModel internal constructor(
  appListRepository: AppListRepository,
  private val chartDataProvider: ChartDataProvider,
  private val chartDataSourceFactory: ChartDataSourceFactory,
  private val chartSettingsRepository: ChartSettingsRepository,
  private val observeChartFeatureInitializationPlans: ObserveChartFeatureInitializationPlansUseCase
) : ViewModel() {
  private val chartUiStatePlanner = ChartUiStatePlanner()
  private var selectedChartType = ChartType.ABI
  private var featureInitializationPending = observeChartFeatureInitializationPlans.initialPending

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
  val currentChartType: ChartType
    get() = selectedChartType

  private val _loadingProgress = MutableStateFlow(LOADING_PROGRESS_MAX)
  val loadingProgress = _loadingProgress.asStateFlow()
  val isChartLoading: Boolean
    get() = loadingProgress.value < LOADING_PROGRESS_MAX
  val showSystemApps: Boolean
    get() = chartSettingsRepository.showSystemApps

  private val _distributionLastUpdateTime = MutableStateFlow("")
  val distributionLastUpdateTime = _distributionLastUpdateTime.asStateFlow()

  private val _detailAbiSwitch = MutableStateFlow(chartSettingsRepository.isDetailedAbiChart)
  val detailAbiSwitch = _detailAbiSwitch.asStateFlow()
  val isDetailedAbiChart: Boolean
    get() = _detailAbiSwitch.value

  private val _detailAbiSwitchVisibility = MutableStateFlow(true)
  val detailAbiSwitchVisibility = _detailAbiSwitchVisibility.asStateFlow()

  fun setLoadingProgress(progress: Int) {
    val currentProgress = _loadingProgress.value
    if (currentProgress >= LOADING_PROGRESS_MAX || progress >= currentProgress) {
      _loadingProgress.value = progress
    }
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

  fun updateFeatureInitializationPlan(
    plan: ChartFeatureInitializationPlan
  ): ChartTypeSelectorPlan {
    featureInitializationPending = plan.isPending
    return createChartTypeSelectorPlan()
  }

  fun selectChartType(chartType: ChartType): ChartTypeSelectorPlan {
    selectedChartType = chartType
    return createChartTypeSelectorPlan()
  }

  fun createChartTypeSelectorPlan(): ChartTypeSelectorPlan {
    val plan = chartUiStatePlanner.planChartTypes(
      currentChartType = selectedChartType,
      featureChartsAvailable = !featureInitializationPending
    )
    selectedChartType = plan.selectedType
    setDetailAbiSwitchVisibility(selectedChartType == ChartType.ABI)
    return plan
  }

  fun createProgressPlan(): ChartProgressPlan {
    return chartUiStatePlanner.planProgress(
      chartLoadingProgress = loadingProgress.value,
      featureInitializationPending = featureInitializationPending
    )
  }

  internal fun createChartDataSourcePlan(
    items: List<LCItem>,
    chartType: ChartType = currentChartType
  ): ChartDataSourcePlan {
    val selectedType = selectChartType(chartType).selectedType
    return chartDataSourceFactory.create(
      items = items,
      chartType = selectedType,
      useDetailedAbiChart = isDetailedAbiChart
    )
  }

  suspend fun buildClassifyDialogPlan(
    source: IChartDataSource<*>,
    x: Int,
    title: String
  ): ChartClassifyDialogPlan {
    val items = source.getListByXValue(x)
    return ChartClassifyDialogPlan(
      title = title,
      items = items,
      itemViewStates = chartDataProvider.buildAppListItemViewStates(items),
      androidVersionNode = source.getAndroidVersionNodeByXValue(x)
    )
  }

  private fun IChartDataSource<*>.getAndroidVersionNodeByXValue(x: Int): AndroidVersions.Node? {
    if (this !is IAndroidSDKChart) {
      return null
    }
    val version = (this as? BaseVariableChartDataSource<*>)?.getListKeyByXValue(x) ?: return null
    return AndroidVersions.versions.find { node -> node.version == version }
  }

  data class ChartClassifyDialogPlan(
    val title: String,
    val items: List<LCItem>,
    val itemViewStates: Map<String, AppListItemViewState>,
    val androidVersionNode: AndroidVersions.Node?
  )
}
