package com.absinthe.libchecker.domain.app.detail.feature

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.absinthe.libchecker.R
import com.absinthe.libchecker.database.entity.Features
import com.absinthe.libchecker.domain.app.detail.model.AppIconItem
import com.absinthe.libchecker.domain.app.model.VersionedFeature

class BuildAppDetailFeatureItemUseCase {

  operator fun invoke(request: AppDetailFeatureItemRequest): AppDetailFeatureItemData? {
    val feature = request.feature
    return when (feature.featureType) {
      Features.SPLIT_APKS -> AppDetailFeatureItemData(
        icon = resourceIcon(R.drawable.ic_aab),
        titleRes = R.string.app_bundle,
        action = AppDetailFeatureAction.SplitApks
      )

      Features.KOTLIN_USED -> AppDetailFeatureItemData(
        icon = resourceIcon(com.absinthe.lc.rulesbundle.R.drawable.ic_lib_kotlin),
        titleRes = R.string.kotlin_string,
        action = AppDetailFeatureAction.Kotlin(feature.extras)
      )

      Features.RX_JAVA -> AppDetailFeatureItemData(
        icon = resourceIcon(R.drawable.ic_reactivex),
        titleRes = R.string.rxjava,
        action = AppDetailFeatureAction.RxJava(feature.version)
      )

      Features.RX_KOTLIN -> AppDetailFeatureItemData(
        icon = resourceIcon(R.drawable.ic_reactivex, RX_KOTLIN_COLOR),
        titleRes = R.string.rxkotlin,
        action = AppDetailFeatureAction.RxKotlin(feature.version)
      )

      Features.RX_ANDROID -> AppDetailFeatureItemData(
        icon = resourceIcon(R.drawable.ic_reactivex, RX_ANDROID_COLOR),
        titleRes = R.string.rxandroid,
        action = AppDetailFeatureAction.RxAndroid(feature.version)
      )

      Features.AGP -> AppDetailFeatureItemData(
        icon = resourceIcon(R.drawable.ic_gradle),
        titleRes = R.string.agp,
        action = AppDetailFeatureAction.Agp(feature.version)
      )

      Features.XPOSED_MODULE -> AppDetailFeatureItemData(
        icon = resourceIcon(R.drawable.ic_xposed),
        titleRes = R.string.xposed_module,
        action = AppDetailFeatureAction.XposedModule
      )

      Features.PLAY_SIGNING -> AppDetailFeatureItemData(
        icon = resourceIcon(com.absinthe.lc.rulesbundle.R.drawable.ic_lib_play_store),
        titleRes = R.string.play_app_signing,
        action = AppDetailFeatureAction.PlaySigning
      )

      Features.PWA -> AppDetailFeatureItemData(
        icon = resourceIcon(R.drawable.ic_pwa),
        titleRes = R.string.pwa,
        action = AppDetailFeatureAction.Pwa
      )

      Features.JETPACK_COMPOSE -> AppDetailFeatureItemData(
        icon = resourceIcon(com.absinthe.lc.rulesbundle.R.drawable.ic_lib_jetpack_compose),
        titleRes = R.string.jetpack_compose,
        action = AppDetailFeatureAction.JetpackCompose(feature.version)
      )

      Features.KMP -> AppDetailFeatureItemData(
        icon = resourceIcon(com.absinthe.lc.rulesbundle.R.drawable.ic_lib_jetbrain_kmp),
        titleRes = R.string.jetbrain_kmp,
        action = AppDetailFeatureAction.Kmp(feature.version)
      )

      Features.LIVE_UPDATE_NOTIFICATION -> AppDetailFeatureItemData(
        icon = resourceIcon(R.drawable.ic_feature_live_update),
        titleRes = R.string.feature_live_update_notification,
        action = AppDetailFeatureAction.LiveUpdateNotification
      )

      Features.Ext.APPLICATION_PROP -> AppDetailFeatureItemData(
        icon = resourceIcon(R.drawable.ic_app_prop),
        titleRes = R.string.lib_detail_app_props_title,
        action = AppDetailFeatureAction.AppProp,
        position = request.currentFeatureCount.coerceAtMost(PRIORITY_APP_PROP)
      )

      Features.Ext.APPLICATION_INSTALL_SOURCE -> buildInstallSourceItem(request)

      Features.Ext.ELF_PAGE_SIZE_16KB -> AppDetailFeatureItemData(
        icon = resourceIcon(R.drawable.ic_16kb_align),
        titleRes = R.string.lib_detail_dialog_title_16kb_page_size,
        action = AppDetailFeatureAction.ElfPageSize16Kb,
        position = request.currentFeatureCount.coerceAtMost(PRIORITY_16_KB_PAGE_SIZE)
      )

      Features.Ext.ELF_PAGE_SIZE_16KB_COMPAT -> AppDetailFeatureItemData(
        icon = resourceIcon(R.drawable.ic_16kb_compat),
        titleRes = R.string.lib_detail_dialog_title_16kb_page_size_compat,
        action = AppDetailFeatureAction.ElfPageSize16KbCompat,
        position = request.currentFeatureCount.coerceAtMost(PRIORITY_16_KB_PAGE_SIZE_COMPAT)
      )

      Features.Ext.APPLICATION_ICONS -> buildAppIconsItem(request.appIcons, request.canShowAppIcons)

      else -> null
    }
  }

