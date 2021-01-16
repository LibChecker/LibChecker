package com.absinthe.libchecker.ui.fragment.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.absinthe.libchecker.databinding.LayoutCloudRuleDialogBinding
import com.absinthe.libchecker.view.BaseBottomSheetDialogFragment

class CloudRulesDialogFragment : BaseBottomSheetDialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = LayoutCloudRuleDialogBinding.inflate(layoutInflater)
            binding.header.tvTitle.text = "Cloud rules"
        return binding.root
    }
}