package com.absinthe.libchecker.features.chart.ui

import android.graphics.Color
import android.view.HapticFeedbackConstants
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.compat.VersionCompat
import com.absinthe.libchecker.constant.AndroidVersions
import com.absinthe.libchecker.constant.GlobalFeatures
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.databinding.FragmentPieChartBinding
import com.absinthe.libchecker.features.chart.BaseChartDataSource
import com.absinthe.libchecker.features.chart.BaseVariableChartDataSource
import com.absinthe.libchecker.features.chart.ChartViewModel
import com.absinthe.libchecker.features.chart.IAndroidSDKChart
import com.absinthe.libchecker.features.chart.IChartDataSource
import com.absinthe.libchecker.features.chart.IntegerFormatter
import com.absinthe.libchecker.features.chart.impl.AABChartDataSource
import com.absinthe.libchecker.features.chart.impl.ABIChartDataSource
import com.absinthe.libchecker.features.chart.impl.CompileApiChartDataSource
import com.absinthe.libchecker.features.chart.impl.DetailedABIChartDataSource
import com.absinthe.libchecker.features.chart.impl.JetpackComposeChartDataSource
import com.absinthe.libchecker.features.chart.impl.KotlinChartDataSource
import com.absinthe.libchecker.features.chart.impl.MarketDistributionChartDataSource
import com.absinthe.libchecker.features.chart.impl.MinApiChartDataSource
import com.absinthe.libchecker.features.chart.impl.PageSize16KBChartDataSource
import com.absinthe.libchecker.features.chart.impl.TargetApiChartDataSource
import com.absinthe.libchecker.features.chart.ui.view.ChartDetailItemView
import com.absinthe.libchecker.features.chart.ui.view.ExpandingView
import com.absinthe.libchecker.features.chart.ui.view.MarketDistributionDashboardView
import com.absinthe.libchecker.services.WorkerService
import com.absinthe.libchecker.ui.base.BaseFragment
import com.absinthe.libchecker.ui.base.SaturationTransformation
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.Telemetry
import com.absinthe.libchecker.utils.extensions.doOnMainThreadIdle
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.core.util.ClipboardUtils
import timber.log.Timber

