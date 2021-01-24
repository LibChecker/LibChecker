package com.absinthe.libchecker.ui.fragment.statistics

import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.absinthe.libchecker.R
import com.absinthe.libchecker.databinding.FragmentStatisticsBinding
import com.absinthe.libchecker.extensions.addPaddingTop
import com.absinthe.libchecker.ui.fragment.BaseFragment
import com.absinthe.libraries.utils.utils.UiUtils
import com.google.android.material.tabs.TabLayoutMediator

class StatisticsFragment : BaseFragment<FragmentStatisticsBinding>(R.layout.fragment_statistics) {

    override fun initBinding(view: View): FragmentStatisticsBinding = FragmentStatisticsBinding.bind(view)

    override fun init() {
        setHasOptionsMenu(true)

        binding.root.addPaddingTop(UiUtils.getStatusBarHeight())
        binding.viewpager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int {
                return 2
            }

            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> LibReferenceFragment()
                    else -> PieChartFragment()
                }
            }
        }

        val mediator = TabLayoutMediator(binding.tabLayout, binding.viewpager) { tab, position ->
            when (position) {
                0 -> tab.text = getText(R.string.tab_lib_reference_statistics)
                else -> tab.text = getText(R.string.tab_pie_chart)
            }
        }
        mediator.attach()
    }
}
