package com.absinthe.libchecker.domain.statistics.chart.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.constant.AndroidVersions
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.repository.AppListRepository
import com.absinthe.libchecker.domain.statistics.chart.model.ClassifyDialogState
import com.absinthe.libchecker.domain.statistics.chart.model.LOADING_PROGRESS_MAX
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticControl
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDefinition
import com.absinthe.libchecker.domain.statistics.chart.repository.ChartSettingsRepository
import com.absinthe.libchecker.domain.statistics.chart.repository.StatisticCatalogRepository
import com.absinthe.libchecker.domain.statistics.chart.source.BaseVariableChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.source.ChartDataProvider
import com.absinthe.libchecker.domain.statistics.chart.source.ChartDataSourceFactory
import com.absinthe.libchecker.domain.statistics.chart.source.ChartDataSourcePlan
import com.absinthe.libchecker.domain.statistics.chart.source.IAndroidSDKChart
import com.absinthe.libchecker.domain.statistics.chart.source.IChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.usecase.BuildAndroidVersionLabelDisplayDataUseCase
import com.absinthe.libchecker.domain.statistics.chart.usecase.ChartFeatureInitializationPlan
import com.absinthe.libchecker.domain.statistics.chart.usecase.ObserveChartFeatureInitializationPlansUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChartViewModel internal constructor(
  appListRepository: AppListRepository,
  private val chartDataProvider: ChartDataProvider,
  private val chartDataSourceFactory: ChartDataSourceFactory,
  private val statisticCatalogRepository: StatisticCatalogRepository,
  private val chartSettingsRepository: ChartSettingsRepository,
  private val observeChartFeatureInitializationPlans: ObserveChartFeatureInitializationPlansUseCase,
  private val buildAndroidVersionLabelDisplayData: BuildAndroidVersionLabelDisplayDataUseCase
) : ViewModel() {
  private val chartUiStatePlanner = ChartUiStatePlanner()
  private var selectedStatisticId: String? = null
  private var featureInitializationPending = observeChartFeatureInitializationPlans.initialPending

  private val _statisticDefinitions = MutableStateFlow<List<StatisticDefinition>>(emptyList())
  val statisticDefinitions = _statisticDefinitions.asStateFlow()

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
  val currentStatistic: StatisticDefinition?
    get() = _statisticDefinitions.value.find { it.id == selectedStatisticId }

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

  init {
    viewModelScope.launch {
      _statisticDefinitions.value = statisticCatalogRepository.getStatistics()
      createStatisticSelectorPlan()
    }
  }

  fun setLoadingProgress(progress: Int, allowDecrease: Boolean = false) {
    val currentProgress = _loadingProgress.value
    if (allowDecrease || currentProgress >= LOADING_PROGRESS_MAX || progress >= currentProgress) {
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
  ): StatisticSelectorPlan {
    featureInitializationPending = plan.isPending
    return createStatisticSelectorPlan()
  }

  fun selectStatistic(statistic: StatisticDefinition): StatisticSelectorPlan {
    selectedStatisticId = statistic.id
    return createStatisticSelectorPlan()
  }

  fun createStatisticSelectorPlan(): StatisticSelectorPlan {
    val plan = chartUiStatePlanner.planStatistics(
      definitions = _statisticDefinitions.value,
      currentStatisticId = selectedStatisticId,
      featureChartsAvailable = !featureInitializationPending
    )
    selectedStatisticId = plan.selectedStatistic?.id
    setDetailAbiSwitchVisibility(
      plan.selectedStatistic?.hasControl(StatisticControl.DETAILED_ABI) == true
    )
    return plan
  }

  fun createProgressPlan(): ChartProgressPlan {
    return chartUiStatePlanner.planProgress(
      chartLoadingProgress = loadingProgress.value
    )
  }

  internal fun createChartDataSourcePlan(
    items: List<LCItem>,
    statistic: StatisticDefinition = checkNotNull(currentStatistic) {
      "Statistic catalog has not been loaded"
    }
  ): ChartDataSourcePlan {
    val selectedStatistic = checkNotNull(selectStatistic(statistic).selectedStatistic) {
      "Statistic ${statistic.id} is not available"
    }
    return chartDataSourceFactory.create(
      items = items,
      statistic = selectedStatistic,
      useDetailedAbiChart = isDetailedAbiChart
    )
  }

  suspend fun buildClassifyDialogState(
    source: IChartDataSource<*>,
    x: Int,
    title: String
  ): ClassifyDialogState {
    val items = source.getListByXValue(x)
    return ClassifyDialogState(
      title = title,
      items = items,
      itemViewStates = chartDataProvider.buildAppListItemViewStates(items),
      androidVersion = buildAndroidVersionLabelDisplayData(
        source.getAndroidVersionNodeByXValue(x)
      )
    )
  }

  private fun IChartDataSource<*>.getAndroidVersionNodeByXValue(x: Int): AndroidVersions.Node? {
    if (this !is IAndroidSDKChart) {
      return null
    }
    val version = (this as? BaseVariableChartDataSource<*>)?.getListKeyByXValue(x) ?: return null
    return AndroidVersions.versions.find { node -> node.version == version }
  }
}
