package com.absinthe.libchecker.utils

import android.content.Context
import android.content.Intent
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import androidx.core.text.toSpannable
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.AUTUMN
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.SPRING
import com.absinthe.libchecker.annotation.SUMMER
import com.absinthe.libchecker.annotation.WINTER
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.URLManager
import com.absinthe.libchecker.model.LibStringItem
import com.absinthe.libchecker.utils.extensions.getDrawable
import com.absinthe.libchecker.utils.extensions.isTempApk
import com.absinthe.libchecker.utils.extensions.toClassDefType
import com.absinthe.libchecker.view.detail.CenterAlignImageSpan
import com.absinthe.rulesbundle.LCRules
import com.absinthe.rulesbundle.Rule
import java.io.File
import java.util.Calendar
import java.util.Locale

object LCAppUtils {

  fun getCurrentSeason(): Int {
    return when (Calendar.getInstance(Locale.getDefault()).get(Calendar.MONTH) + 1) {
      3, 4, 5 -> SPRING
      6, 7, 8 -> SUMMER
      9, 10, 11 -> AUTUMN
      12, 1, 2 -> WINTER
      else -> -1
    }
  }

  fun setTitle(context: Context): Spannable {
    val sb = SpannableStringBuilder(context.getString(R.string.app_name))

    when {
      DateUtils.isChristmas() -> {
        sb.append("\uD83C\uDF84")
      }
      DateUtils.isChineseNewYearEve() -> {
        sb.append("\uD83C\uDFEE")
      }
      DateUtils.isChineseNewYear() -> {
        sb.append(DateUtils.getChineseZodiac())
      }
    }

    if (BuildConfig.IS_DEV_VERSION) {
      val spanString = SpannableString("   ")
      val span = CenterAlignImageSpan(
        R.drawable.ic_ci_label.getDrawable(context)!!.also {
          it.setBounds(0, 0, it.intrinsicWidth, it.intrinsicHeight)
        }
      )
      spanString.setSpan(span, 1, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
      sb.append(spanString)
    } else {
      sb.append(" ")
    }
    return sb.toSpannable()
  }

  suspend fun getRuleWithRegex(
    name: String,
    @LibType type: Int,
    packageName: String? = null,
    nativeLibs: List<LibStringItem>? = null
  ): Rule? {
    val ruleEntity = LCRules.getRule(name, type, true) ?: return null
    if (type != NATIVE || packageName == null) {
      return ruleEntity
    }

    if (packageName.isTempApk()) {
      File(packageName)
    } else {
      runCatching {
        File(PackageUtils.getPackageInfo(packageName).applicationInfo.sourceDir)
      }.getOrNull()
    } ?: return ruleEntity

    if (!checkNativeLibValidation(packageName, name, nativeLibs)) {
      return null
    }
    return ruleEntity
  }

  private val checkNativeLibs =
    listOf("libjiagu.so", "libjiagu_a64.so", "libjiagu_x86.so", "libjiagu_x64.so", "libDexHelper.so", "libDexHelper-x86.so", "libdexjni.so", "libapp.so")
  fun checkNativeLibValidation(
    packageName: String,
    nativeLib: String,
    otherNativeLibs: List<LibStringItem>? = null
  ): Boolean {
    if (!checkNativeLibs.contains(nativeLib)) {
      return true
    }
    val source = File(PackageUtils.getPackageInfo(packageName).applicationInfo.sourceDir)
    return when (nativeLib) {
      "libjiagu.so", "libjiagu_a64.so", "libjiagu_x86.so", "libjiagu_x64.so" -> {
        runCatching {
          PackageUtils.findDexClasses(
            source,
            listOf(
              "com.qihoo.util.*".toClassDefType()
            )
          ).any { it == "com.qihoo.util.*".toClassDefType() }
        }.getOrDefault(false)
      }
      "libDexHelper.so", "libDexHelper-x86.so", "libdexjni.so" -> {
        runCatching {
          PackageUtils.findDexClasses(
            source,
            listOf(
              "com.secneo.apkwrapper.*".toClassDefType()
            )
          ).any { it == "com.secneo.apkwrapper.*".toClassDefType() }
        }.getOrDefault(false)
      }
      "libapp.so" -> {
        runCatching {
          otherNativeLibs?.any { it.name == "libflutter.so" } == true || PackageUtils.findDexClasses(
            source,
            listOf(
              "io.flutter.FlutterInjector".toClassDefType()
            )
          ).any { it == "io.flutter.FlutterInjector".toClassDefType() }
        }.getOrDefault(false)
      }
      else -> true
    }
  }

  fun launchMarketPage(context: Context, packageName: String) {
    context.startActivity(
      Intent.parseUri(
        "${
          if (PackageUtils.isAppInstalled(Constants.PackageNames.COOLAPK)) {
            URLManager.COOLAPK_SCHEME
          } else {
            URLManager.MARKET_SCHEME
          }
        }$packageName",
        0
      )
    )
  }
}
