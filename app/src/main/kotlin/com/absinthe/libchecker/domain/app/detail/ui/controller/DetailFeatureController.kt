package com.absinthe.libchecker.domain.app.detail.ui.controller

import android.content.pm.PackageInfo
import android.graphics.drawable.Drawable
import androidx.fragment.app.FragmentActivity
import com.absinthe.libchecker.domain.app.detail.feature.AppDetailFeatureAction
import com.absinthe.libchecker.domain.app.detail.feature.AppDetailFeatureIcon
import com.absinthe.libchecker.domain.app.detail.feature.AppDetailFeatureItemData
import com.absinthe.libchecker.domain.app.detail.feature.toDialogSpec
import com.absinthe.libchecker.domain.app.detail.model.AppIconItem
import com.absinthe.libchecker.domain.app.detail.model.DetailFeatureItem
import com.absinthe.libchecker.domain.app.detail.model.FeatureItem
import com.absinthe.libchecker.domain.app.detail.model.FeatureItemIcon
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel
import com.absinthe.libchecker.domain.app.detail.ui.FeaturesDialog
import com.absinthe.libchecker.domain.app.detail.ui.dialog.XposedInfoDialogFragment
import com.absinthe.libchecker.domain.app.model.VersionedFeature
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.apk.ApkPreviewInfo

class DetailFeatureController(
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

  fun onItemClick(item: FeatureItem) {
    when (val action = item.action) {
      is AppDetailFeatureAction.Dialog -> FeaturesDialog.show(activity, action.toDialogSpec())

      AppDetailFeatureAction.SplitApks -> FeaturesDialog.showSplitApksDialog(activity, packageInfo())

      AppDetailFeatureAction.XposedModule -> XposedInfoDialogFragment.newInstance(packageInfo().packageName)
        .show(activity.supportFragmentManager, XposedInfoDialogFragment::class.java.name)

      AppDetailFeatureAction.AppProp -> showAppPropDialog()

      AppDetailFeatureAction.InstallSource -> if (OsUtils.atLeastR()) {
        FeaturesDialog.showAppInstallSourceDialog(activity, packageInfo().packageName)
      }

      is AppDetailFeatureAction.AppIcons -> (item.icon as? FeatureItemIcon.Drawables)?.let {
        FeaturesDialog.showAppIconsDialog(activity, it.values, action.isFirstMonochrome)
      }
    }
  }

  private fun buildFeatureItem(itemData: AppDetailFeatureItemData): FeatureItem? {
    val icon = when (val icon = itemData.icon) {
      is AppDetailFeatureIcon.Resource -> FeatureItemIcon.Resource(icon.res, icon.tint)

      AppDetailFeatureIcon.AppIcons -> FeatureItemIcon.Drawables(
        appIconDrawables(appIcons()).takeIf { it.isNotEmpty() } ?: return null
      )
    }
    return FeatureItem(
      titleRes = itemData.titleRes,
      icon = icon,
      action = itemData.action
    )
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