  private fun buildInstallSourceItem(request: AppDetailFeatureItemRequest): AppDetailFeatureItemData? {
    if (!request.canShowInstallSource || request.apkAnalyticsMode) {
      return null
    }

    return AppDetailFeatureItemData(
      icon = resourceIcon(R.drawable.ic_install_source),
      titleRes = R.string.lib_detail_app_install_source_title,
      action = AppDetailFeatureAction.InstallSource,
      position = request.currentFeatureCount.coerceAtMost(PRIORITY_APP_INSTALL_SOURCE)
    )
  }

  private fun buildAppIconsItem(
    appIcons: List<AppIconItem>,
    canShowAppIcons: Boolean
  ): AppDetailFeatureItemData? {
    if (!canShowAppIcons || appIcons.isEmpty()) {
      return null
    }

    return AppDetailFeatureItemData(
      icon = AppDetailFeatureIcon.AppIcons,
      titleRes = R.string.dialog_themed_and_alternative_app_icons,
      action = AppDetailFeatureAction.AppIcons(isFirstMonochrome = appIcons[0].isMonochrome)
    )
  }

  private companion object {
    const val PRIORITY_APP_PROP = 0
    const val PRIORITY_APP_INSTALL_SOURCE = 1
    const val PRIORITY_16_KB_PAGE_SIZE = 2
    const val PRIORITY_16_KB_PAGE_SIZE_COMPAT = 3
    val RX_KOTLIN_COLOR = 0xFF7F52FF.toInt()
    val RX_ANDROID_COLOR = 0xFF3DDC84.toInt()
  }
}

data class AppDetailFeatureItemRequest(
  val feature: VersionedFeature,
  val currentFeatureCount: Int,
  val apkAnalyticsMode: Boolean,
  val canShowInstallSource: Boolean,
  val canShowAppIcons: Boolean,
  val appIcons: List<AppIconItem>
)

data class AppDetailFeatureItemData(
  val icon: AppDetailFeatureIcon,
  @StringRes val titleRes: Int,
  val action: AppDetailFeatureAction,
  val position: Int? = null
)

sealed interface AppDetailFeatureIcon {
  data class Resource(
    @DrawableRes val res: Int,
    @ColorInt val tint: Int? = null
  ) : AppDetailFeatureIcon

  data object AppIcons : AppDetailFeatureIcon
}

sealed interface AppDetailFeatureAction {
  sealed interface Dialog : AppDetailFeatureAction

  data object SplitApks : AppDetailFeatureAction
  data class Kotlin(val extras: Map<String, String?>?) : Dialog
  data class RxJava(val version: String?) : Dialog
  data class RxKotlin(val version: String?) : Dialog
  data class RxAndroid(val version: String?) : Dialog
  data class Agp(val version: String?) : Dialog
  data object XposedModule : AppDetailFeatureAction
  data object PlaySigning : Dialog
  data object Pwa : Dialog
  data class JetpackCompose(val version: String?) : Dialog
  data class Kmp(val version: String?) : Dialog
  data object LiveUpdateNotification : Dialog
  data object AppProp : AppDetailFeatureAction
  data object InstallSource : AppDetailFeatureAction
  data object ElfPageSize16Kb : Dialog
  data object ElfPageSize16KbCompat : Dialog
  data class AppIcons(val isFirstMonochrome: Boolean) : AppDetailFeatureAction
}

private fun resourceIcon(
  @DrawableRes res: Int,
  @ColorInt tint: Int? = null
): AppDetailFeatureIcon.Resource {
  return AppDetailFeatureIcon.Resource(res, tint)
}
