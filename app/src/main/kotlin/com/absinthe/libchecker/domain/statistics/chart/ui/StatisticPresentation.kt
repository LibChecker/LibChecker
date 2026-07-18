package com.absinthe.libchecker.domain.statistics.chart.ui

import android.content.Context
import android.content.res.ColorStateList
import android.widget.ImageView
import androidx.core.widget.ImageViewCompat
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDrawableIcon
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticIconRenderMode
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticIconSpec
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticIconTintRole
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticTitleResource
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticTitleSpec
import com.absinthe.libchecker.utils.extensions.getColorByAttr

internal fun StatisticTitleSpec.resolve(context: Context): String {
  resource?.let { return context.getString(it.stringRes) }
  val locale = context.resources.configuration.locales[0]
  return translations[locale.toLanguageTag()]
    ?: translations[locale.language]
    ?: translations[DEFAULT_TRANSLATION]
    ?: translations.values.firstOrNull()
      .orEmpty()
}

internal fun ImageView.loadStatisticIcon(
  icon: StatisticIconSpec,
  selected: Boolean
) {
  val tint = if (icon.renderMode == StatisticIconRenderMode.MONOCHROME) {
    val useSelectedAccent = selected && icon.asset != null
    val tintColor = if (useSelectedAccent) {
      context.getColorByAttr(androidx.appcompat.R.attr.colorPrimary)
    } else {
      context.getColorByAttr(icon.tintRole.colorAttr)
    }
    ColorStateList.valueOf(tintColor)
  } else {
    null
  }
  icon.drawable?.let { drawable ->
    setImageResource(drawable.drawableRes)
    ImageViewCompat.setImageTintList(this, tint)
    return
  }
  val asset = icon.asset ?: return
  ImageViewCompat.setImageTintList(this, tint)
  load("file:///android_asset/$asset") {
    crossfade(false)
    fallback(R.drawable.ic_chart)
    error(R.drawable.ic_chart)
  }
}

private val StatisticDrawableIcon.drawableRes: Int
  get() = when (this) {
    StatisticDrawableIcon.ABI -> R.drawable.ic_logo
    StatisticDrawableIcon.KOTLIN -> com.absinthe.lc.rulesbundle.R.drawable.ic_lib_kotlin
    StatisticDrawableIcon.TARGET_SDK -> R.drawable.ic_label_target_sdk
    StatisticDrawableIcon.MIN_SDK -> R.drawable.ic_label_min_sdk
    StatisticDrawableIcon.COMPILE_SDK -> R.drawable.ic_label_compile_sdk
    StatisticDrawableIcon.JETPACK_COMPOSE -> com.absinthe.lc.rulesbundle.R.drawable.ic_lib_jetpack_compose
    StatisticDrawableIcon.ANDROID_DISTRIBUTION -> com.absinthe.lc.rulesbundle.R.drawable.ic_lib_android
    StatisticDrawableIcon.APP_BUNDLE -> R.drawable.ic_aab
    StatisticDrawableIcon.PAGE_SIZE_16_KB -> R.drawable.ic_16kb_align
  }

private val StatisticTitleResource.stringRes: Int
  get() = when (this) {
    StatisticTitleResource.ABI -> R.string.abi_string
    StatisticTitleResource.KOTLIN -> R.string.kotlin_string
    StatisticTitleResource.TARGET_SDK -> R.string.target_sdk_string
    StatisticTitleResource.MIN_SDK -> R.string.min_sdk_string
    StatisticTitleResource.COMPILE_SDK -> R.string.compile_sdk_string
    StatisticTitleResource.JETPACK_COMPOSE -> R.string.jetpack_compose_short
    StatisticTitleResource.ANDROID_DISTRIBUTION -> R.string.android_dist_label
    StatisticTitleResource.APP_BUNDLE -> R.string.app_bundle
    StatisticTitleResource.PAGE_SIZE_16_KB -> R.string.lib_detail_dialog_title_16kb_page_size
  }

private val StatisticIconTintRole.colorAttr: Int
  get() = when (this) {
    StatisticIconTintRole.ON_SURFACE -> com.google.android.material.R.attr.colorOnSurface
    StatisticIconTintRole.ON_SURFACE_VARIANT -> com.google.android.material.R.attr.colorOnSurfaceVariant
    StatisticIconTintRole.PRIMARY -> androidx.appcompat.R.attr.colorPrimary
    StatisticIconTintRole.SECONDARY -> com.google.android.material.R.attr.colorSecondary
    StatisticIconTintRole.TERTIARY -> com.google.android.material.R.attr.colorTertiary
  }

private const val DEFAULT_TRANSLATION = "en"
