package com.absinthe.libchecker.ui.fragment.statistics

import android.view.View
import com.absinthe.libchecker.R
import com.absinthe.libchecker.databinding.FragmentStatisticsBinding
import com.absinthe.libchecker.ui.fragment.BaseFragment

class StatisticsFragment : BaseFragment<FragmentStatisticsBinding>(R.layout.fragment_statistics) {

    override fun initBinding(view: View): FragmentStatisticsBinding = FragmentStatisticsBinding.bind(view)

    override fun init() {
        setHasOptionsMenu(true)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, LibReferenceFragment())
            .commit()
    }
}
