package com.absinthe.libchecker.features.applist.detail.ui

import android.content.pm.PackageInfo
import android.graphics.drawable.Drawable
import androidx.fragment.app.FragmentActivity
import com.absinthe.libchecker.domain.app.AppIconItem
import com.absinthe.libchecker.domain.app.VersionedFeature
import com.absinthe.libchecker.domain.app.detail.feature.AppDetailFeatureAction
import com.absinthe.libchecker.domain.app.detail.feature.AppDetailFeatureItemData
import com.absinthe.libchecker.domain.app.detail.model.FeatureItem
import com.absinthe.libchecker.domain.app.detail.ui.FeaturesDialog
import com.absinthe.libchecker.features.applist.detail.DetailViewModel
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.apk.ApkPreviewInfo

class DetailFeatureItemBuilder(
  private val activity: FragmentActivity,
  private val viewModel: DetailViewModel,
  private val packageInfo: () -> PackageInfo,
  private val apkPreviewInfo: () -> ApkPreviewInfo?,
  private val apkAnalyticsMode: () -> Boolean,
  private val appIcons: () -> List<AppIconItem>,
  private val appIconDrawables: (List<AppIconItem>) -> List<Drawable>
) {

  fun build(feature: VersionedFeature, currentFeatureCount: Int): DetailFeatureItem? {
    val itemData = viewModel.buildAppDetailFeatureItem(
      feature = feature,
      currentFeatureCount = currentFeatureCount,
      apkAnalyticsMode = apkAnalyticsMode(),
      canShowInstallSource = OsUtils.atLeastR(),
      canShowAppIcons = OsUtils.atLeastT()
    ) ?: return null

    val featureItem = buildFeatureItem(itemData) ?: return null
    return DetailFeatureItem(
      item = featureItem,
      position = itemData.position
    )
  }

  private fun buildFeatureItem(itemData: AppDetailFeatureItemData): FeatureItem? {
    val appIconDrawables = if (itemData.action is AppDetailFeatureAction.AppIcons) {
      appIconDrawables(appIcons()).takeIf { it.isNotEmpty() } ?: return null
    } else {
      null
    }

    return FeatureItem(
      res = itemData.iconRes,
      titleRes = itemData.titleRes,
      drawables = appIconDrawables,
      colorFilterInt = itemData.colorFilterInt,
      action = { performAction(itemData.action, appIconDrawables) }
    )
  }

  private fun performAction(action: AppDetailFeatureAction, appIconDrawables: List<Drawable>?) {
    when (action) {
      AppDetailFeatureAction.SplitApks -> FeaturesDialog.showSplitApksDialog(activity, packageInfo())

      is AppDetailFeatureAction.Kotlin -> FeaturesDialog.showKotlinDialog(activity, action.extras)

      is AppDetailFeatureAction.RxJava -> FeaturesDialog.showRxJavaDialog(activity, action.version)

      is AppDetailFeatureAction.RxKotlin -> FeaturesDialog.showRxKotlinDialog(activity, action.version)

      is AppDetailFeatureAction.RxAndroid -> FeaturesDialog.showRxAndroidDialog(activity, action.version)

      is AppDetailFeatureAction.Agp -> FeaturesDialog.showAGPDialog(activity, action.version)

      AppDetailFeatureAction.XposedModule -> XposedInfoDialogFragment.newInstance(packageInfo().packageName)
        .show(activity.supportFragmentManager, XposedInfoDialogFragment::class.java.name)

      AppDetailFeatureAction.PlaySigning -> FeaturesDialog.showPlayAppSigningDialog(activity)

      AppDetailFeatureAction.Pwa -> FeaturesDialog.showPWADialog(activity)

      is AppDetailFeatureAction.JetpackCompose -> FeaturesDialog.showJetpackComposeDialog(activity, action.version)

      is AppDetailFeatureAction.Kmp -> FeaturesDialog.showKMPDialog(activity, action.version)

      AppDetailFeatureAction.LiveUpdateNotification -> FeaturesDialog.showLiveUpdateNotificationDialog(activity)

      AppDetailFeatureAction.AppProp -> showAppPropDialog()

      AppDetailFeatureAction.InstallSource -> FeaturesDialog.showAppInstallSourceDialog(activity, packageInfo().packageName)

      AppDetailFeatureAction.ElfPageSize16Kb -> FeaturesDialog.show16KBAlignDialog(activity)

      AppDetailFeatureAction.ElfPageSize16KbCompat -> FeaturesDialog.show16KBCompatDialog(activity)

      is AppDetailFeatureAction.AppIcons -> appIconDrawables?.let {
        FeaturesDialog.showAppIconsDialog(activity, it, action.isFirstMonochrome)
      }
    }
  }

  private fun showAppPropDialog() {
    val previewInfo = apkPreviewInfo()
    if (previewInfo != null) {
      FeaturesDialog.showAppPropDialog(activity, previewInfo.appProps)
    } else {
      FeaturesDialog.showAppPropDialog(activity, packageInfo())
    }
  }
}

data class DetailFeatureItem(
  val item: FeatureItem,
  val position: Int? = null
)
