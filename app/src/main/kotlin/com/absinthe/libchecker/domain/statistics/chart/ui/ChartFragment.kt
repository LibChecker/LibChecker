package com.absinthe.libchecker.domain.statistics.chart.ui

import android.graphics.Color
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.compat.VersionCompat
import com.absinthe.libchecker.constant.AndroidVersions
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.databinding.FragmentPieChartBinding
import com.absinthe.libchecker.domain.statistics.chart.model.LOADING_PROGRESS_MAX
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDashboard
import com.absinthe.libchecker.domain.statistics.chart.presentation.ChartProgressPlan
import com.absinthe.libchecker.domain.statistics.chart.presentation.ChartRenderRequest
import com.absinthe.libchecker.domain.statistics.chart.presentation.ChartRenderRequestKey
import com.absinthe.libchecker.domain.statistics.chart.presentation.ChartViewModel
import com.absinthe.libchecker.domain.statistics.chart.source.BaseChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.source.ChartDataSourcePlan
import com.absinthe.libchecker.domain.statistics.chart.source.IChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.source.IntegerFormatter
import com.absinthe.libchecker.domain.statistics.chart.source.impl.MarketDistributionChartDataSource
import com.absinthe.libchecker.domain.statistics.chart.ui.view.ChartDetailItemView
import com.absinthe.libchecker.domain.statistics.chart.ui.view.MarketDistributionDashboardView
import com.absinthe.libchecker.ui.base.BaseFragment
import com.absinthe.libchecker.ui.base.SaturationTransformation
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.Telemetry
import com.absinthe.libchecker.utils.extensions.applySystemBarsPadding
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import rikka.core.util.ClipboardUtils
import timber.log.Timber

