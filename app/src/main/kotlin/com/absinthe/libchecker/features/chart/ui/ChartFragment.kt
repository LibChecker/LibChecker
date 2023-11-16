package com.absinthe.libchecker.features.chart.ui

import android.graphics.Color
import android.view.HapticFeedbackConstants
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.absinthe.libchecker.R
import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.compat.VersionCompat
import com.absinthe.libchecker.constant.AndroidVersions
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.databinding.FragmentPieChartBinding
import com.absinthe.libchecker.features.chart.BaseVariableChartDataSource
import com.absinthe.libchecker.features.chart.ChartViewModel
import com.absinthe.libchecker.features.chart.IChartDataSource
import com.absinthe.libchecker.features.chart.IntegerFormatter
import com.absinthe.libchecker.features.chart.impl.ABIChartDataSource
import com.absinthe.libchecker.features.chart.impl.JetpackComposeChartDataSource
import com.absinthe.libchecker.features.chart.impl.KotlinChartDataSource
import com.absinthe.libchecker.features.chart.impl.MarketDistributionChartDataSource
import com.absinthe.libchecker.features.chart.impl.MinApiChartDataSource
import com.absinthe.libchecker.features.chart.impl.TargetApiChartDataSource
import com.absinthe.libchecker.features.chart.ui.view.MarketDistributionDashboardView
import com.absinthe.libchecker.services.WorkerService
import com.absinthe.libchecker.ui.base.BaseFragment
import com.absinthe.libchecker.utils.OsUtils
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
import com.google.android.material.button.MaterialButtonToggleGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.core.util.ClipboardUtils
import timber.log.Timber

class ChartFragment :
  BaseFragment<FragmentPieChartBinding>(),
  OnChartValueSelectedListener,
  MaterialButtonToggleGroup.OnButtonCheckedListener {

  private val viewModel: ChartViewModel by activityViewModels()
  private lateinit var chartView: ViewGroup
  private var dataSource: IChartDataSource<*>? = null
  private var dialog: ClassifyBottomSheetDialogFragment? = null
  private var setDataJob: Job? = null
  private var distributionDashboardView: MarketDistributionDashboardView? = null

  override fun init() {
    val featureInitialized = !WorkerService.initializingFeatures

    chartView = generatePieChartView()
    binding.root.addView(chartView, -1)

    binding.buttonsGroup.apply {
      addOnButtonCheckedListener(this@ChartFragment)
      check(R.id.btn_abi)
    }
    binding.btnKotlin.isVisible = featureInitialized
    binding.btnCompose.isVisible = featureInitialized

    viewModel.apply {
      dbItems.observe(viewLifecycleOwner) {
        if (featureInitialized) {
          setDataJob?.cancel()
          setDataJob = lifecycleScope.launch(Dispatchers.IO) {
            delay(2000)
            withContext(Dispatchers.Main) {
              setData()
            }
          }
        }
      }
    }
    lifecycleScope.launch {
      lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.isLoading.collect { isLoading ->
          if (isLoading) {
            binding.progressHorizontal.show()
          } else {
            binding.progressHorizontal.hide()
          }
        }
      }
    }
    lifecycleScope.launch {
      lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.distributionLastUpdateTime.collect { time ->
          if (distributionDashboardView?.parent != null) {
            distributionDashboardView?.subtitle?.text = getString(R.string.android_dist_subtitle_format, time)
          }
        }
      }
    }

    GlobalValues.isShowSystemApps.observe(viewLifecycleOwner) {
      setData()
    }
  }

  private fun setData() {
    context ?: return
    viewModel.setLoading(true)
    applyDistributionDashboardView()
    if (chartView.parent != null) {
      binding.root.removeView(chartView)
    }

    when (binding.buttonsGroup.checkedButtonId) {
      R.id.btn_abi -> setChartData(::generatePieChartView, ::ABIChartDataSource)
      R.id.btn_kotlin -> setChartData(::generatePieChartView, ::KotlinChartDataSource)
      R.id.btn_target_api -> setChartData(::generateBarChartView, ::TargetApiChartDataSource)
      R.id.btn_min_sdk -> setChartData(::generateBarChartView, ::MinApiChartDataSource)
      R.id.btn_compose -> setChartData(::generatePieChartView, ::JetpackComposeChartDataSource)
      R.id.btn_distribution -> setChartData(::generateBarChartView, ::MarketDistributionChartDataSource)
    }
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

    if (OsUtils.atLeastR()) {
      chartView.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    dialog = ClassifyBottomSheetDialogFragment()
    viewModel.filteredList.postValue(emptyList())

    lifecycleScope.launch(Dispatchers.IO) {
      viewModel.dialogTitle.postValue(dataSource?.getLabelByXValue(requireContext(), h.x.toInt()).orEmpty())
      viewModel.filteredList.postValue(dataSource?.getListByXValue(h.x.toInt()) ?: emptyList())

      if (dataSource is TargetApiChartDataSource || dataSource is MinApiChartDataSource) {
        val index = (dataSource as BaseVariableChartDataSource<*>).getListKeyByXValue(h.x.toInt())
        viewModel.androidVersion.postValue(AndroidVersions.versions.find { it.first == index })
      } else {
        viewModel.androidVersion.postValue(null)
      }

      withContext(Dispatchers.Main) {
        activity?.let { activity ->
          dialog?.also {
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
  }

  override fun onButtonChecked(
    group: MaterialButtonToggleGroup?,
    checkedId: Int,
    isChecked: Boolean
  ) {
    setData()
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

  private fun applyDistributionDashboardView() {
    if (binding.buttonsGroup.checkedButtonId == R.id.btn_distribution) {
      if (distributionDashboardView?.parent == null) {
        distributionDashboardView = MarketDistributionDashboardView(requireContext()).apply {
          layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
          )
          chip.setOnClickListener {
            val url = ApiManager.ANDROID_VERSION_DISTRIBUTION_HOST + ApiManager.ANDROID_VERSION_DISTRIBUTION_PATH
            ClipboardUtils.put(context, url)
            VersionCompat.showCopiedOnClipboardToast(context)
          }
          if (viewModel.distributionLastUpdateTime.value.isNotEmpty()) {
            subtitle.text = getString(R.string.android_dist_subtitle_format, viewModel.distributionLastUpdateTime.value)
          }
        }
        binding.root.addView(distributionDashboardView, 1)
      }
    } else {
      distributionDashboardView?.let {
        binding.root.removeView(it)
        distributionDashboardView = null
      }
    }
  }
}
