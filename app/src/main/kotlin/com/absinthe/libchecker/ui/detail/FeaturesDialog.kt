package com.absinthe.libchecker.ui.detail

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.core.text.HtmlCompat
import androidx.fragment.app.FragmentActivity
import com.absinthe.libchecker.R
import com.absinthe.libchecker.ui.base.BaseAlertDialogBuilder
import com.absinthe.libchecker.ui.fragment.detail.AppBundleBottomSheetDialogFragment

object FeaturesDialog {

  fun showSplitApksDialog(activity: FragmentActivity, packageName: String) {
    AppBundleBottomSheetDialogFragment().apply {
      arguments = bundleOf(
        EXTRA_PACKAGE_NAME to packageName
      )
      show(activity.supportFragmentManager, AppBundleBottomSheetDialogFragment::class.java.name)
    }
  }

  fun showKotlinDialog(context: Context, version: String?) {
    commonShowDialogImpl(
      context,
      R.drawable.ic_kotlin_logo,
      R.string.kotlin_string,
      R.string.kotlin_details,
      version
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
    commonShowDialogImpl(
      context,
      R.drawable.ic_rxkotlin,
      R.string.rxkotlin,
      R.string.rx_kotlin_detail,
      version
    )
  }

  fun showRxAndroidDialog(context: Context, version: String?) {
    commonShowDialogImpl(
      context,
      R.drawable.ic_rxandroid,
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
      null
    )
  }

  fun showPlayAppSigningDialog(context: Context) {
    commonShowDialogImpl(
      context,
      com.absinthe.lc.rulesbundle.R.drawable.ic_lib_play_store,
      R.string.play_app_signing,
      R.string.play_app_signing_details,
      null
    )
  }

  fun showPWADialog(context: Context) {
    commonShowDialogImpl(
      context,
      R.drawable.ic_pwa,
      R.string.pwa,
      R.string.pwa_details,
      null
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

  private fun commonShowDialogImpl(
    context: Context,
    icon: Drawable,
    @StringRes titleRes: Int,
    @StringRes messageRes: Int,
    version: String?
  ) {
    val dialog = BaseAlertDialogBuilder(context)
      .setIcon(icon)
      .setTitle(titleRes)
      .setMessage(messageRes)
      .setPositiveButton(android.R.string.ok, null)

    version?.let {
      dialog.setTitle(
        HtmlCompat.fromHtml(
          "${context.getString(titleRes)} <b>$it</b>",
          HtmlCompat.FROM_HTML_MODE_COMPACT
        )
      )
    } ?: run {
      dialog.setTitle(titleRes)
    }

    dialog.show()
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
}