class ChartFragment :
  BaseFragment<FragmentPieChartBinding>(),
  OnChartValueSelectedListener {

  private val viewModel: ChartViewModel by activityViewModels()
  private lateinit var chartView: ViewGroup
  private lateinit var allLCItemsStateFlow: StateFlow<List<LCItem>>
  private var dataSource: IChartDataSource<*>? = null
  private var dialog: ClassifyBottomSheetDialogFragment? = null
  private var setDataJob: Job? = null

  private enum class ChartType {
    ABI,
    KOTLIN,
    TARGET_SDK,
    MIN_SDK,
    COMPILE_SDK,
    JETPACK_COMPOSE,
    MARKET_DISTRIBUTION,
    AAB,
    SUPPORT_16KB
  }

  private val chartTypeToIconRes = mutableMapOf(
    ChartType.ABI to (R.drawable.ic_logo to R.string.abi_string),
    ChartType.KOTLIN to (com.absinthe.lc.rulesbundle.R.drawable.ic_lib_kotlin to R.string.kotlin_string),
    ChartType.TARGET_SDK to (R.drawable.ic_label_target_sdk to R.string.target_sdk_string),
    ChartType.MIN_SDK to (R.drawable.ic_label_min_sdk to R.string.min_sdk_string),
    ChartType.COMPILE_SDK to (R.drawable.ic_label_compile_sdk to R.string.compile_sdk_string),
    ChartType.JETPACK_COMPOSE to (com.absinthe.lc.rulesbundle.R.drawable.ic_lib_jetpack_compose to R.string.jetpack_compose_short),
    ChartType.MARKET_DISTRIBUTION to (com.absinthe.lc.rulesbundle.R.drawable.ic_lib_android to R.string.android_dist_label),
    ChartType.AAB to (R.drawable.ic_aab to R.string.app_bundle)
  )
  private var currentChartType = ChartType.ABI
  private var currentExpandingView: ExpandingView? = null

  init {
    if (GlobalFeatures.ENABLE_DETECTING_16KB_PAGE_ALIGNMENT) {
      chartTypeToIconRes[ChartType.SUPPORT_16KB] =
        (R.drawable.ic_16kb_align to R.string.lib_detail_dialog_title_16kb_page_size)
    }
  }

  override fun init() {
    val featureInitialized = !WorkerService.initializingFeatures

    chartView = generatePieChartView()
    binding.root.addView(chartView, -1)

    chartTypeToIconRes.forEach {
      if (it.key == ChartType.KOTLIN || it.key == ChartType.JETPACK_COMPOSE) {
        if (!featureInitialized) {
          return
        }
      }
      val view = ExpandingView(requireContext()).apply {
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { lp ->
          lp.setMargins(4.dp, 4.dp, 4.dp, 4.dp)
        }
        setContent(it.value.first, getString(it.value.second))
        setOnClickListener { _ ->
          if (currentExpandingView == this) {
            return@setOnClickListener
          }
          setData(allLCItemsStateFlow.value, it.key)
          doOnMainThreadIdle {
            currentExpandingView?.toggle()
            toggle()
            currentExpandingView = this
          }
        }
      }
      if (currentExpandingView == null) {
        currentExpandingView = view
        view.toggle()
      }
      binding.featuresContainer.addView(view)
    }

    lifecycleScope.launch {
      allLCItemsStateFlow = Repositories.lcRepository.allLCItemsFlow.onEach {
        if (featureInitialized) {
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
        }
      }.stateIn(this)
    }
    lifecycleScope.launch {
      lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
        viewModel.isLoading.collect { isLoading ->
          if (isLoading) {
            binding.progressHorizontal.show()
          } else {
            binding.progressHorizontal.hide()
            applyDashboardView()
          }
        }
      }
    }
    lifecycleScope.launch {
      lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
        viewModel.distributionLastUpdateTime.collect { time ->
          (binding.dashboardContainer.getChildAt(0) as? MarketDistributionDashboardView)?.let {
            it.subtitle.text = getString(R.string.android_dist_subtitle_format, time)
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

  private fun setData(items: List<LCItem>, chartType: ChartType = currentChartType) {
    context ?: return
    currentChartType = chartType
    viewModel.setLoading(true)
    viewModel.setDetailAbiSwitchVisibility(chartType == ChartType.ABI)
    if (chartView.parent != null) {
      binding.root.removeView(chartView)
    }

    when (chartType) {
      ChartType.ABI -> {
        if (GlobalValues.isDetailedAbiChart) {
          setChartData(::generateBarChartView) { DetailedABIChartDataSource(items) }
        } else {
          setChartData(::generatePieChartView) { ABIChartDataSource(items) }
        }
      }
      ChartType.KOTLIN -> setChartData(::generatePieChartView) { KotlinChartDataSource(items) }
      ChartType.TARGET_SDK -> setChartData(::generateBarChartView) { TargetApiChartDataSource(items) }
      ChartType.MIN_SDK -> setChartData(::generateBarChartView) { MinApiChartDataSource(items) }
      ChartType.COMPILE_SDK -> setChartData(::generateBarChartView) { CompileApiChartDataSource(items) }
      ChartType.JETPACK_COMPOSE -> setChartData(::generatePieChartView) { JetpackComposeChartDataSource(items) }
      ChartType.MARKET_DISTRIBUTION -> setChartData(::generateBarChartView) { MarketDistributionChartDataSource(items) }
      ChartType.AAB -> setChartData(::generatePieChartView) { AABChartDataSource(items) }
      ChartType.SUPPORT_16KB -> setChartData(::generatePieChartView) { PageSize16KBChartDataSource(items) }
    }
    Telemetry.recordEvent("Chart", mapOf("ChartType" to chartType))
  }

  private fun <T : Chart<*>> setChartData(
    generateChartView: () -> T,
    dataSourceProvider: () -> IChartDataSource<T>
  ) {
    val newChartView = generateChartView()
    val ds = dataSourceProvider()
    viewModel.applyChartData(binding.root, chartView, newChartView, ds)
    chartView = newChartView
    dataSource = ds
  }

  override fun onNothingSelected() {
    Timber.d("Nothing selected")
  }

  override fun onValueSelected(e: Entry?, h: Highlight?) {
    if (e == null) return
    if (h == null) return
    if (dialog != null) return
    if (dataSource is MarketDistributionChartDataSource) {
      (chartView as? Chart<*>)?.highlightValue(null)
      return
    }

    applyItemSelect(h.x.toInt())
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
      animateY(800, Easing.EaseInOutQuad)
      setUsePercentValues(true)
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
      setDrawBorders(false)
      setDrawGridBackground(false)
      setFitBars(true)
      xAxis.apply {
        position = XAxis.XAxisPosition.BOTTOM
        setDrawGridLines(false)
        setDrawLabels(true)
        granularity = 1f
        textSize = 10f
        textColor = colorOnSurface
      }
      axisLeft.apply {
        valueFormatter = IntegerFormatter()
        setDrawGridLines(false)
        setDrawZeroLine(false)
        textColor = colorOnSurface
      }
      axisRight.apply {
        valueFormatter = IntegerFormatter()
        setDrawGridLines(false)
        setDrawZeroLine(false)
        textColor = colorOnSurface
      }
      animateY(650, Easing.EaseInOutQuad)
      setMaxVisibleValueCount(50)
      setDrawGridBackground(false)
      setDrawBorders(false)
      setDrawMarkers(false)
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
          subtitle.text = getString(
            R.string.android_dist_subtitle_format,
            viewModel.distributionLastUpdateTime.value
          )
        }
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

    dialog = ClassifyBottomSheetDialogFragment().also {
      it.setTitle(dataSource?.getLabelByXValue(requireContext(), x).orEmpty())
      it.setList(dataSource?.getListByXValue(x) ?: emptyList())

      if (dataSource is IAndroidSDKChart) {
        val index = (dataSource as BaseVariableChartDataSource<*>).getListKeyByXValue(x)
        it.setAndroidVersionLabel(AndroidVersions.versions.find { node -> node.version == index })
      } else {
        it.setAndroidVersionLabel(null)
      }

      activity?.let { activity ->
        it.setOnDismiss {
          this@ChartFragment.dialog = null
          (chartView as? Chart<*>)?.highlightValue(null)
        }
        it.show(
          activity.supportFragmentManager,
          ClassifyBottomSheetDialogFragment::class.java.name
        )
      }
    }
  }
}
