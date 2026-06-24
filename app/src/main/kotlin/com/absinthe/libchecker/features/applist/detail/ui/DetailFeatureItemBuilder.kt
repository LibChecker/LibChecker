package com.absinthe.libchecker.features.applist.detail.ui

import android.content.pm.PackageInfo
import android.graphics.drawable.Drawable
import androidx.core.graphics.toColorInt
import androidx.fragment.app.FragmentActivity
import com.absinthe.libchecker.R
import com.absinthe.libchecker.database.entity.Features
import com.absinthe.libchecker.domain.app.AppIconItem
import com.absinthe.libchecker.domain.app.VersionedFeature
import com.absinthe.libchecker.features.applist.detail.FeaturesDialog
import com.absinthe.libchecker.features.applist.detail.bean.FeatureItem
import com.absinthe.libchecker.features.applist.detail.bean.FeaturePriority
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.apk.ApkPreviewInfo

class DetailFeatureItemBuilder(
  private val activity: FragmentActivity,
  private val packageInfo: () -> PackageInfo,
  private val isApkPreview: () -> Boolean,
  private val apkPreviewInfo: () -> ApkPreviewInfo?,
  private val apkAnalyticsMode: () -> Boolean,
  private val appIcons: () -> List<AppIconItem>,
  private val appIconDrawables: () -> List<Drawable>
) {

  fun build(feature: VersionedFeature, currentFeatureCount: Int): DetailFeatureItem? {
    return when (feature.featureType) {
      Features.SPLIT_APKS -> DetailFeatureItem(
        FeatureItem(R.drawable.ic_aab, R.string.app_bundle) {
          FeaturesDialog.showSplitApksDialog(activity, packageInfo())
        }
      )

      Features.KOTLIN_USED -> DetailFeatureItem(
        FeatureItem(com.absinthe.lc.rulesbundle.R.drawable.ic_lib_kotlin, R.string.kotlin_string) {
          FeaturesDialog.showKotlinDialog(activity, feature.extras)
        }
      )

      Features.RX_JAVA -> DetailFeatureItem(
        FeatureItem(R.drawable.ic_reactivex, R.string.rxjava) {
          FeaturesDialog.showRxJavaDialog(activity, feature.version)
        }
      )

      Features.RX_KOTLIN -> DetailFeatureItem(
        FeatureItem(R.drawable.ic_reactivex, R.string.rxkotlin, colorFilterInt = "#7F52FF".toColorInt()) {
          FeaturesDialog.showRxKotlinDialog(activity, feature.version)
        }
      )

      Features.RX_ANDROID -> DetailFeatureItem(
        FeatureItem(R.drawable.ic_reactivex, R.string.rxandroid, colorFilterInt = "#3DDC84".toColorInt()) {
          FeaturesDialog.showRxAndroidDialog(activity, feature.version)
        }
      )

      Features.AGP -> DetailFeatureItem(
        FeatureItem(R.drawable.ic_gradle, R.string.agp) {
          FeaturesDialog.showAGPDialog(activity, feature.version)
        }
      )

      Features.XPOSED_MODULE -> DetailFeatureItem(
        FeatureItem(R.drawable.ic_xposed, R.string.xposed_module) {
          XposedInfoDialogFragment.newInstance(packageInfo().packageName)
            .show(activity.supportFragmentManager, XposedInfoDialogFragment::class.java.name)
        }
      )

      Features.PLAY_SIGNING -> DetailFeatureItem(
        FeatureItem(com.absinthe.lc.rulesbundle.R.drawable.ic_lib_play_store, R.string.play_app_signing) {
          FeaturesDialog.showPlayAppSigningDialog(activity)
        }
      )

      Features.PWA -> DetailFeatureItem(
        FeatureItem(R.drawable.ic_pwa, R.string.pwa) {
          FeaturesDialog.showPWADialog(activity)
        }
      )

      Features.JETPACK_COMPOSE -> DetailFeatureItem(
        FeatureItem(com.absinthe.lc.rulesbundle.R.drawable.ic_lib_jetpack_compose, R.string.jetpack_compose) {
          FeaturesDialog.showJetpackComposeDialog(activity, feature.version)
        }
      )

      Features.KMP -> DetailFeatureItem(
        FeatureItem(com.absinthe.lc.rulesbundle.R.drawable.ic_lib_jetbrain_kmp, R.string.jetbrain_kmp) {
          FeaturesDialog.showKMPDialog(activity, feature.version)
        }
      )

      Features.LIVE_UPDATE_NOTIFICATION -> DetailFeatureItem(
        FeatureItem(R.drawable.ic_feature_live_update, R.string.feature_live_update_notification) {
          FeaturesDialog.showLiveUpdateNotificationDialog(activity)
        }
      )

      Features.Ext.APPLICATION_PROP -> DetailFeatureItem(
        FeatureItem(R.drawable.ic_app_prop, R.string.lib_detail_app_props_title) {
          val previewInfo = apkPreviewInfo()
          if (isApkPreview() && previewInfo != null) {
            FeaturesDialog.showAppPropDialog(activity, previewInfo.appProps)
          } else {
            FeaturesDialog.showAppPropDialog(activity, packageInfo())
          }
        },
        position = currentFeatureCount.coerceAtMost(FeaturePriority.PRIORITY_APP_PROP)
      )

      Features.Ext.APPLICATION_INSTALL_SOURCE -> buildInstallSourceItem(currentFeatureCount)

      Features.Ext.ELF_PAGE_SIZE_16KB -> DetailFeatureItem(
        FeatureItem(R.drawable.ic_16kb_align, R.string.lib_detail_dialog_title_16kb_page_size) {
          FeaturesDialog.show16KBAlignDialog(activity)
        },
        position = currentFeatureCount.coerceAtMost(FeaturePriority.PRIORITY_16_KB_PAGE_SIZE)
      )

      Features.Ext.ELF_PAGE_SIZE_16KB_COMPAT -> DetailFeatureItem(
        FeatureItem(R.drawable.ic_16kb_compat, R.string.lib_detail_dialog_title_16kb_page_size_compat) {
          FeaturesDialog.show16KBCompatDialog(activity)
        },
        position = currentFeatureCount.coerceAtMost(FeaturePriority.PRIORITY_16_KB_PAGE_SIZE_COMPAT)
      )

      Features.Ext.APPLICATION_ICONS -> buildAppIconsItem()

      else -> null
    }
  }

  private fun buildInstallSourceItem(currentFeatureCount: Int): DetailFeatureItem? {
    if (!OsUtils.atLeastR() || apkAnalyticsMode()) {
      return null
    }

    return DetailFeatureItem(
      FeatureItem(R.drawable.ic_install_source, R.string.lib_detail_app_install_source_title) {
        FeaturesDialog.showAppInstallSourceDialog(activity, packageInfo().packageName)
      },
      position = currentFeatureCount.coerceAtMost(FeaturePriority.PRIORITY_APP_INSTALL_SOURCE)
    )
  }

  private fun buildAppIconsItem(): DetailFeatureItem? {
    if (!OsUtils.atLeastT()) {
      return null
    }

    val appIcons = appIcons()
    if (appIcons.isEmpty()) {
      return null
    }

    val drawables = appIconDrawables()
    if (drawables.isEmpty()) {
      return null
    }

    return DetailFeatureItem(
      FeatureItem(-1, R.string.dialog_themed_and_alternative_app_icons, drawables = drawables) {
        FeaturesDialog.showAppIconsDialog(activity, drawables, appIcons[0].isMonochrome)
      }
    )
  }
}

data class DetailFeatureItem(
  val item: FeatureItem,
  val position: Int? = null
)
