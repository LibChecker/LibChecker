package com.absinthe.libchecker.features.applist.detail.ui

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.text.SpannableString
import android.text.style.ImageSpan
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.core.view.isGone
import androidx.core.view.isVisible
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.URLManager
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.features.applist.detail.ui.view.AppInstallSourceBottomSheetView
import com.absinthe.libchecker.features.applist.detail.ui.view.AppInstallSourceItemView
import com.absinthe.libchecker.features.applist.detail.ui.view.AppInstallTimeItemView
import com.absinthe.libchecker.features.applist.detail.ui.view.CenterAlignImageSpan
import com.absinthe.libchecker.utils.FreezeUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.PREINSTALLED_TIMESTAMP
import com.absinthe.libchecker.utils.extensions.getDrawable
import com.absinthe.libchecker.utils.extensions.launchDetailPage
import com.absinthe.libchecker.utils.extensions.setLongClickCopiedToClipboard
import com.absinthe.libraries.utils.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import java.text.SimpleDateFormat
import kotlinx.coroutines.runBlocking
import rikka.shizuku.Shizuku
import rikka.sui.Sui

@RequiresApi(Build.VERSION_CODES.R)
class AppInstallSourceBSDFragment :
  BaseBottomSheetViewDialogFragment<AppInstallSourceBottomSheetView>(),
  Shizuku.OnBinderReceivedListener,
  Shizuku.OnRequestPermissionResultListener {

  private val packageName by lazy { arguments?.getString(EXTRA_PACKAGE_NAME) }

  override fun initRootView(): AppInstallSourceBottomSheetView = AppInstallSourceBottomSheetView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    val packageName = packageName ?: return
    val info = PackageUtils.getInstallSourceInfo(packageName) ?: return
    val pi = runCatching { PackageUtils.getPackageInfo(packageName) }.getOrNull() ?: return

    initOriginatingItemView(root.originatingView, info.originatingPackageName)
    initAppInstallSourceItemView(root.installingView, info.installingPackageName)
    initAppInstalledTimeItemView(root.installedTimeView, pi)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    Shizuku.removeRequestPermissionResultListener(this)
    Shizuku.removeBinderReceivedListener(this)
  }

  private fun initOriginatingItemView(
    item: AppInstallSourceItemView,
    originatingPackageName: String?
  ) {
    if (context == null) {
      item.isGone = true
      return
    }
    item.packageView.container.abiInfo.isVisible = false
    item.packageView.container.icon.load(com.absinthe.lc.rulesbundle.R.drawable.ic_lib_shizuku)
    if (!PackageUtils.isAppInstalled(Constants.PackageNames.SHIZUKU) && !Sui.isSui()) {
      item.packageView.container.appName.text =
        getString(R.string.lib_detail_app_install_source_shizuku_uninstalled)
      item.packageView.container.packageName.text =
        getString(R.string.lib_detail_app_install_source_shizuku_usage)
      item.packageView.container.versionInfo.text =
        getString(R.string.lib_detail_app_install_source_shizuku_uninstalled_detail)
      item.packageView.setOnClickListener {
        startActivity(
          Intent(Intent.ACTION_VIEW).apply {
            data = URLManager.SHIZUKU_APP_GITHUB_RELEASE_PAGE.toUri()
          }
        )
      }
    } else {
      if (!Shizuku.pingBinder()) {
        item.packageView.container.appName.text =
          getString(R.string.lib_detail_app_install_source_shizuku_not_running)
        item.packageView.container.packageName.text =
          getString(R.string.lib_detail_app_install_source_shizuku_usage)
        item.packageView.container.versionInfo.text =
          getString(R.string.lib_detail_app_install_source_shizuku_not_running_detail)
        item.packageView.setOnClickListener {
          PackageUtils.startLaunchAppActivity(requireContext(), Constants.PackageNames.SHIZUKU)
          Shizuku.addBinderReceivedListener(this)
        }
      } else {
        if (Shizuku.getVersion() < 10) {
          item.packageView.container.appName.text =
            getString(R.string.lib_detail_app_install_source_shizuku_low_version)
          item.packageView.container.packageName.text =
            getString(R.string.lib_detail_app_install_source_shizuku_usage)
          item.packageView.container.versionInfo.text =
            getString(R.string.lib_detail_app_install_source_shizuku_low_version_detail)
          item.packageView.setOnClickListener {
            startActivity(
              Intent(Intent.ACTION_VIEW).apply {
                data = URLManager.SHIZUKU_APP_GITHUB_RELEASE_PAGE.toUri()
              }
            )
          }
        } else if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
          item.packageView.container.appName.text =
            getString(R.string.lib_detail_app_install_source_shizuku_permission_not_granted)
          item.packageView.container.packageName.text =
            getString(R.string.lib_detail_app_install_source_shizuku_usage)
          item.packageView.container.versionInfo.text =
            getString(R.string.lib_detail_app_install_source_shizuku_permission_not_granted_detail)
          item.packageView.setOnClickListener {
            Shizuku.addRequestPermissionResultListener(this)
            Shizuku.requestPermission(0)
          }
        } else {
          initAppInstallSourceItemView(item, originatingPackageName)
        }
      }
    }
  }

  private fun initAppInstallSourceItemView(
    item: AppInstallSourceItemView,
    packageName: String?
  ) {
    if (context == null) {
      item.isGone = true
      return
    }

    if (packageName == null) {
      item.packageView.container.also {
        it.icon.load(R.drawable.ic_icon_blueprint)
        it.appName.text =
          getString(R.string.lib_detail_app_install_source_empty)
        it.packageName.text =
          getString(R.string.lib_detail_app_install_source_empty_detail)
        it.setVersionInfo("                                                                            ")
        it.setAbiInfo("                                            ")
      }
      item.packageView.setOnClickListener(null)
      return
    }

    val targetLCItem = runBlocking { Repositories.lcRepository.getItem(packageName) } ?: return

    val pi = runCatching {
      PackageUtils.getPackageInfo(packageName)
    }.getOrNull()
    item.packageView.container.also {
      it.icon.load(pi)
      it.appName.text = targetLCItem.label
      it.packageName.text = packageName
      it.versionInfo.text =
        PackageUtils.getVersionString(targetLCItem.versionName, targetLCItem.versionCode)
    }

    val str = StringBuilder()
      .append(PackageUtils.getAbiString(requireContext(), targetLCItem.abi.toInt(), true))
      .append(PackageUtils.getBuildVersionsInfo(pi, packageName))
    val spanString: SpannableString
    val abiBadgeRes = PackageUtils.getAbiBadgeResource(targetLCItem.abi.toInt())

    if (targetLCItem.abi.toInt() != Constants.OVERLAY && targetLCItem.abi.toInt() != Constants.ERROR && abiBadgeRes != 0) {
      spanString = SpannableString("  $str")
      abiBadgeRes.getDrawable(requireContext())?.let { drawable ->
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        val span = CenterAlignImageSpan(drawable)
        spanString.setSpan(span, 0, 1, ImageSpan.ALIGN_BOTTOM)
      }
      item.packageView.container.abiInfo.text = spanString
    } else {
      item.packageView.container.abiInfo.text = str
    }
    item.packageView.container.abiInfo.isVisible = true

    if (targetLCItem.variant == Constants.VARIANT_HAP) {
      item.packageView.container.setBadge(R.drawable.ic_harmony_badge)
    } else {
      item.packageView.container.setBadge(null)
    }

    item.packageView.setOnClickListener {
      activity?.finish()
      dismiss()
      activity?.launchDetailPage(targetLCItem)
    }

    Shizuku.removeRequestPermissionResultListener(this)
    Shizuku.removeBinderReceivedListener(this)
  }

  private fun initAppInstalledTimeItemView(item: AppInstallTimeItemView, pi: PackageInfo) {
    if (context == null || FreezeUtils.isAppFrozen(pi.packageName)) {
      item.isGone = true
      return
    }

    val firstInstalledTime = pi.firstInstallTime
    val lastUpdatedTime = pi.lastUpdateTime
    item.contentView.apply {
      if (firstInstalledTime <= PREINSTALLED_TIMESTAMP) {
        firstInstalledView.libSize.text = getString(R.string.snapshot_preinstalled_app)
      } else {
        firstInstalledView.libSize.text =
          SimpleDateFormat.getDateTimeInstance().format(firstInstalledTime)
      }
      if (lastUpdatedTime <= PREINSTALLED_TIMESTAMP) {
        lastUpdatedView.libSize.text = getString(R.string.snapshot_preinstalled_app)
      } else {
        lastUpdatedView.libSize.text =
          SimpleDateFormat.getDateTimeInstance().format(lastUpdatedTime)
      }
      (parent as? View)?.setLongClickCopiedToClipboard(item.contentView.getAllContentText())
    }
  }

  override fun onBinderReceived() {
    initOriginatingItemView(
      root.originatingView,
      PackageUtils.getInstallSourceInfo(packageName!!)!!.originatingPackageName
    )
  }

  override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
    initOriginatingItemView(
      root.originatingView,
      PackageUtils.getInstallSourceInfo(packageName!!)!!.originatingPackageName
    )
  }
}
