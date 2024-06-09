package com.absinthe.libchecker.features.applist.detail.ui

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
import com.absinthe.libchecker.features.applist.detail.ui.adapter.AppInfoAdapter
import com.absinthe.libchecker.features.applist.detail.ui.view.AppInfoBottomSheetView
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.showToast
import com.absinthe.libraries.utils.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import java.io.File

/**
 * <pre>
 * author : Absinthe
 * time : 2020/10/25
 * </pre>
 */

class AppInfoBottomSheetDialogFragment : BaseBottomSheetViewDialogFragment<AppInfoBottomSheetView>() {

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
          PackageUtils.startLaunchAppActivity(requireContext(), packageName)
        }
      } catch (e: Exception) {
        activity?.let {
          AlternativeLaunchBSDFragment().apply {
            arguments = bundleOf(
              EXTRA_PACKAGE_NAME to packageName
            )
            show(it.supportFragmentManager, AlternativeLaunchBSDFragment::class.java.name)
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
      adapter.setList(getShowAppInfoList() + getShowAppSourceList())
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

  private fun getShowAppInfoList(): List<AppInfoAdapter.AppInfoItem> {
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
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
      }
  }

  private fun getShowAppSourceList(): List<AppInfoAdapter.AppInfoItem> {
    val pkg = packageName ?: return emptyList()
    val sourceDir = runCatching {
      File(PackageUtils.getPackageInfo(pkg).applicationInfo.sourceDir).parent
    }.getOrNull() ?: return emptyList()

    return PackageManagerCompat.queryIntentActivities(
      Intent(Intent.ACTION_VIEW).also {
        it.setType(DocumentsContract.Document.MIME_TYPE_DIR)
      },
      PackageManager.MATCH_DEFAULT_ONLY
    ).filter { it.activityInfo.packageName != BuildConfig.APPLICATION_ID }
      .map {
        AppInfoAdapter.AppInfoItem(
          it.activityInfo,
          Intent(Intent.ACTION_VIEW)
            .setType(DocumentsContract.Document.MIME_TYPE_DIR)
            .setComponent(ComponentName(it.activityInfo.packageName, it.activityInfo.name))
            .putExtra("org.openintents.extra.ABSOLUTE_PATH", sourceDir)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
      }
  }
}
