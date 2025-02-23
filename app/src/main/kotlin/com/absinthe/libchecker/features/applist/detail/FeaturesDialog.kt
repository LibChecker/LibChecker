package com.absinthe.libchecker.features.applist.detail

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
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
import com.absinthe.libchecker.utils.Telemetry
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.utils.UiUtils
import timber.log.Timber

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
      versionInfo = extras,
      sourceLink = "https://kotlinlang.org/"
    )
  }

  fun showRxJavaDialog(context: Context, version: String?) {
    commonShowDialogImpl(
      context,
      R.drawable.ic_reactivex,
      R.string.rxjava,
      R.string.rx_detail,
      version = version,
      sourceLink = "https://reactivex.io/"
    )
  }

  fun showRxKotlinDialog(context: Context, version: String?) {
    val drawable = UiUtils.changeDrawableColor(context, R.drawable.ic_reactivex, "#7F52FF".toColorInt())
    commonShowDialogImpl(
      context,
      drawable,
      R.string.rxkotlin,
      R.string.rx_kotlin_detail,
      version = version,
      sourceLink = "https://github.com/ReactiveX/RxKotlin"
    )
  }

  fun showRxAndroidDialog(context: Context, version: String?) {
    val drawable = UiUtils.changeDrawableColor(context, R.drawable.ic_reactivex, "#3DDC84".toColorInt())
    commonShowDialogImpl(
      context,
      drawable,
      R.string.rxandroid,
      R.string.rx_android_detail,
      version = version,
      sourceLink = "https://github.com/ReactiveX/RxAndroid"
    )
  }

  fun showAGPDialog(context: Context, version: String?) {
    commonShowDialogImpl(
      context,
      R.drawable.ic_gradle,
      R.string.agp,
      R.string.agp_details,
      version = version,
      sourceLink = "https://developer.android.com/build/gradle-build-overview"
    )
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
      version = null,
      sourceLink = "https://developer.android.com/studio/publish/app-signing#enroll"
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
      version = version,
      sourceLink = "https://developer.android.com/compose"
    )
  }

  fun showKMPDialog(context: Context, version: String?) {
    commonShowDialogImpl(
      context,
      R.drawable.ic_jetbrain_kmp,
      R.string.jetbrain_kmp,
      R.string.jetbrain_compose_multiplatform_details,
      version = version,
      sourceLink = "https://www.jetbrains.com/compose-multiplatform/"
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

  fun show16KBAlignDialog(context: Context) {
    commonShowDialogImpl(
      context,
      R.drawable.ic_16kb_align,
      R.string.lib_detail_dialog_title_16kb_page_size,
      R.string.lib_detail_dialog_content_16kb_page_size,
      version = null,
      sourceLink = "https://developer.android.com/guide/practices/page-sizes"
    )
  }

  fun show16KBCompatDialog(context: Context) {
    commonShowDialogImpl(
      context,
      R.drawable.ic_16kb_compat,
      R.string.lib_detail_dialog_title_16kb_page_size_compat,
      R.string.lib_detail_dialog_content_16kb_page_size_compat,
      version = null,
      sourceLink = "https://source.android.com/docs/core/architecture/16kb-page-size/16kb-backcompat-option"
    )
  }

  fun showMultiArchDialog(context: Context) {
    commonShowDialogImpl(
      context,
      R.drawable.ic_abi_label_multi_arch,
      R.string.multiArch,
      R.string.multi_arch_dialog_details,
      version = null,
      sourceLink = "https://source.android.com/docs/setup/create/64-bit-builds"
    )
  }

  private fun commonShowDialogImpl(
    context: Context,
    icon: Drawable,
    @StringRes titleRes: Int,
    @StringRes messageRes: Int,
    version: String?,
    sourceLink: String? = null
  ) {
    commonShowDialogImpl(context, icon, titleRes, messageRes, mapOf(context.getString(titleRes) to version), sourceLink)
  }

  private fun commonShowDialogImpl(
    context: Context,
    @DrawableRes iconRes: Int,
    @StringRes titleRes: Int,
    @StringRes messageRes: Int,
    version: String?,
    sourceLink: String? = null
  ) {
    commonShowDialogImpl(context, context.getDrawable(iconRes)!!, titleRes, messageRes, version, sourceLink)
  }

  private fun commonShowDialogImpl(
    context: Context,
    @DrawableRes iconRes: Int,
    @StringRes titleRes: Int,
    @StringRes messageRes: Int,
    versionInfo: Map<String, String?>?,
    sourceLink: String?
  ) {
    commonShowDialogImpl(context, context.getDrawable(iconRes)!!, titleRes, messageRes, versionInfo, sourceLink)
  }

  private fun commonShowDialogImpl(
    context: Context,
    icon: Drawable,
    @StringRes titleRes: Int,
    @StringRes messageRes: Int,
    versionInfo: Map<String, String?>?,
    sourceLink: String?
  ) {
    val dialog = BaseAlertDialogBuilder(context)
      .setIcon(icon)
      .setTitle(titleRes)
      .setMessage(HtmlCompat.fromHtml(context.getString(messageRes), HtmlCompat.FROM_HTML_MODE_COMPACT))
      .setPositiveButton(android.R.string.ok, null)

    versionInfo?.let { info ->
      val title = info.map { "${it.key} <b>${it.value.orEmpty()}</b>" }.joinToString(", ")
      dialog.setTitle(HtmlCompat.fromHtml(title, HtmlCompat.FROM_HTML_MODE_COMPACT))
    } ?: run {
      dialog.setTitle(titleRes)
    }

    sourceLink?.let { link ->
      dialog.setNeutralButton(R.string.lib_detail_app_props_tip) { _, _ ->
        runCatching {
          CustomTabsIntent.Builder().build().apply {
            launchUrl(context, link.toUri())
          }
        }.onFailure {
          Timber.e(it)
          runCatching {
            val intent = Intent(Intent.ACTION_VIEW)
              .setData(link.toUri())
            context.startActivity(intent)
          }.onFailure { inner ->
            Timber.e(inner)
            Toasty.showShort(context, "No browser application")
          }
        }
      }
    }

    dialog.show()
    Telemetry.recordEvent(
      "FeatureDialog",
      mapOf("Feature" to context.getString(titleRes))
    )
  }
}
