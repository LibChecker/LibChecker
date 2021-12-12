package com.absinthe.libchecker.ui.fragment.detail

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.R
import com.absinthe.libchecker.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.recyclerview.adapter.detail.AppInfoAdapter
import com.absinthe.libchecker.ui.detail.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.showToast
import com.absinthe.libchecker.view.app.BottomSheetHeaderView
import com.absinthe.libchecker.view.detail.AppInfoBottomSheetView

/**
 * <pre>
 * author : Absinthe
 * time : 2020/10/25
 * </pre>
 */

class AppInfoBottomSheetDialogFragment :
  BaseBottomSheetViewDialogFragment<AppInfoBottomSheetView>() {

  private val packageName by lazy { arguments?.getString(EXTRA_PACKAGE_NAME) }
  private val aiAdapter = AppInfoAdapter()

  override fun initRootView(): AppInfoBottomSheetView = AppInfoBottomSheetView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    root.apply {
      layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      setPadding(24.dp, 16.dp, 24.dp, 0)
    }
    root.launch.setOnClickListener {
      try {
        startLaunchAppActivity(packageName)
      } catch (e: NullPointerException) {
        context?.showToast(R.string.toast_package_name_null)
      } catch (e: Exception) {
        context?.showToast(R.string.toast_cant_open_app)
      } finally {
        dismiss()
      }
    }
    root.setting.setOnClickListener {
      try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
          .setData(Uri.parse("package:$packageName"))
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
      } catch (e: Exception) {
        context?.showToast(R.string.toast_cant_open_app)
      } finally {
        dismiss()
      }
    }
    root.list.apply {
      adapter = aiAdapter
      layoutManager = StaggeredGridLayoutManager(4, StaggeredGridLayoutManager.VERTICAL)
      setHasFixedSize(true)
    }

    if (LCAppUtils.atLeastN()) {
      aiAdapter.also { adapter ->
        adapter.setList(getResolveInfoList())
        adapter.setOnItemClickListener { _, _, position ->
          adapter.data[position].let {
            val intent = Intent()
              .setComponent(
                ComponentName(it.activityInfo.packageName, it.activityInfo.name)
              )
              .putExtra(Intent.EXTRA_PACKAGE_NAME, packageName)
            try {
              startActivity(intent)
            } catch (e: Exception) {
              context?.let { ctx ->
                Toasty.showShort(ctx, R.string.toast_cant_open_app)
              }
            }
          }
          dismiss()
        }
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
      .addCategory(Intent.CATEGORY_LAUNCHER)
      .setPackage(packageName)
    val pm = requireActivity().packageManager
    val info = pm.queryIntentActivities(intent, 0)
    launcherActivity = if (info.size == 0) "" else info[0].activityInfo.name
    val launchIntent = Intent(Intent.ACTION_MAIN)
      .addCategory(Intent.CATEGORY_LAUNCHER)
      .setClassName(packageName, launcherActivity)
      .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
      startActivity(launchIntent)
    } catch (e: Exception) {
      Toasty.showShort(requireContext(), R.string.toast_cant_open_app)
    }
  }
}
