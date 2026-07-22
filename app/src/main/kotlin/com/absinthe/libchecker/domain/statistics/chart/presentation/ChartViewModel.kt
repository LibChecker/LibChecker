package com.absinthe.libchecker.domain.statistics.chart.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.constant.AndroidVersions
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.repository.AppListRepository
import com.absinthe.libchecker.domain.statistics.chart.model.ClassifyDialogState
import com.absinthe.libchecker.domain.statistics.chart.model.LOADING_PROGRESS_MAX
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticCatalogEditorState
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
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
  private val _statisticSelectorPlan = MutableStateFlow(StatisticSelectorPlan(emptyList(), null))
  val statisticSelectorPlan = _statisticSelectorPlan.asStateFlow()

  private val _statisticCatalogEditorState = MutableStateFlow(StatisticCatalogEditorState())
  val statisticCatalogEditorState = _statisticCatalogEditorState.asStateFlow()
  private var statisticCatalogRefreshJob: Job? = null

  private val appListItemsState = appListRepository.items
    .map<List<LCItem>, List<LCItem>?> { it }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = null
    )
  private val appListItems: Flow<List<LCItem>> = appListItemsState.filterNotNull()
  private val featureInitializationPlans: Flow<ChartFeatureInitializationPlan> =
    observeChartFeatureInitializationPlans(appListItemsState)
  val currentStatistic: StatisticDefinition?
    get() = statisticSelectorPlan.value.selectedStatistic

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

  private val chartRenderItems = appListItems.debounceSubsequent(CHART_UPDATE_DEBOUNCE_MILLIS)
  val chartRenderRequests: Flow<ChartRenderRequest> = combine(
    chartRenderItems,
    statisticSelectorPlan,
    detailAbiSwitch
  ) { items, selectorPlan, useDetailedAbiChart ->
    selectorPlan.selectedStatistic?.let { statistic ->
      createChartRenderRequest(
        items = items,
        statistic = statistic,
        useDetailedAbiChart = statistic.hasControl(StatisticControl.DETAILED_ABI) && useDetailedAbiChart,
        showSystemApps = showSystemApps
      )
    }
  }.filterNotNull().distinctUntilChangedBy(ChartRenderRequest::key)

  init {
    viewModelScope.launch {
      applySelectedStatistics(statisticCatalogRepository.getSelectedStatistics())
    }
    viewModelScope.launch {
      featureInitializationPlans.collect(::applyFeatureInitializationPlan)
    }
  }

  fun openStatisticCatalogEditor() {
    statisticCatalogRefreshJob?.cancel()
    statisticCatalogRefreshJob = viewModelScope.launch {
      val cachedAvailableStatistics = statisticCatalogRepository.getAvailableStatistics()
      _statisticCatalogEditorState.value = StatisticCatalogEditorState(
        selectedStatistics = _statisticDefinitions.value,
        availableStatistics = cachedAvailableStatistics,
        isRefreshing = true
      )

      val refreshedStatistics = statisticCatalogRepository.refreshAvailableStatistics()
      val availableStatistics = refreshedStatistics ?: cachedAvailableStatistics
      if (refreshedStatistics != null) {
        val selectedIds = _statisticDefinitions.value.map(StatisticDefinition::id)
        applySelectedStatistics(resolveStatistics(selectedIds, refreshedStatistics))
      }
      _statisticCatalogEditorState.value = StatisticCatalogEditorState(
        selectedStatistics = _statisticDefinitions.value,
        availableStatistics = availableStatistics,
        refreshFailed = refreshedStatistics == null
      )
    }
  }

  fun addStatistic(statistic: StatisticDefinition) {
    val selectedStatistics = _statisticDefinitions.value
    if (selectedStatistics.any { it.id == statistic.id }) return
    updateSelectedStatistics(selectedStatistics + statistic)
  }

  fun removeStatistic(statisticId: String) {
    val selectedStatistics = _statisticDefinitions.value
    if (selectedStatistics.size <= 1) return
    updateSelectedStatistics(selectedStatistics.filterNot { it.id == statisticId })
  }

  fun moveStatistic(fromIndex: Int, toIndex: Int) {
    val selectedStatistics = _statisticDefinitions.value
    if (
      fromIndex !in selectedStatistics.indices ||
      toIndex !in selectedStatistics.indices ||
      fromIndex == toIndex
    ) {
      return
    }
    val reorderedStatistics = selectedStatistics.toMutableList().apply {
      add(toIndex, removeAt(fromIndex))
    }
    updateSelectedStatistics(reorderedStatistics)
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

  private fun applyFeatureInitializationPlan(plan: ChartFeatureInitializationPlan) {
    featureInitializationPending = plan.isPending
    refreshStatisticSelectorPlan()
  }

  fun selectStatistic(statisticId: String) {
    selectedStatisticId = statisticId
    refreshStatisticSelectorPlan()
  }

  private fun refreshStatisticSelectorPlan() {
    val plan = chartUiStatePlanner.planStatistics(
      definitions = _statisticDefinitions.value,
      currentStatisticId = selectedStatisticId,
      featureChartsAvailable = !featureInitializationPending
    )
    selectedStatisticId = plan.selectedStatistic?.id
    setDetailAbiSwitchVisibility(
      plan.selectedStatistic?.hasControl(StatisticControl.DETAILED_ABI) == true
    )
    _statisticSelectorPlan.value = plan
  }

  fun createProgressPlan(): ChartProgressPlan {
    return chartUiStatePlanner.planProgress(
      chartLoadingProgress = loadingProgress.value
    )
  }

  internal fun createChartDataSourcePlan(
    request: ChartRenderRequest
  ): ChartDataSourcePlan {
    return chartDataSourceFactory.create(
      items = request.items,
      statistic = request.statistic,
      useDetailedAbiChart = request.key.useDetailedAbiChart
    )
  }

  private fun updateSelectedStatistics(statistics: List<StatisticDefinition>) {
    applySelectedStatistics(statistics)
    viewModelScope.launch {
      statisticCatalogRepository.setSelectedStatisticIds(
        statistics.map(StatisticDefinition::id)
      )
    }
  }

  private fun applySelectedStatistics(statistics: List<StatisticDefinition>) {
    _statisticDefinitions.value = statistics
    refreshStatisticSelectorPlan()
    _statisticCatalogEditorState.value = _statisticCatalogEditorState.value.copy(
      selectedStatistics = statistics
    )
  }

  private fun resolveStatistics(
    ids: List<String>,
    availableStatistics: List<StatisticDefinition>
  ): List<StatisticDefinition> {
    val definitionsById = availableStatistics.associateBy(StatisticDefinition::id)
    return ids.mapNotNull(definitionsById::get)
  }

  suspend fun buildClassifyDialogState(
    source: IChartDataSource<*>,
    x: Int,
    title: String,
    subtitle: String?
  ): ClassifyDialogState {
    val items = source.getListByXValue(x)
    return ClassifyDialogState(
      title = title,
      subtitle = subtitle?.takeUnless { it.trim() == title.trim() },
      items = items,
      itemViewStates = chartDataProvider.buildAppListItemViewStates(items),
      itemChips = source.getItemChipsByXValue(x),
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

  private companion object {
    const val CHART_UPDATE_DEBOUNCE_MILLIS = 2_000L
  }
}

internal fun <T> Flow<T>.debounceSubsequent(delayMillis: Long): Flow<T> = channelFlow {
  var hasEmitted = false
  collectLatest { value ->
    if (hasEmitted) {
      delay(delayMillis)
    }
    send(value)
    hasEmitted = true
  }
}
