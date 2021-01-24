package com.absinthe.libchecker.ui.fragment.detail

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.R
import com.absinthe.libchecker.databinding.LayoutBottomSheetAppInfoBinding
import com.absinthe.libchecker.extensions.addPaddingBottom
import com.absinthe.libchecker.recyclerview.adapter.AppInfoAdapter
import com.absinthe.libchecker.ui.detail.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.view.BaseBottomSheetDialogFragment
import com.absinthe.libraries.utils.utils.UiUtils
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.IntentUtils

/**
 * <pre>
 * author : Absinthe
 * time : 2020/10/25
 * </pre>
 */

class AppInfoBottomShellDialogFragment : BaseBottomSheetDialogFragment() {

    private val packageName by lazy { arguments?.getString(EXTRA_PACKAGE_NAME) }
    private val mAdapter = AppInfoAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = LayoutBottomSheetAppInfoBinding.inflate(inflater)
        initView(binding)
        return binding.root
    }

    private fun initView(binding: LayoutBottomSheetAppInfoBinding) {
        binding.infoLaunch.setOnClickListener {
            try {
                startActivity(IntentUtils.getLaunchAppIntent(packageName))
            } catch (e: ActivityNotFoundException) {
                Toasty.show(requireContext(), R.string.toast_cant_open_app)
            } catch (e: NullPointerException) {
                Toasty.show(requireContext(), R.string.toast_package_name_null)
            } finally {
                dismiss()
            }
        }
        binding.infoSettings.setOnClickListener {
            AppUtils.launchAppDetailsSettings(packageName)
            dismiss()
        }
        binding.rvList.apply {
            adapter = mAdapter
            layoutManager = StaggeredGridLayoutManager(4, StaggeredGridLayoutManager.VERTICAL)
            setHasFixedSize(true)
            addPaddingBottom(UiUtils.getNavBarHeight(requireActivity().contentResolver))
        }

        if (LCAppUtils.atLeastN()) {
            mAdapter.setList(getResolveInfoList())
            mAdapter.setOnItemClickListener { _, _, position ->
                val info = mAdapter.data[position]
                startActivity(Intent().apply {
                    component = ComponentName(info.activityInfo.packageName, info.activityInfo.name)
                    putExtra(Intent.EXTRA_PACKAGE_NAME, packageName)
                })
                dismiss()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun getResolveInfoList(): List<ResolveInfo> {
        return requireContext().packageManager.queryIntentActivities(
            Intent(Intent.ACTION_SHOW_APP_INFO), PackageManager.MATCH_DEFAULT_ONLY
        ).filter { it.activityInfo.packageName != BuildConfig.APPLICATION_ID }
    }
}