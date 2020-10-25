package com.absinthe.libchecker.ui.fragment.detail

import android.content.ActivityNotFoundException
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.absinthe.libchecker.R
import com.absinthe.libchecker.databinding.LayoutBottomSheetAppInfoBinding
import com.absinthe.libchecker.ui.detail.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.utils.Toasty
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.IntentUtils
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * <pre>
 * author : Absinthe
 * time : 2020/10/25
 * </pre>
 */

class AppInfoBottomShellDialogFragment : BottomSheetDialogFragment() {

    private val packageName by lazy { arguments?.getString(EXTRA_PACKAGE_NAME) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = LayoutBottomSheetAppInfoBinding.inflate(inflater)
        initView(binding)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let {
            it.findViewById<View>(com.google.android.material.R.id.container).fitsSystemWindows = false
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.attributes?.windowAnimations = R.style.DialogAnimation
    }

    private fun initView(binding: LayoutBottomSheetAppInfoBinding) {
        binding.infoLaunch.setOnClickListener {
            try {
                startActivity(IntentUtils.getLaunchAppIntent(packageName))
            } catch (e: ActivityNotFoundException) {
                Toasty.show(requireContext(), R.string.toast_cant_open_app)
            } catch (e: NullPointerException) {
                Toasty.show(requireContext(), R.string.toast_package_name_null)
            }
        }
        binding.infoSettings.setOnClickListener {
            AppUtils.launchAppDetailsSettings(packageName)
        }
    }
}