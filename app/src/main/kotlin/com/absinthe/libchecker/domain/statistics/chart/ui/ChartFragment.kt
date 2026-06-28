package com.absinthe.libchecker.domain.statistics.chart.ui

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
import com.absinthe.libchecker.domain.statistics.chart.model.ChartType
import com.absinthe.libchecker.domain.statistics.chart.model.LOADING_PROGRESS_MAX
import com.absinthe.libchecker.domain.statistics.chart.presentation.ChartProgressPlan
import com.absinthe.libchecker.domain.statistics.chart.presentation.ChartTypeSelectorPlan
import com.absinthe.libchecker.domain.statistics.chart.presentation.ChartViewModel
import com.absinthe.libchecker.domain.statistics.chart.source.BaseChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.source.ChartDataSourcePlan
import com.absinthe.libchecker.domain.statistics.chart.source.IChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.source.IntegerFormatter
import com.absinthe.libchecker.domain.statistics.chart.source.impl.MarketDistributionChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.ui.view.ChartDetailItemView
import com.absinthe.libchecker.domain.statistics.chart.ui.view.ExpandingView
import com.absinthe.libchecker.domain.statistics.chart.ui.view.MarketDistributionDashboardView
import com.absinthe.libchecker.domain.statistics.chart.usecase.ChartFeatureInitializationPlan
import com.absinthe.libchecker.ui.base.BaseFragment
import com.absinthe.libchecker.ui.base.SaturationTransformation
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.Telemetry
import com.absinthe.libchecker.utils.extensions.applySystemBarsPadding
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import info.appdev.charting.animation.Easing
import info.appdev.charting.charts.BarChart
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
  private lateinit var chartDataRenderer: ChartDataRenderer
  private var currentExpandingView: ExpandingView? = null
  private var currentChartRequestKey: ChartRequestKey? = null
  private var currentProgressPlan: ChartProgressPlan? = null

  override fun init() {
    binding.root.applySystemBarsPadding(top = true, bottom = true)
    chartDataRenderer = ChartDataRenderer(
      scope = viewLifecycleOwner.lifecycleScope,
      onLoadingProgressChanged = viewModel::setLoadingProgress,
      onDistributionLastUpdateTimeChanged = viewModel::setDistributionLastUpdateTime
    )

    chartView = generatePieChartView()
    binding.root.addView(chartView, -1)
    renderChartTypeSelector()
    updateProgressIndicator()

    lifecycleScope.launch {
      allLCItemsStateFlow = viewModel.appListItems.onEach {
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
          if (progress >= LOADING_PROGRESS_MAX) {
            applyDashboardView()
          }
          updateProgressIndicator()
        }
      }
    }
    lifecycleScope.launch {
      lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
        viewModel.featureInitializationPlans.collect { plan ->
          renderChartTypeSelector(viewModel.updateFeatureInitializationPlan(plan))
          updateProgressIndicator()

          if (plan.shouldRefreshData && this@ChartFragment::allLCItemsStateFlow.isInitialized) {
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
          if (viewModel.currentChartType == ChartType.ABI && this@ChartFragment::allLCItemsStateFlow.isInitialized) {
            setData(allLCItemsStateFlow.value)
          }
        }
      }
    }
  }

  private fun renderChartTypeSelector(
    selectorPlan: ChartTypeSelectorPlan = viewModel.createChartTypeSelectorPlan()
  ) {
    binding.featuresContainer.removeAllViews()
    currentExpandingView = null

    selectorPlan.visibleTypes.forEach { chartType ->
      val view = ExpandingView(requireContext()).apply {
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { lp ->
          lp.setMargins(4.dp, 4.dp, 4.dp, 4.dp)
        }
        setContent(chartType.iconRes, getString(chartType.titleRes))
        setOnClickListener {
          if (viewModel.currentChartType == chartType || !this@ChartFragment::allLCItemsStateFlow.isInitialized) {
            return@setOnClickListener
          }
          setData(allLCItemsStateFlow.value, chartType)
          renderChartTypeSelector()
        }
      }

      if (selectorPlan.selectedType == chartType) {
        currentExpandingView = view
        view.toggle()
      }
      binding.featuresContainer.addView(view)
    }
  }

  private fun updateProgressIndicator() {
    val progressPlan = viewModel.createProgressPlan()
    val previousPlan = currentProgressPlan
    currentProgressPlan = progressPlan

    binding.progressHorizontal.let { indicator ->
      if (!progressPlan.isVisible) {
        if (previousPlan?.isVisible != false) {
          indicator.hide()
        }
        return@let
      }

      val wasVisible = previousPlan?.isVisible == true
      if (previousPlan?.isIndeterminate != progressPlan.isIndeterminate) {
        indicator.isIndeterminate = progressPlan.isIndeterminate
      }

      if (progressPlan.isIndeterminate) {
        if (previousPlan?.isIndeterminate != true && indicator.progress != 0) {
          indicator.progress = 0
        }
      } else if (progressPlan.progress == 0) {
        if (previousPlan?.progress != 0 || indicator.progress != 0) {
          indicator.progress = 0
        }
      } else if (
        previousPlan?.progress != progressPlan.progress ||
        previousPlan.isIndeterminateOrHidden()
      ) {
        indicator.setProgressCompat(progressPlan.progress, wasVisible)
      }

      if (!wasVisible) {
        indicator.show()
      }
    }
  }

  private fun setData(items: List<LCItem>, chartType: ChartType = viewModel.currentChartType) {
    context ?: return
    val plan = viewModel.createChartDataSourcePlan(items, chartType)
    val selectedChartType = viewModel.currentChartType
    val requestKey = ChartRequestKey(
      chartType = selectedChartType,
      useDetailedAbiChart = selectedChartType == ChartType.ABI && viewModel.isDetailedAbiChart,
      showSystemApps = viewModel.showSystemApps,
      itemsHash = items.chartRequestHash(selectedChartType)
    )
    if (requestKey == currentChartRequestKey) {
      return
    }
    currentChartRequestKey = requestKey

    if (chartView.parent != null) {
      binding.root.removeView(chartView)
    }

    when (plan) {
      is ChartDataSourcePlan.Pie -> setChartData(::generatePieChartView, plan)
      is ChartDataSourcePlan.Bar -> setChartData(::generateBarChartView, plan)
    }
    Telemetry.recordEvent(Constants.Event.CHART, mapOf(Telemetry.Param.ITEM_ID to selectedChartType))
  }

  private fun setChartData(
    generateChartView: () -> PieChart,
    plan: ChartDataSourcePlan.Pie
  ) {
    val newChartView = generateChartView()
    viewModel.setLoadingProgress(plan.initialLoadingProgress)
    chartDataRenderer.render(binding.root, chartView, newChartView, plan.dataSource)
    chartView = newChartView
    dataSource = plan.dataSource
  }

  private fun setChartData(
    generateChartView: () -> BarChart,
    plan: ChartDataSourcePlan.Bar
  ) {
    val newChartView = generateChartView()
    viewModel.setLoadingProgress(plan.initialLoadingProgress)
    chartDataRenderer.render(binding.root, chartView, newChartView, plan.dataSource)
    chartView = newChartView
    dataSource = plan.dataSource
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
    if (viewModel.currentChartType == ChartType.MARKET_DISTRIBUTION) {
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

    showClassifyDialogJob = lifecycleScope.launch {
      val plan = viewModel.buildClassifyDialogPlan(source, x, title)
      val hostActivity = activity
      if (!isAdded || hostActivity == null || dialog != null) {
        return@launch
      }

      dialog = ClassifyBottomSheetDialogFragment().also {
        it.setTitle(plan.title)
        it.setList(plan.items, plan.itemViewStates)
        it.setAndroidVersionLabel(plan.androidVersionNode)

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
}

private data class ChartRequestKey(
  val chartType: ChartType,
  val useDetailedAbiChart: Boolean,
  val showSystemApps: Boolean,
  val itemsHash: Int
)

private fun List<LCItem>.chartRequestHash(chartType: ChartType): Int {
  return fold(1) { result, item ->
    31 * result + item.chartRequestHash(chartType)
  }
}

private fun LCItem.chartRequestHash(chartType: ChartType): Int {
  if (chartType == ChartType.SUPPORT_16KB) {
    return chart16KBRequestHash()
  }
  return fullChartRequestHash(includeFeatures = chartType.requiresFeatureInitialization)
}

private fun LCItem.chart16KBRequestHash(): Int {
  var result = packageName.hashCode()
  result = 31 * result + isSystem.hashCode()
  result = 31 * result + abi.hashCode()
  result = 31 * result + versionCode.hashCode()
  result = 31 * result + lastUpdatedTime.hashCode()
  return result
}

private fun LCItem.fullChartRequestHash(includeFeatures: Boolean): Int {
  var result = packageName.hashCode()
  result = 31 * result + label.hashCode()
  result = 31 * result + versionName.hashCode()
  result = 31 * result + versionCode.hashCode()
  result = 31 * result + installedTime.hashCode()
  result = 31 * result + lastUpdatedTime.hashCode()
  result = 31 * result + isSystem.hashCode()
  result = 31 * result + abi.hashCode()
  result = 31 * result + if (includeFeatures) features else 0
  result = 31 * result + targetApi.hashCode()
  result = 31 * result + variant.hashCode()
  return result
}

private fun ChartProgressPlan?.isIndeterminateOrHidden(): Boolean {
  return this == null || !isVisible || isIndeterminate
}
