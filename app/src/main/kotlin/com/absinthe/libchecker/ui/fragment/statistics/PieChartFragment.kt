package com.absinthe.libchecker.ui.fragment.statistics

import android.graphics.Color
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.absinthe.libchecker.R
import com.absinthe.libchecker.bean.*
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.database.AppItemRepository
import com.absinthe.libchecker.databinding.FragmentPieChartBinding
import com.absinthe.libchecker.ui.fragment.BaseFragment
import com.absinthe.libchecker.view.dialogfragment.ClassifyDialogFragment
import com.github.mikephil.charting.components.Legend
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

private const val TYPE_ABI = 0
private const val TYPE_KOTLIN = 1

class PieChartFragment : BaseFragment<FragmentPieChartBinding>(R.layout.fragment_pie_chart),
    OnChartValueSelectedListener,
    MaterialButtonToggleGroup.OnButtonCheckedListener {

    private var pieType = TYPE_ABI

    override fun initBinding(view: View): FragmentPieChartBinding =
        FragmentPieChartBinding.bind(view)

    override fun init() {
        binding.chart.apply {
            dragDecelerationFrictionCoef = 0.95f
            description = null
            legend.apply {
                verticalAlignment = Legend.LegendVerticalAlignment.TOP
                horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
                orientation = Legend.LegendOrientation.VERTICAL
                textColor = ContextCompat.getColor(context, R.color.textNormal)
                xEntrySpace = 7f
                yEntrySpace = 0f
                yOffset = 0f
            }
            setUsePercentValues(true)
            setExtraOffsets(5f, 10f, 5f, 5f)
            setEntryLabelColor(Color.BLACK)
            setNoDataText(getString(R.string.loading))
            setNoDataTextColor(ContextCompat.getColor(context, R.color.textNormal))
            setOnChartValueSelectedListener(this@PieChartFragment)
            setHoleColor(Color.TRANSPARENT)
        }

        binding.buttonsGroup.apply {
            addOnButtonCheckedListener(this@PieChartFragment)
            check(R.id.btn_abi)
        }

        AppItemRepository.allItems.observe(viewLifecycleOwner, Observer {
            setAbiData()
        })

        GlobalValues.isShowSystemApps.observe(viewLifecycleOwner, Observer {
            setAbiData()
        })
    }

    private fun setAbiData() {
        val parties = listOf(
            resources.getString(R.string.string_64_bit),
            resources.getString(R.string.string_32_bit),
            resources.getString(R.string.no_libs)
        )
        val entries: ArrayList<PieEntry> = ArrayList()

        AppItemRepository.allItems.value?.let {
            val list = mutableListOf(0, 0, 0)

            for (item in it) {
                when (item.abi) {
                    ARMV8 -> list[0]++
                    ARMV5, ARMV7 -> list[1]++
                    else -> list[2]++
                }
            }

            // NOTE: The order of the entries when being added to the entries array determines their position around the center of
            // the chart.
            for (i in parties.indices) {
                entries.add(
                    PieEntry(
                        list[i].toFloat(),
                        parties[i % parties.size],
                        resources.getDrawable(R.drawable.ic_logo, null)
                    )
                )
            }
            val dataSet = PieDataSet(entries, "").apply {
                setDrawIcons(false)
                sliceSpace = 3f
                iconsOffset = MPPointF(0f, 40f)
                selectionShift = 5f
            }

            // add a lot of colors
            val colors: ArrayList<Int> = ArrayList()
            for (c in ColorTemplate.VORDIPLOM_COLORS) colors.add(c)
            for (c in ColorTemplate.JOYFUL_COLORS) colors.add(c)
            for (c in ColorTemplate.COLORFUL_COLORS) colors.add(c)

            dataSet.colors = colors
            //dataSet.setSelectionShift(0f);
            val data = PieData(dataSet).apply {
                setValueFormatter(PercentFormatter(binding.chart))
                setValueTextSize(11f)
                setValueTextColor(Color.BLACK)
            }

            binding.chart.apply {
                this.data = data
                highlightValues(null)
                invalidate()
            }
        }
    }

    private fun setKotlinData() {
        val parties = listOf(
            resources.getString(R.string.string_kotlin_used),
            resources.getString(R.string.string_kotlin_unused)
        )
        val entries: ArrayList<PieEntry> = ArrayList()

        AppItemRepository.allItems.value?.let {
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
            for (i in parties.indices) {
                entries.add(
                    PieEntry(
                        list[i].toFloat(),
                        parties[i % parties.size],
                        resources.getDrawable(R.drawable.ic_kotlin_logo, null)
                    )
                )
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
            for (c in ColorTemplate.PASTEL_COLORS) colors.add(c)

            dataSet.colors = colors
            //dataSet.setSelectionShift(0f);
            val data = PieData(dataSet).apply {
                setValueFormatter(PercentFormatter(binding.chart))
                setValueTextSize(11f)
                setValueTextColor(Color.BLACK)
            }

            binding.chart.apply {
                this.data = data
                highlightValues(null)
                invalidate()
            }
        }
    }

    override fun onNothingSelected() {
        Log.d("Classify Fragment", "Nothing selected")
    }

    override fun onValueSelected(e: Entry?, h: Highlight?) {
        if (e == null) return
        if (h == null) return

        var dialogTitle = ""
        var item: List<AppItem> = listOf()

        if (pieType == TYPE_ABI) {
            when (h.x) {
                0f -> {
                    dialogTitle = String.format(
                        getString(R.string.title_statistics_dialog),
                        getString(R.string.string_64_bit)
                    )
                    AppItemRepository.allItems.value?.filter { it.abi == ARMV8 }
                        ?.let { filter ->
                            item = ArrayList(filter)
                        }
                }
                1f -> {
                    dialogTitle = String.format(
                        getString(R.string.title_statistics_dialog),
                        getString(R.string.string_32_bit)
                    )
                    AppItemRepository.allItems.value?.filter { it.abi == ARMV7 || it.abi == ARMV5 }
                        ?.let { filter ->
                            item = ArrayList(filter)
                        }
                }
                2f -> {
                    dialogTitle = getString(R.string.title_statistics_dialog_no_native_libs)
                    AppItemRepository.allItems.value?.filter { it.abi == NO_LIBS }
                        ?.let { filter ->
                            item = ArrayList(filter)
                        }
                }
            }
        } else if (pieType == TYPE_KOTLIN) {
            when (h.x) {
                0f -> {
                    dialogTitle = getString(R.string.string_kotlin_used)
                    AppItemRepository.allItems.value?.filter { it.isKotlinUsed }
                        ?.let { filter ->
                            item = ArrayList(filter)
                        }
                }
                1f -> {
                    dialogTitle = getString(R.string.string_kotlin_unused)
                    AppItemRepository.allItems.value?.filter { !it.isKotlinUsed }
                        ?.let { filter ->
                            item = ArrayList(filter)
                        }
                }
            }
        }

        val dialog = ClassifyDialogFragment.newInstance(dialogTitle)
        dialog.show(requireActivity().supportFragmentManager, tag)
        dialog.item = ArrayList(item)
    }

    override fun onButtonChecked(
        group: MaterialButtonToggleGroup?,
        checkedId: Int,
        isChecked: Boolean
    ) {
        when (checkedId) {
            R.id.btn_abi -> if (isChecked) {
                pieType = TYPE_ABI
                setAbiData()
            }
            R.id.btn_kotlin -> if (isChecked) {
                pieType = TYPE_KOTLIN
                setKotlinData()
            }
        }
    }
}
