package com.absinthe.libchecker.ui.fragment.detail

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.R
import com.absinthe.libchecker.databinding.LayoutBottomSheetAppInfoBinding
import com.absinthe.libchecker.extensions.addPaddingTop
import com.absinthe.libchecker.extensions.addSystemBarPaddingAsync
import com.absinthe.libchecker.extensions.dp
import com.absinthe.libchecker.recyclerview.adapter.AppInfoAdapter
import com.absinthe.libchecker.ui.detail.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.ui.fragment.BaseBottomSheetDialogFragment
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.Toasty

/**
 * <pre>
 * author : Absinthe
 * time : 2020/10/25
 * </pre>
 */

class AppInfoBottomShellDialogFragment : BaseBottomSheetDialogFragment<LayoutBottomSheetAppInfoBinding>() {

    private val packageName by lazy { arguments?.getString(EXTRA_PACKAGE_NAME) }
    private val mAdapter = AppInfoAdapter()

    override fun initBinding(): LayoutBottomSheetAppInfoBinding = LayoutBottomSheetAppInfoBinding.inflate(layoutInflater)

    override fun init() {
        initView(binding)
    }

    private fun initView(binding: LayoutBottomSheetAppInfoBinding) {
        binding.root.addPaddingTop(16.dp)
        binding.infoLaunch.setOnClickListener {
            try {
                startLaunchAppActivity(packageName)
            } catch (e: ActivityNotFoundException) {
                Toasty.show(requireContext(), R.string.toast_cant_open_app)
            } catch (e: NullPointerException) {
                Toasty.show(requireContext(), R.string.toast_package_name_null)
            } finally {
                dismiss()
            }
        }
        binding.infoSettings.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            dismiss()
        }
        binding.rvList.apply {
            adapter = mAdapter
            layoutManager = StaggeredGridLayoutManager(4, StaggeredGridLayoutManager.VERTICAL)
            setHasFixedSize(true)
            addSystemBarPaddingAsync(addStatusBarPadding = false)
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

    private fun startLaunchAppActivity(packageName: String?) {
        if (packageName == null) {
            return
        }
        val launcherActivity: String
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        intent.setPackage(packageName)
        val pm = requireActivity().packageManager
        val info = pm.queryIntentActivities(intent, 0)
        launcherActivity = if (info.size == 0) "" else info[0].activityInfo.name
        val launchIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setClassName(packageName, launcherActivity)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(launchIntent)
    }
}