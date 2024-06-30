package com.absinthe.libchecker.features.applist.detail

import android.content.Context
import android.content.pm.PackageInfo
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.core.text.HtmlCompat
import androidx.fragment.app.FragmentActivity
import com.absinthe.libchecker.R
import com.absinthe.libchecker.features.applist.detail.ui.AppBundleBottomSheetDialogFragment
import com.absinthe.libchecker.features.applist.detail.ui.AppInstallSourceBSDFragment
import com.absinthe.libchecker.features.applist.detail.ui.AppPropBottomSheetDialogFragment
import com.absinthe.libchecker.features.applist.detail.ui.EXTRA_PACKAGE_INFO
import com.absinthe.libchecker.features.applist.detail.ui.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.ui.base.BaseAlertDialogBuilder
import com.absinthe.libchecker.utils.UiUtils

object FeaturesDialog {

  fun showSplitApksDialog(activity: FragmentActivity, packageInfo: PackageInfo) {
    AppBundleBottomSheetDialogFragment().apply {
      arguments = bundleOf(
        EXTRA_PACKAGE_INFO to packageInfo
      )
      show(activity.supportFragmentManager, AppBundleBottomSheetDialogFragment::class.java.name)
    }
  }

  fun showKotlinDialog(context: Context, extras: Map<String, String?>?) {
    commonShowDialogImpl(
      context,
      com.absinthe.lc.rulesbundle.R.drawable.ic_lib_kotlin,
      R.string.kotlin_string,
      R.string.kotlin_details,
      extras
    )
  }

  fun showRxJavaDialog(context: Context, version: String?) {
    commonShowDialogImpl(
      context,
      R.drawable.ic_reactivex,
      R.string.rxjava,
      R.string.rx_detail,
      version
    )
  }

  fun showRxKotlinDialog(context: Context, version: String?) {
    val drawable = UiUtils.changeDrawableColor(context, R.drawable.ic_reactivex, Color.parseColor("#7F52FF"))
    commonShowDialogImpl(
      context,
      drawable,
      R.string.rxkotlin,
      R.string.rx_kotlin_detail,
      version
    )
  }

  fun showRxAndroidDialog(context: Context, version: String?) {
    val drawable = UiUtils.changeDrawableColor(context, R.drawable.ic_reactivex, Color.parseColor("#3DDC84"))
    commonShowDialogImpl(
      context,
      drawable,
      R.string.rxandroid,
      R.string.rx_android_detail,
      version
    )
  }

  fun showAGPDialog(context: Context, version: String?) {
    commonShowDialogImpl(context, R.drawable.ic_gradle, R.string.agp, R.string.agp_details, version)
  }

  fun showXPosedDialog(context: Context) {
    commonShowDialogImpl(
      context,
      R.drawable.ic_xposed,
      R.string.xposed_module,
      R.string.xposed_module_details,
      version = null
    )
  }

  fun showPlayAppSigningDialog(context: Context) {
    commonShowDialogImpl(
      context,
      com.absinthe.lc.rulesbundle.R.drawable.ic_lib_play_store,
      R.string.play_app_signing,
      R.string.play_app_signing_details,
      version = null
    )
  }

  fun showPWADialog(context: Context) {
    commonShowDialogImpl(
      context,
      R.drawable.ic_pwa,
      R.string.pwa,
      R.string.pwa_details,
      version = null
    )
  }

  fun showJetpackComposeDialog(context: Context, version: String?) {
    commonShowDialogImpl(
      context,
      com.absinthe.lc.rulesbundle.R.drawable.ic_lib_jetpack_compose,
      R.string.jetpack_compose,
      R.string.jetpack_compose_details,
      version
    )
  }

  fun showAppPropDialog(activity: FragmentActivity, packageInfo: PackageInfo?) {
    val pi = packageInfo ?: return

    AppPropBottomSheetDialogFragment().apply {
      arguments = bundleOf(
        EXTRA_PACKAGE_INFO to pi
      )
      show(activity.supportFragmentManager, AppPropBottomSheetDialogFragment::class.java.name)
    }
  }

  @RequiresApi(Build.VERSION_CODES.R)
  fun showAppInstallSourceDialog(activity: FragmentActivity, packageName: String) {
    AppInstallSourceBSDFragment().apply {
      arguments = bundleOf(
        EXTRA_PACKAGE_NAME to packageName
      )
      show(activity.supportFragmentManager, AppInstallSourceBSDFragment::class.java.name)
    }
  }

  private fun commonShowDialogImpl(
    context: Context,
    icon: Drawable,
    @StringRes titleRes: Int,
    @StringRes messageRes: Int,
    version: String?
  ) {
    commonShowDialogImpl(context, icon, titleRes, messageRes, mapOf(context.getString(titleRes) to version))
  }

  private fun commonShowDialogImpl(
    context: Context,
    @DrawableRes iconRes: Int,
    @StringRes titleRes: Int,
    @StringRes messageRes: Int,
    version: String?
  ) {
    commonShowDialogImpl(context, context.getDrawable(iconRes)!!, titleRes, messageRes, version)
  }

  private fun commonShowDialogImpl(
    context: Context,
    @DrawableRes iconRes: Int,
    @StringRes titleRes: Int,
    @StringRes messageRes: Int,
    versionInfo: Map<String, String?>?
  ) {
    commonShowDialogImpl(context, context.getDrawable(iconRes)!!, titleRes, messageRes, versionInfo)
  }

  private fun commonShowDialogImpl(
    context: Context,
    icon: Drawable,
    @StringRes titleRes: Int,
    @StringRes messageRes: Int,
    versionInfo: Map<String, String?>?
  ) {
    val dialog = BaseAlertDialogBuilder(context)
      .setIcon(icon)
      .setTitle(titleRes)
      .setMessage(messageRes)
      .setPositiveButton(android.R.string.ok, null)

    versionInfo?.let { info ->
      val title = info.map { "${it.key} <b>${it.value.orEmpty()}</b>" }.joinToString(", ")
      dialog.setTitle(HtmlCompat.fromHtml(title, HtmlCompat.FROM_HTML_MODE_COMPACT))
    } ?: run {
      dialog.setTitle(titleRes)
    }

    dialog.show()
  }
}
