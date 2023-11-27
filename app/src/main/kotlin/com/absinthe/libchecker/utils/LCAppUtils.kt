package com.absinthe.libchecker.utils

import android.content.Context
import android.content.Intent
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import androidx.core.text.buildSpannedString
import androidx.core.text.toSpannable
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.URLManager
import com.absinthe.libchecker.features.applist.detail.ui.view.CenterAlignImageSpan
import com.absinthe.libchecker.features.snapshot.detail.bean.SnapshotDiffItem
import com.absinthe.libchecker.features.snapshot.ui.adapter.ARROW
import com.absinthe.libchecker.features.statistics.bean.LibStringItem
import com.absinthe.libchecker.utils.extensions.getDrawable
import com.absinthe.libchecker.utils.extensions.isTempApk
import com.absinthe.libchecker.utils.extensions.toClassDefType
import com.absinthe.rulesbundle.LCRules
import com.absinthe.rulesbundle.Rule
import java.io.File

object LCAppUtils {

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

  fun <T> getDiffString(
    diff: SnapshotDiffItem.DiffNode<T>,
    isNewOrDeleted: Boolean = false,
    format: String = "%s",
    highlightDiffColor: Int? = null
  ): CharSequence {
    return if (diff.old != diff.new && !isNewOrDeleted) {
      val oldString = format.format(diff.old)
      val newString = format.format(diff.new)

      val highlightNewString = if (highlightDiffColor != null) {
        getHighlightDifferences(oldString, newString, highlightDiffColor)
      } else {
        newString
      }
      buildSpannedString {
        append(oldString)
        append(" $ARROW ")
        append(highlightNewString)
      }
    } else {
      format.format(diff.old)
    }
  }

  fun getDiffString(
    diff1: SnapshotDiffItem.DiffNode<*>,
    diff2: SnapshotDiffItem.DiffNode<*>,
    isNewOrDeleted: Boolean = false,
    highlightDiffColor: Int? = null,
    diff2Suffix: String = ""
  ): CharSequence {
    return if ((diff1.old != diff1.new || diff2.old != diff2.new) && !isNewOrDeleted) {
      if (highlightDiffColor != null) {
        val highlightedNew1 =
          getHighlightDifferences(diff1.old.toString(), diff1.new.toString(), highlightDiffColor)
        val highlightedNew2 =
          getHighlightDifferences(diff2.old.toString(), diff2.new.toString(), highlightDiffColor)
        buildSpannedString {
          append("${diff1.old} (${diff2.old})")
          append(" $ARROW ")
          append(highlightedNew1)
          append(" (")
          append(highlightedNew2)
          append(diff2Suffix)
          append(")")
        }
      } else {
        "${diff1.old} (${diff2.old}) $ARROW ${diff1.new} (${diff2.new})"
      }
    } else {
      "${diff1.old} (${diff2.old})"
    }
  }

  private fun getHighlightDifferences(
    oldString: String,
    newString: String,
    highlightDiffColor: Int
  ): SpannableString {
    val spannable = SpannableString(newString)
    val minLength = minOf(oldString.length, newString.length)

    for (i in 0 until minLength) {
      if (oldString[i] != newString[i]) {
        spannable.setSpan(
          ForegroundColorSpan(highlightDiffColor),
          i,
          i + 1,
          Spannable.SPAN_INCLUSIVE_EXCLUSIVE
        )
      }
    }

    if (newString.length > oldString.length) {
      spannable.setSpan(
        ForegroundColorSpan(highlightDiffColor),
        minLength,
        newString.length,
        Spannable.SPAN_INCLUSIVE_EXCLUSIVE
      )
    }

    return spannable
  }
}
