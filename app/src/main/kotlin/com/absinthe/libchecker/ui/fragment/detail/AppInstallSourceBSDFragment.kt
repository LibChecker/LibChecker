package com.absinthe.libchecker.ui.fragment.detail

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.text.SpannableString
import android.text.style.ImageSpan
import androidx.annotation.RequiresApi
import androidx.core.text.buildSpannedString
import androidx.core.text.scale
import androidx.core.view.isGone
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.AdvancedOptions
import com.absinthe.libchecker.constant.AndroidVersions
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.ui.detail.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.getDrawable
import com.absinthe.libchecker.utils.extensions.launchDetailPage
import com.absinthe.libchecker.view.detail.AppInstallSourceBottomSheetView
import com.absinthe.libchecker.view.detail.AppInstallSourceItemView
import com.absinthe.libchecker.view.detail.CenterAlignImageSpan
import com.absinthe.libraries.utils.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import kotlinx.coroutines.runBlocking
import rikka.shizuku.Shizuku
import rikka.sui.Sui

@RequiresApi(Build.VERSION_CODES.R)
class AppInstallSourceBSDFragment :
  BaseBottomSheetViewDialogFragment<AppInstallSourceBottomSheetView>(),
  Shizuku.OnBinderReceivedListener,
  Shizuku.OnRequestPermissionResultListener {

  private val packageName by lazy { arguments?.getString(EXTRA_PACKAGE_NAME) }

  override fun initRootView(): AppInstallSourceBottomSheetView =
    AppInstallSourceBottomSheetView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    val packageName = packageName ?: return
    val info = PackageUtils.getInstallSourceInfo(packageName) ?: return

    initOriginatingItemView(root.originatingView, info.originatingPackageName)
    initAppInstallSourceItemView(root.installingView, info.installingPackageName)
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
    item.packageView.container.icon.load(com.absinthe.lc.rulesbundle.R.drawable.ic_lib_shizuku)
    if (!PackageUtils.isAppInstalled(Constants.PackageNames.SHIZUKU) && !Sui.isSui()) {
      item.packageView.container.appName.text =
        getString(R.string.lib_detail_app_install_source_shizuku_uninstalled)
      item.packageView.container.packageName.text =
        getString(R.string.lib_detail_app_install_source_shizuku_usage)
      item.packageView.container.versionInfo.text =
        getString(R.string.lib_detail_app_install_source_shizuku_uninstalled_detail)
      item.packageView.setOnClickListener {
        LCAppUtils.launchMarketPage(requireContext(), Constants.PackageNames.SHIZUKU)
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
            LCAppUtils.launchMarketPage(requireContext(), Constants.PackageNames.SHIZUKU)
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
      .append(getBuildVersionsInfo(pi, packageName))
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

  private fun getBuildVersionsInfo(packageInfo: PackageInfo?, packageName: String): CharSequence {
    if (packageInfo == null && packageName != Constants.EXAMPLE_PACKAGE) {
      return ""
    }
    val showAndroidVersion =
      (GlobalValues.advancedOptions and AdvancedOptions.SHOW_ANDROID_VERSION) > 0
    val showTarget =
      (GlobalValues.advancedOptions and AdvancedOptions.SHOW_TARGET_API) > 0
    val showMin =
      (GlobalValues.advancedOptions and AdvancedOptions.SHOW_MIN_API) > 0
    val target = packageInfo?.applicationInfo?.targetSdkVersion ?: Build.VERSION.SDK_INT
    val min = packageInfo?.applicationInfo?.minSdkVersion ?: Build.VERSION.SDK_INT

    return buildSpannedString {
      if (showTarget) {
        append(", ")
        scale(0.8f) {
          append("Target: ")
        }
        append(target.toString())
        if (showAndroidVersion) {
          append(" (${AndroidVersions.simpleVersions[target]})")
        }
      }

      if (showMin) {
        if (showTarget) {
          append(", ")
        }
        scale(0.8f) {
          append(" Min: ")
        }
        append(min.toString())
        if (showAndroidVersion) {
          append(" (${AndroidVersions.simpleVersions[min]})")
        }
      }
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
