package com.absinthe.libchecker.ui.fragment.statistics

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.base.BaseFragment
import com.absinthe.libchecker.constant.Constants.ARMV5
import com.absinthe.libchecker.constant.Constants.ARMV7
import com.absinthe.libchecker.constant.Constants.ARMV8
import com.absinthe.libchecker.constant.Constants.NO_LIBS
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.databinding.FragmentPieChartBinding
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.getColor
import com.absinthe.libchecker.utils.extensions.isShowing
import com.absinthe.libchecker.view.statistics.IntegerFormatter
import com.absinthe.libchecker.view.statistics.OsVersionAxisFormatter
import com.absinthe.libchecker.viewmodel.ChartViewModel
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

private const val TYPE_ABI = 0
private const val TYPE_KOTLIN = 1
private const val TYPE_TARGET_API = 2

class ChartFragment :
  BaseFragment<FragmentPieChartBinding>(),
  OnChartValueSelectedListener,
  MaterialButtonToggleGroup.OnButtonCheckedListener {

  private val viewModel: ChartViewModel by activityViewModels()
  private val legendList = mutableListOf<String>()
  private val existApiList = mutableListOf<Int>()
  private var chartType = TYPE_ABI
  private var mDialog: ClassifyBottomSheetDialogFragment? = null
  private lateinit var chartView: ViewGroup

  override fun init() {
    chartView = generatePieChartView()
    binding.root.addView(chartView, -1)

    binding.buttonsGroup.apply {
      addOnButtonCheckedListener(this@ChartFragment)
      check(R.id.btn_abi)
    }

    viewModel.apply {
      dbItems.observe(viewLifecycleOwner) {
        setData()
      }
    }

    GlobalValues.isShowSystemApps.observe(viewLifecycleOwner) {
      setData()
    }
  }

  private fun setData() {
    when (chartType) {
      TYPE_ABI -> setAbiData()
      TYPE_KOTLIN -> setKotlinData()
      TYPE_TARGET_API -> setTargetApiData()
    }
  }

  private fun setAbiData() {
    if (chartView.parent != null) {
      binding.root.removeView(chartView)
    }
    chartView = generatePieChartView()
    binding.root.addView(chartView, -1)

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

    filteredList?.let {
      val list = mutableListOf(0, 0, 0)

      for (item in it) {
        if (GlobalValues.isShowSystemApps.value == false) {
          if (item.isSystem) continue
        }
        when (item.abi) {
          ARMV8.toShort() -> list[0]++
          ARMV5.toShort(), ARMV7.toShort() -> list[1]++
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
      }

      // add a lot of colors
      val colors: ArrayList<Int> = ArrayList()
      for (c in ColorTemplate.MATERIAL_COLORS) colors.add(c)

      dataSet.colors = colors
      // dataSet.setSelectionShift(0f);
      val data = PieData(dataSet).apply {
        setValueFormatter(PercentFormatter(chartView as PieChart))
        setValueTextSize(10f)
        setValueTextColor(R.color.textNormal.getColor(requireContext()))
      }

      (chartView as PieChart).apply {
        this.data = data
        setEntryLabelColor(R.color.textNormal.getColor(requireContext()))
        highlightValues(null)
        invalidate()
      }
    }
  }

  private fun setKotlinData() {
    if (chartView.parent != null) {
      binding.root.removeView(chartView)
    }
    chartView = generatePieChartView()
    binding.root.addView(chartView, -1)

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

    filteredList?.let {
      val list = mutableListOf(0, 0)

      for (item in it) {
        if (item.isKotlinUsed) {
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
      }

      // add a lot of colors
      val colors: ArrayList<Int> = ArrayList()
      for (c in ColorTemplate.LIBERTY_COLORS) colors.add(c)

      dataSet.colors = colors
      // dataSet.setSelectionShift(0f);
      val data = PieData(dataSet).apply {
        setValueFormatter(PercentFormatter(chartView as PieChart))
        setValueTextSize(10f)
        setValueTextColor(Color.BLACK)
      }

      (chartView as PieChart).apply {
        this.data = data
        setEntryLabelColor(Color.BLACK)
        highlightValues(null)
        invalidate()
      }
    }
  }

  private fun setTargetApiData() {
    val parties = osNameMap.map { it.value }.toMutableList()

    val entries: ArrayList<BarEntry> = ArrayList()
    var packageInfo: PackageInfo

    val filteredList = if (GlobalValues.isShowSystemApps.value == true) {
      viewModel.dbItems.value
    } else {
      viewModel.dbItems.value?.filter { !it.isSystem }
    }

    filteredList?.let {
      val list = IntArray(Build.VERSION_CODES.R + 1) { 0 }

      var targetApi: Int
      for (item in it) {
        try {
          packageInfo = PackageUtils.getPackageInfo(item.packageName)
          targetApi = packageInfo.applicationInfo.targetSdkVersion
          if (targetApi > 0 && targetApi <= Build.VERSION_CODES.R + 1) {
            list[targetApi - 1]++
          }
        } catch (e: PackageManager.NameNotFoundException) {
          Timber.e(e)
        }
      }
      existApiList.clear()
      val iterator = list.iterator().withIndex()
      while (iterator.hasNext()) {
        val entry = iterator.next()
        if (entry.value != 0) {
          existApiList.add(entry.index + 1)
        }
      }

      if (chartView.parent != null) {
        binding.root.removeView(chartView)
      }
      chartView = generateBarChartView()
      binding.root.addView(chartView, -1)

      // NOTE: The order of the entries when being added to the entries array determines their position around the center of
      // the chart.
      legendList.clear()
      for (i in parties.indices) {
        if (list[i] > 0) {
          entries.add(BarEntry(existApiList.indexOf(i + 1).toFloat(), list[i].toFloat()))
          legendList.add((i + 1).toString())
        }
      }
      val dataSet = BarDataSet(entries, "").apply {
        setDrawIcons(false)
        valueFormatter = IntegerFormatter()
      }

      // add a lot of colors
      val colors: ArrayList<Int> = ArrayList()
      for (c in ColorTemplate.COLORFUL_COLORS) colors.add(c)
      for (c in ColorTemplate.MATERIAL_COLORS) colors.add(c)

      dataSet.colors = colors
      // dataSet.setSelectionShift(0f);
      val data = BarData(dataSet).apply {
        setValueTextSize(10f)
        setValueTextColor(R.color.textNormal.getColor(requireContext()))
      }

      (chartView as HorizontalBarChart).apply {
        this.data = data
        highlightValues(null)
        invalidate()
      }
    }
  }

  override fun onNothingSelected() {
    Timber.d("Nothing selected")
  }

  override fun onValueSelected(e: Entry?, h: Highlight?) {
    if (e == null) return
    if (h == null) return
    if (mDialog != null && mDialog!!.isShowing()) return

    if (LCAppUtils.atLeastR()) {
      chartView.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    var dialogTitle = ""
    viewModel.filteredList.postValue(emptyList())

    lifecycleScope.launch(Dispatchers.IO) {
      var item: List<LCItem> = emptyList()

      val filteredList = if (GlobalValues.isShowSystemApps.value == true) {
        viewModel.dbItems.value
      } else {
        viewModel.dbItems.value?.filter { !it.isSystem }
      }

      when (chartType) {
        TYPE_ABI -> {
          when (legendList[h.x.toInt()]) {
            getString(R.string.string_64_bit) -> {
              dialogTitle = String.format(
                getString(R.string.title_statistics_dialog),
                getString(R.string.string_64_bit)
              )
              filteredList?.filter { it.abi == ARMV8.toShort() }
                ?.let { filter ->
                  item = ArrayList(filter)
                }
            }
            getString(R.string.string_32_bit) -> {
              dialogTitle = String.format(
                getString(R.string.title_statistics_dialog),
                getString(R.string.string_32_bit)
              )
              filteredList?.filter { it.abi == ARMV7.toShort() || it.abi == ARMV5.toShort() }
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
        }
        TYPE_KOTLIN -> {
          when (legendList[h.x.toInt()]) {
            getString(R.string.string_kotlin_used) -> {
              dialogTitle = getString(R.string.string_kotlin_used)
              filteredList?.filter { it.isKotlinUsed }
                ?.let { filter ->
                  item = ArrayList(filter)
                }
            }
            getString(R.string.string_kotlin_unused) -> {
              dialogTitle = getString(R.string.string_kotlin_unused)
              filteredList?.filter { !it.isKotlinUsed }
                ?.let { filter ->
                  item = ArrayList(filter)
                }
            }
          }
        }
        TYPE_TARGET_API -> {
          val targetApi = legendList[h.x.toInt()].toInt()
          var packageInfo: PackageInfo?

          dialogTitle = "Target API $targetApi"
          filteredList?.filter {
            packageInfo = try {
              PackageUtils.getPackageInfo(it.packageName)
            } catch (e: PackageManager.NameNotFoundException) {
              null
            }
            packageInfo?.applicationInfo?.targetSdkVersion == targetApi
          }?.let { filter -> item = ArrayList(filter) }
        }
      }

      viewModel.dialogTitle.postValue(dialogTitle)
      viewModel.filteredList.postValue(item)
    }

    mDialog = ClassifyBottomSheetDialogFragment().apply {
      setOnDismissListener(object : ClassifyBottomSheetDialogFragment.OnDismissListener {
        override fun onDismiss() {
          mDialog = null
        }
      })
    }.also {
      it.show(requireActivity().supportFragmentManager, tag)
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
    }
    setData()
  }

  private fun generatePieChartView(): PieChart {
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
        textColor = R.color.textNormal.getColor(requireContext())
        xEntrySpace = 7f
        yEntrySpace = 0f
        isWordWrapEnabled = true
      }
      setUsePercentValues(true)
      setExtraOffsets(24f, 0f, 24f, 0f)
      setEntryLabelColor(R.color.textNormal.getColor(requireContext()))
      setEntryLabelTextSize(11f)
      setNoDataText(getString(R.string.loading))
      setNoDataTextColor(R.color.textNormal.getColor(requireContext()))
      setOnChartValueSelectedListener(this@ChartFragment)
      setHoleColor(Color.TRANSPARENT)
    }
  }

  private fun generateBarChartView(): HorizontalBarChart {
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
        valueFormatter = OsVersionAxisFormatter(existApiList)
        position = XAxis.XAxisPosition.BOTTOM
        setDrawGridLines(false)
        setDrawLabels(true)
        setLabelCount(existApiList.size, false)
        granularity = 1f
        textSize = 10f
        textColor = R.color.textNormal.getColor(requireContext())
      }
      axisLeft.apply {
        valueFormatter = IntegerFormatter()
        setDrawGridLines(false)
        setDrawZeroLine(false)
        textColor = R.color.textNormal.getColor(requireContext())
      }
      axisRight.apply {
        valueFormatter = IntegerFormatter()
        setDrawGridLines(false)
        setDrawZeroLine(false)
        textColor = R.color.textNormal.getColor(requireContext())
      }
      setMaxVisibleValueCount(Build.VERSION_CODES.R + 1)
      setDrawGridBackground(false)
      setDrawBorders(false)
      setDrawMarkers(false)
      setExtraOffsets(12f, 0f, 24f, 0f)
      setNoDataText(getString(R.string.loading))
      setNoDataTextColor(R.color.textNormal.getColor(context))
      setOnChartValueSelectedListener(this@ChartFragment)
    }
  }

  private val osNameMap by lazy {
    hashMapOf(
      1 to "1.0",
      2 to "1.1",
      3 to "Cupcake",
      4 to "Donut",
      5 to "Eclair",
      6 to "Eclair",
      7 to "Eclair",
      8 to "Froyo",
      9 to "Gingerbread",
      10 to "Gingerbread",
      11 to "Honeycomb",
      12 to "Honeycomb",
      13 to "Honeycomb",
      14 to "Ice Cream Sandwich",
      15 to "Ice Cream Sandwich",
      16 to "Jelly Bean",
      17 to "Jelly Bean",
      18 to "Jelly Bean",
      19 to "KitKat",
      20 to "KitKat",
      21 to "Lollipop",
      22 to "Lollipop",
      23 to "Marshmallow",
      24 to "Nougat",
      25 to "Nougat",
      26 to "Oreo",
      27 to "Oreo",
      28 to "Pie",
      29 to "Android10",
      30 to "Android11",
      31 to "S"
    )
  }
}
