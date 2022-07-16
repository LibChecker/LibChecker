package com.absinthe.libchecker.ui.fragment.detail

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.Settings
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.R
import com.absinthe.libchecker.compat.PackageManagerCompat
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.recyclerview.adapter.detail.AppInfoAdapter
import com.absinthe.libchecker.ui.detail.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.showToast
import com.absinthe.libchecker.view.detail.AppInfoBottomSheetView
import com.absinthe.libraries.utils.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import java.io.File

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
        if (packageName == BuildConfig.APPLICATION_ID) {
          Toasty.showShort(requireContext(), "But why…")
        } else {
          startLaunchAppActivity(packageName)
        }
      } catch (e: Exception) {
        activity?.let {
          AlternativeLaunchBSDFragment().apply {
            arguments = bundleOf(
              EXTRA_PACKAGE_NAME to packageName
            )
            show(it.supportFragmentManager, tag)
          }
        }
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

    aiAdapter.also { adapter ->
      adapter.setList(getResolveInfoList() + getMaterialFilesItem())
      adapter.setOnItemClickListener { _, _, position ->
        adapter.data[position].let {
          runCatching {
            startActivity(it.intent)
          }.onFailure {
            context?.let { ctx ->
              Toasty.showShort(ctx, R.string.toast_cant_open_app)
            }
          }
        }
        dismiss()
      }
    }
  }

  private fun getResolveInfoList(): List<AppInfoAdapter.AppInfoItem> {
    return PackageManagerCompat.queryIntentActivities(
      Intent(Intent.ACTION_SHOW_APP_INFO),
      PackageManager.MATCH_DEFAULT_ONLY
    ).filter { it.activityInfo.packageName != BuildConfig.APPLICATION_ID }
      .map {
        AppInfoAdapter.AppInfoItem(
          it.activityInfo,
          Intent(Intent.ACTION_SHOW_APP_INFO)
            .setComponent(ComponentName(it.activityInfo.packageName, it.activityInfo.name))
            .putExtra(Intent.EXTRA_PACKAGE_NAME, packageName)
        )
      }
  }

  private fun getMaterialFilesItem(): List<AppInfoAdapter.AppInfoItem> {
    val pkg = packageName ?: return emptyList()

    if (!PackageUtils.isAppInstalled(Constants.PackageNames.MATERIAL_FILES)) {
      return emptyList()
    }

    val sourceDir = runCatching {
      File(PackageUtils.getPackageInfo(pkg).applicationInfo.sourceDir).parent
    }.getOrNull() ?: return emptyList()

    return listOf(
      AppInfoAdapter.AppInfoItem(
        PackageUtils.getPackageInfo(Constants.PackageNames.MATERIAL_FILES).applicationInfo,
        Intent(Intent.ACTION_VIEW)
          .setType(DocumentsContract.Document.MIME_TYPE_DIR)
          .setComponent(
            ComponentName(
              Constants.PackageNames.MATERIAL_FILES,
              "${Constants.PackageNames.MATERIAL_FILES}.filelist.FileListActivity"
            )
          )
          .putExtra("org.openintents.extra.ABSOLUTE_PATH", sourceDir)
      )
    )
  }

  private fun startLaunchAppActivity(packageName: String?) {
    if (packageName == null) {
      return
    }
    val launcherActivity: String
    val intent = Intent(Intent.ACTION_MAIN, null)
      .addCategory(Intent.CATEGORY_LAUNCHER)
      .setPackage(packageName)
    val info = PackageManagerCompat.queryIntentActivities(intent, 0)
    launcherActivity = info.getOrNull(0)?.activityInfo?.name.orEmpty()
    val launchIntent = Intent(Intent.ACTION_MAIN)
      .addCategory(Intent.CATEGORY_LAUNCHER)
      .setClassName(packageName, launcherActivity)
      .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(launchIntent)
  }
}
