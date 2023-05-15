package com.absinthe.libchecker.ui.fragment.detail

import android.content.pm.PackageInfo
import android.os.Build
import android.text.SpannableString
import android.text.style.ImageSpan
import android.view.View
import android.widget.TextView
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
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.getDrawable
import com.absinthe.libchecker.utils.extensions.launchDetailPage
import com.absinthe.libchecker.view.applist.AppItemView
import com.absinthe.libchecker.view.detail.AppInstallSourceBottomSheetView
import com.absinthe.libchecker.view.detail.CenterAlignImageSpan
import com.absinthe.libraries.utils.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import kotlinx.coroutines.runBlocking

class AppInstallSourceBSDFragment :
  BaseBottomSheetViewDialogFragment<AppInstallSourceBottomSheetView>() {

  private val packageName by lazy { arguments?.getString(EXTRA_PACKAGE_NAME) }

  override fun initRootView(): AppInstallSourceBottomSheetView =
    AppInstallSourceBottomSheetView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    if (!OsUtils.atLeastR()) {
      return
    }
    val packageName = packageName ?: return
    val info = PackageUtils.getInstallSourceInfo(packageName) ?: return

    initAppItemContainerView(root.initiatingTitleView, root.initiatingPackageView.container, info.originatingPackageName)
    initAppItemContainerView(root.installingTitleView, root.installingPackageView.container, info.installingPackageName)
  }

  private fun initAppItemContainerView(
    title: TextView,
    view: AppItemView.AppItemContainerView,
    packageName: String?
  ) {
    if (packageName == null || context == null) {
      title.isGone = true
      (view.parent as View).isGone = true
      return
    }
    val targetLCItem = runBlocking { Repositories.lcRepository.getItem(packageName) } ?: return

    val pi = runCatching {
      PackageUtils.getPackageInfo(packageName)
    }.getOrNull()
    view.icon.load(pi)
    view.appName.text = targetLCItem.label
    view.packageName.text = packageName
    view.versionInfo.text =
      PackageUtils.getVersionString(targetLCItem.versionName, targetLCItem.versionCode)

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
      view.abiInfo.text = spanString
    } else {
      view.abiInfo.text = str
    }

    if (targetLCItem.variant == Constants.VARIANT_HAP) {
      view.setBadge(R.drawable.ic_harmony_badge)
    } else {
      view.setBadge(null)
    }

    (view.parent as View).setOnClickListener {
      activity?.finish()
      dismiss()
      activity?.launchDetailPage(targetLCItem)
    }
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
}
