package com.absinthe.libchecker.ui.classify

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.absinthe.libchecker.R
import com.absinthe.libchecker.databinding.FragmentClassifyBinding
import com.absinthe.libchecker.utils.GlobalValues
import com.absinthe.libchecker.view.ClassifyDialogFragment
import com.absinthe.libchecker.viewholder.ARMV5
import com.absinthe.libchecker.viewholder.ARMV7
import com.absinthe.libchecker.viewholder.ARMV8
import com.absinthe.libchecker.viewholder.NO_LIBS
import com.absinthe.libchecker.viewmodel.AppViewModel
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


class ClassifyFragment : Fragment(), OnChartValueSelectedListener {

    private lateinit var binding: FragmentClassifyBinding
    private lateinit var viewModel: AppViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProvider(requireActivity()).get(AppViewModel::class.java)
        binding = FragmentClassifyBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        binding.chart.apply {
            dragDecelerationFrictionCoef = 0.95f
            description = null
            legend.apply {
                verticalAlignment = Legend.LegendVerticalAlignment.TOP
                horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
                orientation = Legend.LegendOrientation.VERTICAL
                textColor = resources.getColor(R.color.textNormal)
                xEntrySpace = 7f
                yEntrySpace = 0f
                yOffset = 0f
                setDrawInside(false)
            }
            setUsePercentValues(true)
            setExtraOffsets(5f, 10f, 5f, 5f)
            setEntryLabelColor(Color.BLACK)
            setNoDataText(getString(R.string.chart_no_data_text))
            setNoDataTextColor(resources.getColor(R.color.textNormal))
            setOnChartValueSelectedListener(this@ClassifyFragment)
        }

        viewModel.items.observe(viewLifecycleOwner, Observer {
            setData()
        })

        GlobalValues.isShowSystemApps.observe(viewLifecycleOwner, Observer {
            setData()
        })
    }

    private fun setData() {
        val parties = listOf(
            resources.getString(R.string.string_64_bit),
            resources.getString(R.string.string_32_bit),
            resources.getString(R.string.no_libs)
        )
        val entries: ArrayList<PieEntry> = ArrayList()

        viewModel.items.value?.let {
            val list = mutableListOf(0, 0, 0)

            for (item in it) {
                when (item.abi) {
                    ARMV8 -> list[0]++
                    ARMV7 -> list[1]++
                    ARMV5 -> list[1]++
                    NO_LIBS -> list[2]++
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
            for (c in ColorTemplate.LIBERTY_COLORS) colors.add(c)
            for (c in ColorTemplate.PASTEL_COLORS) colors.add(c)
            colors.add(ColorTemplate.getHoloBlue())

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

        val dialogFragment = ClassifyDialogFragment()

        when (h.x) {
            0f -> {
                viewModel.items.value?.filter { it.abi == ARMV8 }?.let { filter ->
                    dialogFragment.item = ArrayList(filter)
                }
            }
            1f -> {
                viewModel.items.value?.filter { it.abi == ARMV7 || it.abi == ARMV5 }
                    ?.let { filter ->
                        dialogFragment.item = ArrayList(filter)
                    }
            }
            2f -> {
                viewModel.items.value?.filter { it.abi == NO_LIBS }?.let { filter ->
                    dialogFragment.item = ArrayList(filter)
                }
            }
        }

        dialogFragment.show(requireActivity().supportFragmentManager, dialogFragment.tag)
    }
}
