package com.absinthe.libchecker.ui.fragment.snapshot

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import com.absinthe.libchecker.databinding.FragmentSnapshotBinding
import com.blankj.utilcode.util.BarUtils
import com.blankj.utilcode.util.ConvertUtils

class SnapshotFragment : Fragment() {

    private lateinit var binding: FragmentSnapshotBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSnapshotBinding.inflate(inflater, container, false)
        initView()
        return binding.root
    }

    private fun initView() {
        binding.root.setPadding(binding.root.paddingStart, binding.root.paddingTop + BarUtils.getStatusBarHeight(), binding.root.paddingEnd, 0)
        binding.extendedFab.apply {
            (layoutParams as CoordinatorLayout.LayoutParams)
                .setMargins(
                    0,
                    0,
                    ConvertUtils.dp2px(16f),
                    ConvertUtils.dp2px(16f) + paddingBottom
                )
        }
    }

}