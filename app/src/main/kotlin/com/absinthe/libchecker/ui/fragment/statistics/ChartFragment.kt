package com.absinthe.libchecker.ui.fragment.statistics

import android.content.pm.PackageInfo
import android.graphics.Color
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.AndroidVersions
import com.absinthe.libchecker.constant.Constants.ARMV5
import com.absinthe.libchecker.constant.Constants.ARMV7
import com.absinthe.libchecker.constant.Constants.ARMV8
import com.absinthe.libchecker.constant.Constants.MULTI_ARCH
import com.absinthe.libchecker.constant.Constants.NO_LIBS
import com.absinthe.libchecker.constant.Constants.X86
import com.absinthe.libchecker.constant.Constants.X86_64
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.database.entity.Features
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.databinding.FragmentPieChartBinding
import com.absinthe.libchecker.services.WorkerService
import com.absinthe.libchecker.ui.base.BaseFragment
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.view.statistics.IntegerFormatter
import com.absinthe.libchecker.view.statistics.OsVersionAxisFormatter
import com.absinthe.libchecker.viewmodel.ChartViewModel
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.utils.ColorTemplate
import com.github.mikephil.charting.utils.MPPointF
import com.google.android.material.button.MaterialButtonToggleGroup
import java.util.TreeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

private const val TYPE_ABI = 0
private const val TYPE_KOTLIN = 1
private const val TYPE_TARGET_API = 2
private const val TYPE_MIN_SDK = 3

private val ABI_64_BIT = setOf(ARMV8, X86_64)
private val ABI_32_BIT = setOf(ARMV5, ARMV7, X86)

