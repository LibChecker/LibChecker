package com.absinthe.libchecker.features.chart

import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.AppListItemViewState
import com.absinthe.libchecker.domain.app.AppListRepository
import com.absinthe.libchecker.domain.app.BuildAppListItemViewStatesUseCase
import com.absinthe.libchecker.domain.statistics.AbiChartData
import com.absinthe.libchecker.domain.statistics.AndroidDistributionChartData
import com.absinthe.libchecker.domain.statistics.BuildAbiChartDataUseCase
import com.absinthe.libchecker.domain.statistics.BuildApiLevelChartDataUseCase
import com.absinthe.libchecker.domain.statistics.BuildDetailedAbiChartDataUseCase
import com.absinthe.libchecker.domain.statistics.BuildFeatureFlagChartDataUseCase
import com.absinthe.libchecker.domain.statistics.BuildPageSize16KBChartDataUseCase
import com.absinthe.libchecker.domain.statistics.ChartSettingsRepository
import com.absinthe.libchecker.domain.statistics.DetailedAbiChartData
import com.absinthe.libchecker.domain.statistics.FeatureFlagChartData
import com.absinthe.libchecker.domain.statistics.GetAndroidDistributionUseCase
import com.absinthe.libchecker.domain.statistics.PageSize16KBChartData
import com.absinthe.libchecker.features.chart.impl.MarketDistributionChartDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val LOADING_PROGRESS_INFINITY = -1
const val LOADING_PROGRESS_MAX = 100

class ChartViewModel(
  appListRepository: AppListRepository,
  private val buildAppListItemViewStatesUseCase: BuildAppListItemViewStatesUseCase,
  private val buildAbiChartDataUseCase: BuildAbiChartDataUseCase,
  private val buildApiLevelChartDataUseCase: BuildApiLevelChartDataUseCase,
  private val buildDetailedAbiChartDataUseCase: BuildDetailedAbiChartDataUseCase,
  private val buildFeatureFlagChartDataUseCase: BuildFeatureFlagChartDataUseCase,
  private val buildPageSize16KBChartDataUseCase: BuildPageSize16KBChartDataUseCase,
  private val getAndroidDistributionUseCase: GetAndroidDistributionUseCase,
  private val chartSettingsRepository: ChartSettingsRepository
) : ViewModel() {
  private var queryJob: Job? = null

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

  fun setDetailAbiSwitch(isDetailedAbiChart: Boolean) {
    chartSettingsRepository.isDetailedAbiChart = isDetailedAbiChart
    _detailAbiSwitch.value = isDetailedAbiChart
  }

  fun setDetailAbiSwitchVisibility(isVisible: Boolean) {
    _detailAbiSwitchVisibility.value = isVisible
  }

  suspend fun buildAppListItemViewStates(items: List<LCItem>): Map<String, AppListItemViewState> {
    return buildAppListItemViewStatesUseCase(
      BuildAppListItemViewStatesUseCase.Request(
        items = items,
        options = chartSettingsRepository.appListDisplayOptions
      )
    )
  }

  suspend fun buildAbiChartData(items: List<LCItem>): AbiChartData? {
    return buildAbiChartDataUseCase(
      BuildAbiChartDataUseCase.Request(
        items = items,
        showSystemApps = chartSettingsRepository.showSystemApps
      )
    )
  }

  suspend fun buildApiLevelChartData(
    items: List<LCItem>,
    kind: BuildApiLevelChartDataUseCase.Kind
  ): Map<Int, List<LCItem>> {
    return buildApiLevelChartDataUseCase(
      BuildApiLevelChartDataUseCase.Request(
        items = items,
        kind = kind,
        showSystemApps = chartSettingsRepository.showSystemApps
      )
    )
  }

  suspend fun buildDetailedAbiChartData(
    items: List<LCItem>,
    onProgress: suspend (Int) -> Unit
  ): DetailedAbiChartData? {
    return buildDetailedAbiChartDataUseCase(
      BuildDetailedAbiChartDataUseCase.Request(
        items = items,
        showSystemApps = chartSettingsRepository.showSystemApps
      ),
      onProgress
    )
  }

  suspend fun buildFeatureFlagChartData(
    items: List<LCItem>,
    kind: BuildFeatureFlagChartDataUseCase.Kind
  ): FeatureFlagChartData? {
    return buildFeatureFlagChartDataUseCase(
      BuildFeatureFlagChartDataUseCase.Request(
        items = items,
        kind = kind,
        showSystemApps = chartSettingsRepository.showSystemApps
      )
    )
  }

  suspend fun buildPageSize16KBChartData(
    items: List<LCItem>,
    onProgress: suspend (Int) -> Unit
  ): PageSize16KBChartData? {
    return buildPageSize16KBChartDataUseCase(
      BuildPageSize16KBChartDataUseCase.Request(
        items = items,
        showSystemApps = chartSettingsRepository.showSystemApps
      ),
      onProgress
    )
  }

  suspend fun getAndroidDistribution(): AndroidDistributionChartData? {
    return getAndroidDistributionUseCase()
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
          _distributionLastUpdateTime.value = source.lastUpdateTime
        }
      }
    }
  }
}
