package com.absinthe.libchecker.features.chart

import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.GlobalFeatures

enum class ChartType(
  val iconRes: Int,
  val titleRes: Int,
  val requiresFeatureInitialization: Boolean = false
) {
  ABI(
    iconRes = R.drawable.ic_logo,
    titleRes = R.string.abi_string
  ),
  KOTLIN(
    iconRes = com.absinthe.lc.rulesbundle.R.drawable.ic_lib_kotlin,
    titleRes = R.string.kotlin_string,
    requiresFeatureInitialization = true
  ),
  TARGET_SDK(
    iconRes = R.drawable.ic_label_target_sdk,
    titleRes = R.string.target_sdk_string
  ),
  MIN_SDK(
    iconRes = R.drawable.ic_label_min_sdk,
    titleRes = R.string.min_sdk_string
  ),
  COMPILE_SDK(
    iconRes = R.drawable.ic_label_compile_sdk,
    titleRes = R.string.compile_sdk_string
  ),
  JETPACK_COMPOSE(
    iconRes = com.absinthe.lc.rulesbundle.R.drawable.ic_lib_jetpack_compose,
    titleRes = R.string.jetpack_compose_short,
    requiresFeatureInitialization = true
  ),
  MARKET_DISTRIBUTION(
    iconRes = com.absinthe.lc.rulesbundle.R.drawable.ic_lib_android,
    titleRes = R.string.android_dist_label
  ),
  AAB(
    iconRes = R.drawable.ic_aab,
    titleRes = R.string.app_bundle
  ),
  SUPPORT_16KB(
    iconRes = R.drawable.ic_16kb_align,
    titleRes = R.string.lib_detail_dialog_title_16kb_page_size
  );

  companion object {

    fun availableTypes(): List<ChartType> {
      return values().filter {
        it != SUPPORT_16KB || GlobalFeatures.ENABLE_DETECTING_16KB_PAGE_ALIGNMENT
      }
    }
  }
}