class ChartFragment :
  BaseFragment<FragmentPieChartBinding>(),
  OnChartValueSelectedListener,
  MaterialButtonToggleGroup.OnButtonCheckedListener {

  private val viewModel: ChartViewModel by activityViewModels()
  private val legendList = mutableListOf<String>()
  private val apiScope = Build.VERSION_CODES.S_V2

  private lateinit var chartView: ViewGroup
  private var chartType = TYPE_ABI
  private var dialog: ClassifyBottomSheetDialogFragment? = null
  private var queryJob: Job? = null
  private var setDataJob: Job? = null

  override fun init() {
    val isKotlinShowed = !WorkerService.initializingFeatures

    chartView = generatePieChartView()
    binding.root.addView(chartView, -1)

    binding.buttonsGroup.apply {
      addOnButtonCheckedListener(this@ChartFragment)
      check(R.id.btn_abi)
    }
    binding.btnKotlin.isVisible = isKotlinShowed

    viewModel.apply {
      dbItems.observe(viewLifecycleOwner) {
        if (isKotlinShowed) {
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

    GlobalValues.isShowSystemApps.observe(viewLifecycleOwner) {
      setData()
    }
  }

  private fun setData() {
    context ?: return

    when (chartType) {
      TYPE_ABI -> setAbiData()
      TYPE_KOTLIN -> setKotlinData()
      TYPE_TARGET_API -> setTargetApiData()
      TYPE_MIN_SDK -> setMinSdkData()
    }
  }

  private fun setAbiData() {
    binding.progressHorizontal.show()
    if (chartView.parent != null) {
      binding.root.removeView(chartView)
    }
    chartView = generatePieChartView()
    binding.root.addView(chartView, -1)
    queryJob?.cancel()
    queryJob = lifecycleScope.launch(Dispatchers.IO) {
      val context = context ?: return@launch
      val parties = listOf(
        resources.getString(R.string.string_64_bit),
        resources.getString(R.string.string_32_bit),
        resources.getString(R.string.no_libs)
      )
      val entries: ArrayList<PieEntry> = ArrayList()

      val filteredList = if (GlobalValues.isShowSystemApps.value == true) {
        viewModel.dbItems.value
      } else {
        viewModel.dbItems.value?.filter { !it.isSystem }
      }
      val colorOnSurface = context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface)

      filteredList?.let {
        val list = mutableListOf(0, 0, 0)

        for (item in it) {
          if (GlobalValues.isShowSystemApps.value == false) {
            if (item.isSystem) continue
          }
          when (item.abi % 10) {
            in ABI_64_BIT -> list[0]++
            in ABI_32_BIT -> list[1]++
            else -> list[2]++
          }
        }

        // NOTE: The order of the entries when being added to the entries array determines their position around the center of
        // the chart.
        legendList.clear()
        for (i in parties.indices) {
          entries.add(PieEntry(list[i].toFloat(), parties[i % parties.size]))
          legendList.add(parties[i % parties.size])
        }
        val dataSet = PieDataSet(entries, "").apply {
          setDrawIcons(false)
          sliceSpace = 3f
          iconsOffset = MPPointF(0f, 40f)
          selectionShift = 5f
          xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
          yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
          valueLineColor = colorOnSurface
        }

        // add a lot of colors
        val colors: ArrayList<Int> = ArrayList()

        if (OsUtils.atLeastS()) {
          if (com.absinthe.libraries.utils.utils.UiUtils.isDarkMode()) {
            colors.add(context.getColor(android.R.color.system_accent1_700))
            colors.add(context.getColor(android.R.color.system_accent1_800))
            colors.add(context.getColor(android.R.color.system_accent1_900))
          } else {
            colors.add(context.getColor(android.R.color.system_accent1_200))
            colors.add(context.getColor(android.R.color.system_accent1_300))
            colors.add(context.getColor(android.R.color.system_accent1_400))
          }
        } else {
          for (c in ColorTemplate.MATERIAL_COLORS) colors.add(c)
        }

        dataSet.colors = colors
        // dataSet.setSelectionShift(0f);
        val data = PieData(dataSet).apply {
          setValueFormatter(PercentFormatter(chartView as PieChart))
          setValueTextSize(10f)
          setValueTextColor(colorOnSurface)
        }

        withContext(Dispatchers.Main) {
          (chartView as PieChart).apply {
            this.data = data
            setEntryLabelColor(colorOnSurface)
            highlightValues(null)
            invalidate()
          }
          binding.progressHorizontal.hide()
        }
      }
    }
  }

  private fun setKotlinData() {
    binding.progressHorizontal.show()
    if (chartView.parent != null) {
      binding.root.removeView(chartView)
    }
    chartView = generatePieChartView()
    binding.root.addView(chartView, -1)
    queryJob?.cancel()
    queryJob = lifecycleScope.launch(Dispatchers.IO) {
      val context = context ?: return@launch
      val parties = listOf(
        resources.getString(R.string.string_kotlin_used),
        resources.getString(R.string.string_kotlin_unused)
      )
      val entries: ArrayList<PieEntry> = ArrayList()

      val filteredList = if (GlobalValues.isShowSystemApps.value == true) {
        viewModel.dbItems.value
      } else {
        viewModel.dbItems.value?.filter { !it.isSystem }
      }
      val colorOnSurface = context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface)

      filteredList?.let {
        val list = mutableListOf(0, 0)

        for (item in it) {
          if ((item.features and Features.KOTLIN_USED) > 0) {
            list[0]++
          } else {
            list[1]++
          }
        }

        // NOTE: The order of the entries when being added to the entries array determines their position around the center of
        // the chart.
        legendList.clear()
        for (i in parties.indices) {
          entries.add(PieEntry(list[i].toFloat(), parties[i % parties.size]))
          legendList.add(parties[i % parties.size])
        }
        val dataSet = PieDataSet(entries, "").apply {
          setDrawIcons(false)
          sliceSpace = 3f
          iconsOffset = MPPointF(0f, 40f)
          selectionShift = 5f
          xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
          yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
          valueLineColor = context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface)
        }

        // add a lot of colors
        val colors = arrayListOf(
          Color.parseColor("#7E52FF"),
          Color.parseColor("#D9318E")
        )

        dataSet.colors = colors
        // dataSet.setSelectionShift(0f);
        val data = PieData(dataSet).apply {
          setValueFormatter(PercentFormatter(chartView as PieChart))
          setValueTextSize(10f)
          setValueTextColor(colorOnSurface)
        }

        withContext(Dispatchers.Main) {
          (chartView as PieChart).apply {
            this.data = data
            setEntryLabelColor(colorOnSurface)
            highlightValues(null)
            invalidate()
          }
          binding.progressHorizontal.hide()
        }
      }
    }
  }

  private fun setTargetApiData() {
    binding.progressHorizontal.show()
    queryJob?.cancel()
    queryJob = lifecycleScope.launch(Dispatchers.IO) {
      val context = context ?: return@launch

      val entries: ArrayList<BarEntry> = ArrayList()

      val filteredList = if (GlobalValues.isShowSystemApps.value == true) {
        viewModel.dbItems.value
      } else {
        viewModel.dbItems.value?.filter { !it.isSystem }
      }

      filteredList?.let {
        val apiMap = mutableMapOf<Int, Int>()

        var targetApi: Int
        for (item in it) {
          try {
            targetApi = PackageUtils.getPackageInfo(item.packageName)
              .applicationInfo.targetSdkVersion
            apiMap[targetApi] = apiMap[targetApi]?.plus(1) ?: 1
          } catch (e: Exception) {
            Timber.e(e)
          }
        }

        val sortedApiMap = TreeMap(apiMap)
        withContext(Dispatchers.Main) {
          if (chartView.parent != null) {
            binding.root.removeView(chartView)
          }
          chartView = generateBarChartView(sortedApiMap)
          binding.root.addView(chartView, -1)
        }

        legendList.clear()
        var index = 0
        sortedApiMap.forEach { entry ->
          entries.add(BarEntry(index.toFloat(), entry.value.toFloat()))
          legendList.add(entry.key.toString())
          index++
        }
        val dataSet = BarDataSet(entries, "").apply {
          setDrawIcons(false)
          valueFormatter = IntegerFormatter()
        }

        // add a lot of colors
        val colors: ArrayList<Int> = ArrayList()
        (0..apiScope).forEach { _ ->
          colors.add(UiUtils.getRandomColor())
        }

        dataSet.colors = colors
        // dataSet.setSelectionShift(0f);
        val data = BarData(dataSet).apply {
          setValueTextSize(10f)
          setValueTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface))
        }

        withContext(Dispatchers.Main) {
          (chartView as HorizontalBarChart).apply {
            this.data = data
            highlightValues(null)
            invalidate()
          }
          binding.progressHorizontal.hide()
        }
      }
    }.also {
      it.start()
    }
  }

  private fun setMinSdkData() {
    binding.progressHorizontal.show()
    queryJob?.cancel()
    queryJob = lifecycleScope.launch(Dispatchers.IO) {
      val context = context ?: return@launch

      val entries: ArrayList<BarEntry> = ArrayList()
      var packageInfo: PackageInfo

      val filteredList = if (GlobalValues.isShowSystemApps.value == true) {
        viewModel.dbItems.value
      } else {
        viewModel.dbItems.value?.filter { !it.isSystem }
      }

      filteredList?.let {
        val apiMap = mutableMapOf<Int, Int>()

        var minSdk: Int
        for (item in it) {
          try {
            packageInfo = PackageUtils.getPackageInfo(item.packageName)
            minSdk = packageInfo.applicationInfo.minSdkVersion
            apiMap[minSdk] = apiMap[minSdk]?.plus(1) ?: 1
          } catch (e: Exception) {
            Timber.e(e)
          }
        }
        val sortedApiMap = TreeMap(apiMap)
        withContext(Dispatchers.Main) {
          if (chartView.parent != null) {
            binding.root.removeView(chartView)
          }
          chartView = generateBarChartView(sortedApiMap)
          binding.root.addView(chartView, -1)
        }

        legendList.clear()
        var index = 0
        sortedApiMap.forEach { entry ->
          entries.add(BarEntry(index.toFloat(), entry.value.toFloat()))
          legendList.add(entry.key.toString())
          index++
        }
        val dataSet = BarDataSet(entries, "").apply {
          setDrawIcons(false)
          valueFormatter = IntegerFormatter()
        }

        // add a lot of colors
        val colors: ArrayList<Int> = ArrayList()
        (0..apiScope).forEach { _ ->
          colors.add(UiUtils.getRandomColor())
        }

        dataSet.colors = colors
        // dataSet.setSelectionShift(0f);
        val data = BarData(dataSet).apply {
          setValueTextSize(10f)
          setValueTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface))
        }

        withContext(Dispatchers.Main) {
          (chartView as HorizontalBarChart).apply {
            this.data = data
            highlightValues(null)
            invalidate()
          }
          binding.progressHorizontal.hide()
        }
      }
    }
  }

  override fun onNothingSelected() {
    Timber.d("Nothing selected")
  }

  override fun onValueSelected(e: Entry?, h: Highlight?) {
    if (e == null) return
    if (h == null) return
    if (dialog != null) return

    if (OsUtils.atLeastR()) {
      chartView.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    dialog = ClassifyBottomSheetDialogFragment()
    viewModel.filteredList.postValue(emptyList())

    lifecycleScope.launch(Dispatchers.IO) {
      var dialogTitle = ""
      var item: List<LCItem> = emptyList()

      val filteredList = if (GlobalValues.isShowSystemApps.value == true) {
        viewModel.dbItems.value
      } else {
        viewModel.dbItems.value?.filter { !it.isSystem }
      }

      when (chartType) {
        TYPE_ABI -> {
          when (legendList.getOrNull(h.x.toInt())) {
            getString(R.string.string_64_bit) -> {
              dialogTitle = String.format(
                getString(R.string.title_statistics_dialog),
                getString(R.string.string_64_bit)
              )
              filteredList?.filter { (it.abi % MULTI_ARCH) in ABI_64_BIT }
                ?.let { filter ->
                  item = ArrayList(filter)
                }
            }
            getString(R.string.string_32_bit) -> {
              dialogTitle = String.format(
                getString(R.string.title_statistics_dialog),
                getString(R.string.string_32_bit)
              )
              filteredList?.filter { (it.abi % MULTI_ARCH) in ABI_32_BIT }
                ?.let { filter ->
                  item = ArrayList(filter)
                }
            }
            getString(R.string.no_libs) -> {
              dialogTitle = getString(R.string.title_statistics_dialog_no_native_libs)
              filteredList?.filter { it.abi == NO_LIBS.toShort() }
                ?.let { filter ->
                  item = ArrayList(filter)
                }
            }
          }
          viewModel.androidVersion.postValue(null)
        }
        TYPE_KOTLIN -> {
          when (legendList.getOrNull(h.x.toInt())) {
            getString(R.string.string_kotlin_used) -> {
              dialogTitle = getString(R.string.string_kotlin_used)
              filteredList?.filter { (it.features and Features.KOTLIN_USED) > 0 }
                ?.let { filter ->
                  item = ArrayList(filter)
                }
            }
            getString(R.string.string_kotlin_unused) -> {
              dialogTitle = getString(R.string.string_kotlin_unused)
              filteredList?.filter { (it.features and Features.KOTLIN_USED) == 0 }
                ?.let { filter ->
                  item = ArrayList(filter)
                }
            }
          }
          viewModel.androidVersion.postValue(null)
        }
        TYPE_TARGET_API -> {
          val targetApi = legendList.getOrNull(h.x.toInt())?.toInt() ?: 0
          var packageInfo: PackageInfo?

          dialogTitle = "Target SDK $targetApi"
          viewModel.androidVersion.postValue(AndroidVersions.versions.find { it.first == targetApi })
          filteredList?.filter {
            packageInfo = runCatching {
              PackageUtils.getPackageInfo(it.packageName)
            }.getOrNull()
            packageInfo?.applicationInfo?.targetSdkVersion == targetApi
          }?.let { filter -> item = ArrayList(filter) }
        }
        TYPE_MIN_SDK -> {
          val minSdk = legendList.getOrNull(h.x.toInt())?.toInt() ?: 0

          dialogTitle = "Min SDK $minSdk"
          viewModel.androidVersion.postValue(AndroidVersions.versions.find { it.first == minSdk })
          filteredList?.filter {
            runCatching { PackageUtils.getPackageInfo(it.packageName) }.getOrNull()
              ?.let { it.applicationInfo.minSdkVersion == minSdk } ?: false
          }?.let { filter -> item = ArrayList(filter) }
        }
      }

      viewModel.dialogTitle.postValue(dialogTitle)
      viewModel.filteredList.postValue(item)

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
    when (checkedId) {
      R.id.btn_abi -> if (isChecked) {
        chartType = TYPE_ABI
      }
      R.id.btn_kotlin -> if (isChecked) {
        chartType = TYPE_KOTLIN
      }
      R.id.btn_target_api -> if (isChecked) {
        chartType = TYPE_TARGET_API
      }
      R.id.btn_min_sdk -> if (isChecked) {
        chartType = TYPE_MIN_SDK
      }
    }
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

  private fun generateBarChartView(map: TreeMap<Int, Int>): HorizontalBarChart {
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
        valueFormatter = OsVersionAxisFormatter(map.map { it.key })
        position = XAxis.XAxisPosition.BOTTOM
        setDrawGridLines(false)
        setDrawLabels(true)
        setLabelCount(map.size, false)
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
      setMaxVisibleValueCount(apiScope)
      setDrawGridBackground(false)
      setDrawBorders(false)
      setDrawMarkers(false)
      setExtraOffsets(12f, 0f, 24f, 0f)
      setNoDataText(getString(R.string.loading))
      setNoDataTextColor(colorOnSurface)
      setOnChartValueSelectedListener(this@ChartFragment)
    }
  }
}
