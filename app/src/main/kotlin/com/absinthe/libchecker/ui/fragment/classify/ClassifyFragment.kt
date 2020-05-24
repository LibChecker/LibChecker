package com.absinthe.libchecker.ui.fragment.classify

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.absinthe.libchecker.R
import com.absinthe.libchecker.databinding.FragmentClassifyBinding
import com.blankj.utilcode.util.BarUtils
import com.google.android.material.tabs.TabLayoutMediator


class ClassifyFragment : Fragment() {

    private lateinit var binding: FragmentClassifyBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentClassifyBinding.inflate(inflater, container, false)
        initView()
        return binding.root
    }

    private fun initView() {
        binding.root.setPadding(0, binding.root.paddingTop + BarUtils.getStatusBarHeight(), 0, 0)
        binding.viewpager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int {
                return 2
            }

            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> PieChartFragment()
                    1 -> LibReferenceFragment()
                    else -> PieChartFragment()
                }
            }
        }

        val mediator = TabLayoutMediator(binding.tabLayout, binding.viewpager,
            TabLayoutMediator.TabConfigurationStrategy { tab, position ->
                when (position) {
                    0 -> tab.text = getText(R.string.tab_pie_chart)
                    1 -> tab.text = getText(R.string.tab_lib_reference_statistics)
                    else -> tab.text = getText(R.string.tab_pie_chart)
                }
            })
        mediator.attach()
    }
}
