package com.absinthe.libchecker.domain.statistics.chart.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.widget.ImageViewCompat
import coil.Coil
import coil.load
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDrawableIcon
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticFacetSpec
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticIconRenderMode
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticIconSpec
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticIconTintRole
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticTitleResource
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticTitleSpec
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal fun StatisticTitleSpec.resolve(context: Context): String {
  resource?.let { return context.getString(it.stringRes) }
  val locale = context.resources.configuration.locales[0]
  return resolveStatisticTranslation(translations, locale)
}

internal suspend fun StatisticIconSpec.resolveDrawable(context: Context): Drawable? {
  val resolvedDrawable = drawable?.let { ContextCompat.getDrawable(context, it.drawableRes) }
    ?: localPath?.let(::File)?.let { data ->
      withContext(Dispatchers.IO) {
        val request = ImageRequest.Builder(context).data(data).build()
        when (val result = Coil.imageLoader(context).execute(request)) {
          is SuccessResult -> result.drawable
          else -> null
        }
      }
    }
    ?: return null
  return monochromeTintRole?.let { tintRole ->
    DrawableCompat.wrap(resolvedDrawable.mutate()).also { tintedDrawable ->
      DrawableCompat.setTint(tintedDrawable, context.getColorByAttr(tintRole.colorAttr))
    }
  } ?: resolvedDrawable
}

internal val StatisticFacetSpec.summaryTitle: StatisticTitleSpec
  get() = shortTitle ?: title

internal val StatisticIconSpec.monochromeTintRole: StatisticIconTintRole?
  get() = tintRole.takeIf { renderMode == StatisticIconRenderMode.MONOCHROME }

internal fun resolveStatisticTranslation(
  translations: Map<String, String>,
  locale: Locale
): String {
  return translations[locale.toLanguageTag()]
    ?: translations[locale.languageScriptTag()]
    ?: translations[listOf(locale.language, locale.country).filter(String::isNotBlank).joinToString("-")]
    ?: translations[locale.language]
    ?: translations[DEFAULT_TRANSLATION]
    ?: translations.values.firstOrNull()
      .orEmpty()
}

private fun Locale.languageScriptTag(): String {
  val resolvedScript = script.ifBlank {
    if (language != CHINESE_LANGUAGE) return@ifBlank ""
    when (country) {
      CHINA_REGION, SINGAPORE_REGION -> SIMPLIFIED_CHINESE_SCRIPT
      HONG_KONG_REGION, MACAO_REGION, TAIWAN_REGION -> TRADITIONAL_CHINESE_SCRIPT
      else -> ""
    }
  }
  return listOf(language, resolvedScript).filter(String::isNotBlank).joinToString("-")
}

internal fun ImageView.loadStatisticIcon(
  icon: StatisticIconSpec,
  selected: Boolean,
  grayscale: Boolean = false
) {
  val tint = icon.monochromeTintRole?.let { tintRole ->
    val useSelectedAccent = selected && icon.asset != null
    val tintColor = if (useSelectedAccent) {
      context.getColorByAttr(androidx.appcompat.R.attr.colorPrimary)
    } else {
      context.getColorByAttr(tintRole.colorAttr)
    }
    ColorStateList.valueOf(tintColor)
  }
  clearColorFilter()
  if (grayscale && tint == null) {
    colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
  }
  icon.drawable?.let { drawable ->
    setPadding(0, 0, 0, 0)
    setImageResource(drawable.drawableRes)
    ImageViewCompat.setImageTintList(this, tint)
    return
  }
  val imageData = icon.localPath?.let(::File)
    ?: icon.asset?.let { "file:///android_asset/$it" }
    ?: return
  val remoteIconInset = REMOTE_ICON_INSET_DP.dp
  setPadding(remoteIconInset, remoteIconInset, remoteIconInset, remoteIconInset)
  ImageViewCompat.setImageTintList(this, tint)
  load(imageData) {
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
private const val CHINESE_LANGUAGE = "zh"
private const val SIMPLIFIED_CHINESE_SCRIPT = "Hans"
private const val TRADITIONAL_CHINESE_SCRIPT = "Hant"
private const val CHINA_REGION = "CN"
private const val HONG_KONG_REGION = "HK"
private const val MACAO_REGION = "MO"
private const val SINGAPORE_REGION = "SG"
private const val TAIWAN_REGION = "TW"
private const val REMOTE_ICON_INSET_DP = 8