class ChartFragment :
  BaseFragment<FragmentPieChartBinding>(),
  OnChartValueSelectedListener {

  private val viewModel: ChartViewModel by activityViewModel()
  private lateinit var chartView: ViewGroup
  private var dataSource: IChartDataSource<*>? = null
  private var dialog: ClassifyBottomSheetDialogFragment? = null
  private var showClassifyDialogJob: Job? = null
  private lateinit var chartDataRenderer: ChartDataRenderer
  private lateinit var statisticSelectorController: StatisticSelectorController
  private lateinit var dashboardVisibilityController: DashboardVisibilityController
  private var currentChartRequestKey: ChartRenderRequestKey? = null
  private var currentProgressPlan: ChartProgressPlan? = null

  override fun init() {
    binding.root.applySystemBarsPadding(top = true, bottom = true)
    chartDataRenderer = ChartDataRenderer(
      scope = viewLifecycleOwner.lifecycleScope,
      chartHost = binding.chartHost,
      onLoadingProgressChanged = viewModel::setLoadingProgress
    )
    statisticSelectorController = StatisticSelectorController(
      context = requireContext(),
      container = binding.featuresContainer,
      chartContainer = binding.chartContainer,
      onStatisticSelected = viewModel::selectStatistic,
      onAddStatistic = {
        StatisticCatalogEditorBottomSheetDialogFragment().show(
          childFragmentManager,
          StatisticCatalogEditorBottomSheetDialogFragment::class.java.name
        )
      }
    )
    dashboardVisibilityController = DashboardVisibilityController(
      dashboard = binding.dashboardContainer,
      content = binding.animatedContentContainer
    )

    chartView = generatePieChartView()
    chartDataRenderer.showInitialChart(chartView)
    updateProgressIndicator()

    lifecycleScope.launch {
      lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
        viewModel.statisticSelectorPlan.collect(statisticSelectorController::render)
      }
    }
    lifecycleScope.launch {
      lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
        viewModel.chartRenderRequests.collect(::renderChart)
      }
    }
    lifecycleScope.launch {
      lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
        viewModel.loadingProgress.collect { updateProgressIndicator() }
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
  }

  override fun onDestroyView() {
    dashboardVisibilityController.cancel()
    super.onDestroyView()
  }

  private fun updateProgressIndicator() {
    val progressPlan = viewModel.createProgressPlan()
    val previousPlan = currentProgressPlan
    currentProgressPlan = progressPlan

    dashboardVisibilityController.setVisible(
      visible = !progressPlan.isVisible,
      animate = previousPlan != null
    )
    binding.loadingContainer.isVisible = progressPlan.isVisible
    binding.progressIndicator.let { indicator ->
      if (!progressPlan.isVisible) return@let

      if (previousPlan?.isIndeterminate != progressPlan.isIndeterminate) {
        indicator.isIndeterminate = progressPlan.isIndeterminate
      }
      if (progressPlan.isIndeterminate) {
        if (indicator.progress != 0) {
          indicator.progress = 0
        }
      } else if (
        previousPlan?.progress != progressPlan.progress ||
        previousPlan.isIndeterminateOrHidden()
      ) {
        indicator.setProgressCompat(
          progressPlan.progress,
          previousPlan?.isVisible == true
        )
      }
    }
  }

  private fun renderChart(request: ChartRenderRequest) {
    context ?: return
    val plan = viewModel.createChartDataSourcePlan(request)
    val canContinueLoadingProgress = currentChartRequestKey
      ?.canContinueLoadingProgress(request.key) == true
    val shouldResetLoadingProgress = !canContinueLoadingProgress || !viewModel.isChartLoading
    currentChartRequestKey = request.key

    when (plan) {
      is ChartDataSourcePlan.Pie -> setChartData(::generatePieChartView, plan, shouldResetLoadingProgress)
      is ChartDataSourcePlan.Bar -> setChartData(::generateBarChartView, plan, shouldResetLoadingProgress)
    }
    Telemetry.recordEvent(
      Constants.Event.CHART,
      mapOf(Telemetry.Param.ITEM_ID to request.statistic.id)
    )
  }

  private fun setChartData(
    generateChartView: () -> PieChart,
    plan: ChartDataSourcePlan.Pie,
    shouldResetLoadingProgress: Boolean
  ) {
    val newChartView = generateChartView()
    viewModel.setLoadingProgress(plan.initialLoadingProgress, allowDecrease = shouldResetLoadingProgress)
    val source = plan.dataSource
    chartDataRenderer.render(
      newChartView = newChartView,
      fillChart = source::fillChartView
    ) { committedChartView ->
      chartView = committedChartView
      dataSource = source
      completeChartRender(source)
    }
  }

  private fun setChartData(
    generateChartView: () -> BarChart,
    plan: ChartDataSourcePlan.Bar,
    shouldResetLoadingProgress: Boolean
  ) {
    val newChartView = generateChartView()
    viewModel.setLoadingProgress(plan.initialLoadingProgress, allowDecrease = shouldResetLoadingProgress)
    val source = plan.dataSource
    chartDataRenderer.render(
      newChartView = newChartView,
      fillChart = source::fillChartView
    ) { committedChartView ->
      chartView = committedChartView
      dataSource = source
      completeChartRender(source)
    }
  }

  private fun completeChartRender(source: IChartDataSource<*>) {
    if (source is MarketDistributionChartDataSource) {
      viewModel.setDistributionLastUpdateTime(source.lastUpdateTime)
    }
    applyDashboardView()
    if (source.getData().isNotEmpty()) {
      viewModel.setLoadingProgress(LOADING_PROGRESS_MAX)
    }
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
      layoutParams = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT
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
      layoutParams = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT
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
    if (viewModel.currentStatistic?.dashboard == StatisticDashboard.ANDROID_DISTRIBUTION) {
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
              if (value.statisticIcon != null) {
                icon.loadStatisticIcon(
                  icon = value.statisticIcon,
                  selected = false,
                  grayscale = value.isGrayIcon
                )
              } else {
                icon.load(value.iconRes) {
                  if (value.isGrayIcon) {
                    transformations(SaturationTransformation(0f))
                  }
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
      val state = viewModel.buildClassifyDialogState(source, x, title)
      val hostActivity = activity
      if (!isAdded || hostActivity == null || dialog != null) {
        return@launch
      }

      dialog = ClassifyBottomSheetDialogFragment().also {
        it.setState(state)

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

private fun ChartProgressPlan?.isIndeterminateOrHidden(): Boolean {
  return this == null || !isVisible || isIndeterminate
}
