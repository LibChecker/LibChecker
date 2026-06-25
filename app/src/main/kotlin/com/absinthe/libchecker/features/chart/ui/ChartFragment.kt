package com.absinthe.libchecker.features.chart.ui

import android.graphics.Color
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.compat.VersionCompat
import com.absinthe.libchecker.constant.AndroidVersions
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.databinding.FragmentPieChartBinding
import com.absinthe.libchecker.domain.statistics.BuildApiLevelChartDataUseCase
import com.absinthe.libchecker.domain.statistics.BuildFeatureFlagChartDataUseCase
import com.absinthe.libchecker.features.chart.BaseChartDataSource
import com.absinthe.libchecker.features.chart.BaseVariableChartDataSource
import com.absinthe.libchecker.features.chart.ChartType
import com.absinthe.libchecker.features.chart.ChartUiStatePlanner
import com.absinthe.libchecker.features.chart.ChartViewModel
import com.absinthe.libchecker.features.chart.IAndroidSDKChart
import com.absinthe.libchecker.features.chart.IChartDataSource
import com.absinthe.libchecker.features.chart.IHeavyWork
import com.absinthe.libchecker.features.chart.IntegerFormatter
import com.absinthe.libchecker.features.chart.LOADING_PROGRESS_INFINITY
import com.absinthe.libchecker.features.chart.LOADING_PROGRESS_MAX
import com.absinthe.libchecker.features.chart.impl.AABChartDataSource
import com.absinthe.libchecker.features.chart.impl.ABIChartDataSource
import com.absinthe.libchecker.features.chart.impl.ApiLevelChartDataSource
import com.absinthe.libchecker.features.chart.impl.DetailedABIChartDataSource
import com.absinthe.libchecker.features.chart.impl.JetpackComposeChartDataSource
import com.absinthe.libchecker.features.chart.impl.KotlinChartDataSource
import com.absinthe.libchecker.features.chart.impl.MarketDistributionChartDataSource
import com.absinthe.libchecker.features.chart.impl.PageSize16KBChartDataSource
import com.absinthe.libchecker.features.chart.ui.view.ChartDetailItemView
import com.absinthe.libchecker.features.chart.ui.view.ExpandingView
import com.absinthe.libchecker.features.chart.ui.view.MarketDistributionDashboardView
import com.absinthe.libchecker.services.WorkerService
import com.absinthe.libchecker.ui.base.BaseFragment
import com.absinthe.libchecker.ui.base.SaturationTransformation
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.Telemetry
import com.absinthe.libchecker.utils.extensions.applySystemBarsPadding
import com.absinthe.libchecker.utils.extensions.doOnMainThreadIdle
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import info.appdev.charting.animation.Easing
import info.appdev.charting.charts.Chart
import info.appdev.charting.charts.HorizontalBarChart
import info.appdev.charting.charts.PieChart
import info.appdev.charting.components.Legend
import info.appdev.charting.components.XAxis
import info.appdev.charting.data.EntryFloat
import info.appdev.charting.highlight.Highlight
import info.appdev.charting.listener.OnChartValueSelectedListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import rikka.core.util.ClipboardUtils
import timber.log.Timber

