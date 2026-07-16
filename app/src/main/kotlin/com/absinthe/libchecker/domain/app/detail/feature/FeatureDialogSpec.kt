package com.absinthe.libchecker.domain.app.detail.feature

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.URLManager.ANDROID_DEV_HOST

data class FeatureDialogSpec(
  @DrawableRes val iconRes: Int,
  @StringRes val titleRes: Int,
  @StringRes val messageRes: Int,
  @ColorInt val iconTint: Int? = null,
  val titleEntries: List<FeatureDialogTitleEntry>? = null,
  val sourceUrl: String? = null
)

data class FeatureDialogTitleEntry(
  val label: FeatureDialogTitleLabel,
  val value: String?
)

sealed interface FeatureDialogTitleLabel {
  data class Resource(
    @StringRes val res: Int
  ) : FeatureDialogTitleLabel

  data class Text(
    val value: String
  ) : FeatureDialogTitleLabel
}

fun AppDetailFeatureAction.Dialog.toDialogSpec(): FeatureDialogSpec {
  return when (this) {
    is AppDetailFeatureAction.Kotlin -> FeatureDialogSpec(
      iconRes = com.absinthe.lc.rulesbundle.R.drawable.ic_lib_kotlin,
      titleRes = R.string.kotlin_string,
      messageRes = R.string.kotlin_details,
      titleEntries = extras?.map { (label, version) ->
        FeatureDialogTitleEntry(FeatureDialogTitleLabel.Text(label), version)
      },
      sourceUrl = "https://kotlinlang.org/"
    )

    is AppDetailFeatureAction.RxJava -> versionDialogSpec(
      iconRes = R.drawable.ic_reactivex,
      titleRes = R.string.rxjava,
      messageRes = R.string.rx_detail,
      version = version,
      sourceUrl = "https://reactivex.io/"
    )

    is AppDetailFeatureAction.RxKotlin -> versionDialogSpec(
      iconRes = R.drawable.ic_reactivex,
      titleRes = R.string.rxkotlin,
      messageRes = R.string.rx_kotlin_detail,
      version = version,
      iconTint = RX_KOTLIN_COLOR,
      sourceUrl = "https://github.com/ReactiveX/RxKotlin"
    )

    is AppDetailFeatureAction.RxAndroid -> versionDialogSpec(
      iconRes = R.drawable.ic_reactivex,
      titleRes = R.string.rxandroid,
      messageRes = R.string.rx_android_detail,
      version = version,
      iconTint = RX_ANDROID_COLOR,
      sourceUrl = "https://github.com/ReactiveX/RxAndroid"
    )

    is AppDetailFeatureAction.Agp -> versionDialogSpec(
      iconRes = R.drawable.ic_gradle,
      titleRes = R.string.agp,
      messageRes = R.string.agp_details,
      version = version,
      sourceUrl = "https://developer.$ANDROID_DEV_HOST/build/gradle-build-overview"
    )

    AppDetailFeatureAction.PlaySigning -> versionDialogSpec(
      iconRes = com.absinthe.lc.rulesbundle.R.drawable.ic_lib_play_store,
      titleRes = R.string.play_app_signing,
      messageRes = R.string.play_app_signing_details,
      version = null,
      sourceUrl = "https://developer.$ANDROID_DEV_HOST/studio/publish/app-signing#enroll"
    )

    AppDetailFeatureAction.Pwa -> versionDialogSpec(
      iconRes = R.drawable.ic_pwa,
      titleRes = R.string.pwa,
      messageRes = R.string.pwa_details,
      version = null
    )

    is AppDetailFeatureAction.JetpackCompose -> versionDialogSpec(
      iconRes = com.absinthe.lc.rulesbundle.R.drawable.ic_lib_jetpack_compose,
      titleRes = R.string.jetpack_compose,
      messageRes = R.string.jetpack_compose_details,
      version = version,
      sourceUrl = "https://developer.$ANDROID_DEV_HOST/compose"
    )

    is AppDetailFeatureAction.Kmp -> versionDialogSpec(
      iconRes = com.absinthe.lc.rulesbundle.R.drawable.ic_lib_jetbrain_kmp,
      titleRes = R.string.jetbrain_kmp,
      messageRes = R.string.jetbrain_compose_multiplatform_details,
      version = version,
      sourceUrl = "https://www.jetbrains.com/compose-multiplatform/"
    )

    AppDetailFeatureAction.LiveUpdateNotification -> versionDialogSpec(
      iconRes = R.drawable.ic_feature_live_update,
      titleRes = R.string.feature_live_update_notification,
      messageRes = R.string.feature_live_update_notification_summary,
      version = null,
      sourceUrl = "https://developer.$ANDROID_DEV_HOST/develop/ui/views/notifications/live-update"
    )

    AppDetailFeatureAction.ElfPageSize16Kb -> versionDialogSpec(
      iconRes = R.drawable.ic_16kb_align,
      titleRes = R.string.lib_detail_dialog_title_16kb_page_size,
      messageRes = R.string.lib_detail_dialog_content_16kb_page_size,
      version = null,
      sourceUrl = "https://developer.$ANDROID_DEV_HOST/guide/practices/page-sizes"
    )

    AppDetailFeatureAction.ElfPageSize16KbCompat -> versionDialogSpec(
      iconRes = R.drawable.ic_16kb_compat,
      titleRes = R.string.lib_detail_dialog_title_16kb_page_size_compat,
      messageRes = R.string.lib_detail_dialog_content_16kb_page_size_compat,
      version = null,
      sourceUrl = "https://source.$ANDROID_DEV_HOST/docs/core/architecture/16kb-page-size/16kb-backcompat-option"
    )
  }
}

fun xposedFeatureDialogSpec(): FeatureDialogSpec {
  return versionDialogSpec(
    iconRes = R.drawable.ic_xposed,
    titleRes = R.string.xposed_module,
    messageRes = R.string.xposed_module_details,
    version = null
  )
}

fun multiArchFeatureDialogSpec(): FeatureDialogSpec {
  return versionDialogSpec(
    iconRes = R.drawable.ic_abi_label_multi_arch,
    titleRes = R.string.multiArch,
    messageRes = R.string.multi_arch_dialog_details,
    version = null,
    sourceUrl = "https://source.$ANDROID_DEV_HOST/docs/setup/create/64-bit-builds"
  )
}

private fun versionDialogSpec(
  @DrawableRes iconRes: Int,
  @StringRes titleRes: Int,
  @StringRes messageRes: Int,
  version: String?,
  @ColorInt iconTint: Int? = null,
  sourceUrl: String? = null
): FeatureDialogSpec {
  return FeatureDialogSpec(
    iconRes = iconRes,
    titleRes = titleRes,
    messageRes = messageRes,
    iconTint = iconTint,
    titleEntries = listOf(
      FeatureDialogTitleEntry(
        label = FeatureDialogTitleLabel.Resource(titleRes),
        value = version
      )
    ),
    sourceUrl = sourceUrl
  )
}

private const val RX_KOTLIN_COLOR = 0xFF7F52FF.toInt()
private const val RX_ANDROID_COLOR = 0xFF3DDC84.toInt()