class ChartFragment :
  BaseFragment<FragmentPieChartBinding>(),
  OnChartValueSelectedListener {

  private val viewModel: ChartViewModel by activityViewModel()
  private lateinit var chartView: ViewGroup
  private lateinit var allLCItemsStateFlow: StateFlow<List<LCItem>>
  private var dataSource: IChartDataSource<*>? = null
  private var dialog: ClassifyBottomSheetDialogFragment? = null
  private var setDataJob: Job? = null
  private var showClassifyDialogJob: Job? = null
  private var chartLoadingProgress = LOADING_PROGRESS_MAX
  private var featureInitializationRunning = WorkerService.initializingFeatures
  private var featureInitializationCompleted = WorkerService.featureInitializationState.value.completed
  private var hasReceivedLCItems = false
  private var hasUninitializedFeatureItems = false
  private val isFeatureInitializationPending: Boolean
    get() = featureInitializationRunning ||
      (hasReceivedLCItems && !featureInitializationCompleted && hasUninitializedFeatureItems)
  private val chartUiStatePlanner = ChartUiStatePlanner()
  private var currentChartType = ChartType.ABI
  private var currentExpandingView: ExpandingView? = null

  override fun init() {
    binding.root.applySystemBarsPadding(top = true, bottom = true)
    featureInitializationRunning = WorkerService.featureInitializationState.value.running
    featureInitializationCompleted = WorkerService.featureInitializationState.value.completed

    chartView = generatePieChartView()
    binding.root.addView(chartView, -1)
    renderChartTypeSelector()
    updateProgressIndicator()

    lifecycleScope.launch {
      allLCItemsStateFlow = viewModel.appListItems.onEach {
        hasReceivedLCItems = true
        hasUninitializedFeatureItems = it.any { item -> item.features == FEATURES_NOT_INITIALIZED }
        featureInitializationCompleted =
          !hasUninitializedFeatureItems || WorkerService.featureInitializationState.value.completed
        renderChartTypeSelector()
        updateProgressIndicator()

        setDataJob?.cancel()
        setDataJob = lifecycleScope.launch(Dispatchers.IO) {
          if (dataSource != null) {
            delay(2000)
          }
          withContext(Dispatchers.Main) {
            if (dataSource == null) {
              setData(it, ChartType.ABI)
            } else {
              setData(it)
            }
          }
        }
      }.stateIn(this)
    }
    lifecycleScope.launch {
      lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
        viewModel.loadingProgress.collect { progress ->
          chartLoadingProgress = progress
          if (progress >= LOADING_PROGRESS_MAX) {
            applyDashboardView()
          }
          updateProgressIndicator()
        }
      }
    }
    lifecycleScope.launch {
      lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
        WorkerService.featureInitializationState.collect { state ->
          val wasPending = isFeatureInitializationPending
          featureInitializationRunning = state.running
          featureInitializationCompleted =
            state.completed || (hasReceivedLCItems && !hasUninitializedFeatureItems)
          renderChartTypeSelector()
          updateProgressIndicator()

          if (wasPending && !isFeatureInitializationPending && this@ChartFragment::allLCItemsStateFlow.isInitialized) {
            setData(allLCItemsStateFlow.value)
          }
        }
      }
    }
    lifecycleScope.launch {
      lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
        viewModel.distributionLastUpdateTime.collect { time ->
          (binding.dashboardContainer.getChildAt(0) as? MarketDistributionDashboardView)?.let {
            it.subtitle.text = getDistDashboardSubtitle(time)
          }
        }
      }
    }
    lifecycleScope.launch {
      lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
        viewModel.detailAbiSwitch.collect {
          if (currentChartType == ChartType.ABI && this@ChartFragment::allLCItemsStateFlow.isInitialized) {
            setData(allLCItemsStateFlow.value)
          }
        }
      }
    }
  }

  private fun renderChartTypeSelector() {
    val featureChartsAvailable = !isFeatureInitializationPending
    val selectorPlan = chartUiStatePlanner.planChartTypes(
      currentChartType = currentChartType,
      featureChartsAvailable = featureChartsAvailable
    )
    currentChartType = selectorPlan.selectedType

    binding.featuresContainer.removeAllViews()
    currentExpandingView = null

    selectorPlan.visibleTypes.forEach { chartType ->
      val view = ExpandingView(requireContext()).apply {
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { lp ->
          lp.setMargins(4.dp, 4.dp, 4.dp, 4.dp)
        }
        setContent(chartType.iconRes, getString(chartType.titleRes))
        setOnClickListener {
          if (currentChartType == chartType || !this@ChartFragment::allLCItemsStateFlow.isInitialized) {
            return@setOnClickListener
          }
          setData(allLCItemsStateFlow.value, chartType)
          doOnMainThreadIdle {
            currentExpandingView?.toggle()
            toggle()
            currentExpandingView = this
          }
        }
      }

      if (currentChartType == chartType) {
        currentExpandingView = view
        view.toggle()
      }
      binding.featuresContainer.addView(view)
    }
  }

  private fun updateProgressIndicator() {
    val progressPlan = chartUiStatePlanner.planProgress(
      chartLoadingProgress = chartLoadingProgress,
      featureInitializationPending = isFeatureInitializationPending
    )

    binding.progressHorizontal.let { indicator ->
      if (progressPlan.isVisible) {
        indicator.isIndeterminate = progressPlan.isIndeterminate
        if (progressPlan.isIndeterminate || progressPlan.progress == 0) {
          indicator.progress = 0
        } else {
          indicator.setProgressCompat(progressPlan.progress, true)
        }
        indicator.show()
      } else {
        indicator.hide()
      }
    }
  }

  private fun setData(items: List<LCItem>, chartType: ChartType = currentChartType) {
    context ?: return
    currentChartType = chartType
    viewModel.setDetailAbiSwitchVisibility(chartType == ChartType.ABI)
    if (chartView.parent != null) {
      binding.root.removeView(chartView)
    }

    when (chartType) {
      ChartType.ABI -> {
        if (viewModel.isDetailedAbiChart) {
          setChartData(::generateBarChartView) {
            DetailedABIChartDataSource(items, viewModel::buildDetailedAbiChartData)
          }
        } else {
          setChartData(::generatePieChartView) {
            ABIChartDataSource(items, viewModel::buildAbiChartData)
          }
        }
      }

      ChartType.KOTLIN -> setChartData(::generatePieChartView) {
        KotlinChartDataSource(items) { chartItems ->
          viewModel.buildFeatureFlagChartData(
            chartItems,
            BuildFeatureFlagChartDataUseCase.Kind.Kotlin
          )
        }
      }

      ChartType.TARGET_SDK -> setChartData(::generateBarChartView) {
        ApiLevelChartDataSource(
          items,
          BuildApiLevelChartDataUseCase.Kind.TargetSdk,
          viewModel::buildApiLevelChartData
        )
      }

      ChartType.MIN_SDK -> setChartData(::generateBarChartView) {
        ApiLevelChartDataSource(
          items,
          BuildApiLevelChartDataUseCase.Kind.MinSdk,
          viewModel::buildApiLevelChartData
        )
      }

      ChartType.COMPILE_SDK -> setChartData(::generateBarChartView) {
        ApiLevelChartDataSource(
          items,
          BuildApiLevelChartDataUseCase.Kind.CompileSdk,
          viewModel::buildApiLevelChartData
        )
      }

      ChartType.JETPACK_COMPOSE -> setChartData(::generatePieChartView) {
        JetpackComposeChartDataSource(items) { chartItems ->
          viewModel.buildFeatureFlagChartData(
            chartItems,
            BuildFeatureFlagChartDataUseCase.Kind.JetpackCompose
          )
        }
      }

      ChartType.MARKET_DISTRIBUTION -> setChartData(::generateBarChartView) {
        MarketDistributionChartDataSource(items, viewModel::getAndroidDistribution)
      }

      ChartType.AAB -> setChartData(::generatePieChartView) {
        AABChartDataSource(items) { chartItems ->
          viewModel.buildFeatureFlagChartData(
            chartItems,
            BuildFeatureFlagChartDataUseCase.Kind.AppBundle
          )
        }
      }

      ChartType.SUPPORT_16KB -> setChartData(::generatePieChartView) {
        PageSize16KBChartDataSource(items, viewModel::buildPageSize16KBChartData)
      }
    }
    Telemetry.recordEvent(Constants.Event.CHART, mapOf(Telemetry.Param.ITEM_ID to chartType))
  }

  private fun <T : Chart<*>> setChartData(
    generateChartView: () -> T,
    dataSourceProvider: () -> IChartDataSource<T>
  ) {
    val newChartView = generateChartView()
    val ds = dataSourceProvider()
    val loadingProgress = if (ds is IHeavyWork) 0 else LOADING_PROGRESS_INFINITY
    viewModel.setLoadingProgress(loadingProgress)
    viewModel.applyChartData(binding.root, chartView, newChartView, ds)
    chartView = newChartView
    dataSource = ds
  }

  override fun onNothingSelected() {
    Timber.d("Nothing selected")
  }

  override fun onValueSelected(entryFloat: EntryFloat, highlight: Highlight) {
    if (dialog != null || showClassifyDialogJob?.isActive == true) return
    if (dataSource is MarketDistributionChartDataSource) {
      (chartView as? Chart<*>)?.highlightValue(null)
      return
    }

    applyItemSelect(highlight.x.toInt())
  }

  private fun generatePieChartView(): PieChart {
    val colorOnSurface =
      requireContext().getColorByAttr(com.google.android.material.R.attr.colorOnSurface)
    return PieChart(requireContext()).apply {
      layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.MATCH_PARENT
      )
      dragDecelerationFrictionCoef = 0.95f
      description.isEnabled = false
      legend.apply {
        verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
        orientation = Legend.LegendOrientation.HORIZONTAL
        textColor = colorOnSurface
        xEntrySpace = 7f
        yEntrySpace = 0f
        isWordWrapEnabled = true
      }
      isUsePercentValues = true
      animateY(800, Easing.easeInOutQuad)
      setExtraOffsets(24f, 0f, 24f, 0f)
      setEntryLabelColor(colorOnSurface)
      setEntryLabelTextSize(11f)
      setNoDataText(getString(R.string.loading))
      setNoDataTextColor(colorOnSurface)
      setOnChartValueSelectedListener(this@ChartFragment)
      setHoleColor(Color.TRANSPARENT)
    }
  }

  private fun generateBarChartView(): HorizontalBarChart {
    val colorOnSurface =
      requireContext().getColorByAttr(com.google.android.material.R.attr.colorOnSurface)
    return HorizontalBarChart(requireContext()).apply {
      layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.MATCH_PARENT
      )
      description.isEnabled = false
      legend.isEnabled = false
      isDragEnabled = false
      setDrawBorders(false)
      setDrawGridBackground(false)
      setFitBars(true)
      xAxis.apply {
        position = XAxis.XAxisPosition.BOTTOM
        isDrawGridLines = false
        granularity = 1f
        textSize = 10f
        textColor = colorOnSurface
        setDrawLabels(true)
      }
      axisLeft.apply {
        valueFormatter = IntegerFormatter()
        isDrawGridLines = false
        isDrawZeroLine = false
        textColor = colorOnSurface
      }
      axisRight.apply {
        valueFormatter = IntegerFormatter()
        isDrawGridLines = false
        isDrawZeroLine = false
        textColor = colorOnSurface
      }
      animateY(650, Easing.easeInOutQuad)
      setMaxVisibleValueCount(50)
      setDrawGridBackground(false)
      setDrawBorders(false)
      isDrawMarkersEnabled = false
      setExtraOffsets(12f, 0f, 24f, 0f)
      setNoDataText(getString(R.string.loading))
      setNoDataTextColor(colorOnSurface)
      setOnChartValueSelectedListener(this@ChartFragment)
    }
  }

  private fun applyDashboardView() {
    binding.dashboardContainer.removeAllViews()
    if (currentChartType == ChartType.MARKET_DISTRIBUTION) {
      val view = MarketDistributionDashboardView(requireContext()).apply {
        layoutParams = ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
        )
        chip.setOnClickListener {
          ClipboardUtils.put(context, ApiManager.ANDROID_VERSION_DISTRIBUTION_URL)
          VersionCompat.showCopiedOnClipboardToast(context)
        }
        if (viewModel.distributionLastUpdateTime.value.isNotEmpty()) {
          subtitle.text = getDistDashboardSubtitle(viewModel.distributionLastUpdateTime.value)
        }
        updateContentDescription()
      }
      binding.dashboardContainer.addView(view)
    } else if (dataSource is BaseChartDataSource && chartView is PieChart) {
      (dataSource as BaseChartDataSource).let {
        it.getChartSourceItems().forEach { (key, value) ->
          val view = ChartDetailItemView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.WRAP_CONTENT
            )
            container.apply {
              icon.load(value.iconRes) {
                if (value.isGrayIcon) {
                  transformations(SaturationTransformation(0f))
                }
              }
              labelName.text = it.getLabelByXValue(context, key)
              // noinspection SetTextI18n
              count.text = value.data.size.toString()
              setOnClickListener {
                if (value.data.isNotEmpty()) {
                  applyItemSelect(key)
                }
              }
            }
          }
          binding.dashboardContainer.addView(view)
        }
      }
    }
  }

  private fun applyItemSelect(x: Int) {
    if (OsUtils.atLeastR()) {
      chartView.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    val source = dataSource ?: return
    val title = source.getLabelByXValue(requireContext(), x)
    val items = source.getListByXValue(x)
    val androidVersionNode = if (source is IAndroidSDKChart) {
      val index = (source as BaseVariableChartDataSource<*>).getListKeyByXValue(x)
      AndroidVersions.versions.find { node -> node.version == index }
    } else {
      null
    }

    showClassifyDialogJob = lifecycleScope.launch {
      val itemViewStates = viewModel.buildAppListItemViewStates(items)
      val hostActivity = activity
      if (!isAdded || hostActivity == null || dialog != null) {
        return@launch
      }

      dialog = ClassifyBottomSheetDialogFragment().also {
        it.setTitle(title)
        it.setList(items, itemViewStates)
        it.setAndroidVersionLabel(androidVersionNode)

        it.setOnDismiss {
          this@ChartFragment.dialog = null
          (chartView as? Chart<*>)?.highlightValue(null)
        }
        it.show(
          hostActivity.supportFragmentManager,
          ClassifyBottomSheetDialogFragment::class.java.name
        )
      }
    }
  }

  private fun getDistDashboardSubtitle(time: String): String {
    var androidVersion = AndroidVersions.simpleVersions[Build.VERSION.SDK_INT]
    if (OsUtils.atLeastBaklava()) {
      val minor = Build.getMinorSdkVersion(Build.VERSION.SDK_INT_FULL)
      if (minor > 0) {
        androidVersion = androidVersion.plus(", minor $minor")
      }
    }
    return getString(R.string.android_dist_subtitle_format, time) + System.lineSeparator() + "API ${Build.VERSION.SDK_INT} (Android $androidVersion)"
  }

  companion object {
    private const val FEATURES_NOT_INITIALIZED = -1
  }
}
