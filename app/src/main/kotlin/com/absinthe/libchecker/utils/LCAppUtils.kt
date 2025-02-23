package com.absinthe.libchecker.utils

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import androidx.core.text.BidiFormatter
import androidx.core.text.buildSpannedString
import androidx.core.text.toSpannable
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.features.applist.detail.ui.view.CenterAlignImageSpan
import com.absinthe.libchecker.features.snapshot.detail.bean.SnapshotDiffItem
import com.absinthe.libchecker.features.snapshot.ui.adapter.ARROW
import com.absinthe.libchecker.features.snapshot.ui.adapter.ARROW_REVERT
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
        File(PackageUtils.getPackageInfo(packageName).applicationInfo!!.sourceDir)
      }.getOrNull()
    } ?: return ruleEntity

    if (!checkNativeLibValidation(packageName, name, nativeLibs)) {
      return null
    }
    return ruleEntity
  }

  private val NATIVE_SET_QIHOO = setOf("libjiagu.so", "libjiagu_a64.so", "libjiagu_x86.so", "libjiagu_x64.so")
  private val NATIVE_SET_SECNEO = setOf("libDexHelper.so", "libDexHelper-x86.so", "libdexjni.so")
  private val NATIVE_SET_FLUTTER = setOf("libapp.so")
  private val NATIVE_SET_UNITY = setOf("libmain.so")
  private val NATIVE_ALL = NATIVE_SET_QIHOO + NATIVE_SET_SECNEO + NATIVE_SET_FLUTTER + NATIVE_SET_UNITY

  fun checkNativeLibValidation(
    packageName: String,
    nativeLib: String,
    otherNativeLibs: List<LibStringItem>? = null
  ): Boolean {
    if (!NATIVE_ALL.contains(nativeLib)) return true
    val sourceDir = PackageUtils.getPackageInfo(packageName).applicationInfo?.sourceDir ?: return false
    val source = File(sourceDir)
    return when {
      NATIVE_SET_QIHOO.contains(nativeLib) -> {
        runCatching {
          PackageUtils.findDexClasses(
            source,
            listOf(
              "com.qihoo.util.*".toClassDefType(),
              "com.tianyu.util.*".toClassDefType()
            ),
            hasAny = true
          ).isNotEmpty()
        }.getOrDefault(false)
      }

      NATIVE_SET_SECNEO.contains(nativeLib) -> {
        runCatching {
          PackageUtils.findDexClasses(
            source,
            listOf(
              "com.secneo.apkwrapper.*".toClassDefType()
            )
          ).isNotEmpty()
        }.getOrDefault(false)
      }

      NATIVE_SET_FLUTTER.contains(nativeLib) -> {
        runCatching {
          otherNativeLibs?.any { it.name == "libflutter.so" } == true ||
            PackageUtils.findDexClasses(
              source,
              listOf(
                "io.flutter.FlutterInjector".toClassDefType()
              )
            ).isNotEmpty()
        }.getOrDefault(false)
      }

      NATIVE_SET_UNITY.contains(nativeLib) -> {
        runCatching {
          otherNativeLibs?.any { it.name == "libunity.so" } == true
        }.getOrDefault(false)
      }

      else -> true
    }
  }

  fun <T> getDiffString(
    diff: SnapshotDiffItem.DiffNode<T>,
    isNewOrDeleted: Boolean = false,
    format: String = "%s",
    highlightDiffColor: Int? = null
  ): CharSequence {
    return if (diff.old != diff.new && diff.new != null && !isNewOrDeleted) {
      val oldString = format.format(diff.old)
      val newString = format.format(diff.new)

      if (highlightDiffColor != null) {
        val pair = getHighlightDifferences(oldString, newString, highlightDiffColor)
        buildSpannedString {
          append(pair.first)
          append(" $ARROW ")
          append(pair.second)
        }
      } else {
        buildSpannedString {
          append(oldString)
          append(" $ARROW ")
          append(newString)
        }
      }
    } else {
      format.format(diff.old)
    }
  }

  fun getDiffString(
    diff1: SnapshotDiffItem.DiffNode<*>,
    diff1Suffix: String = "",
    diff2: SnapshotDiffItem.DiffNode<*>,
    diff2Suffix: String = "",
    isNewOrDeleted: Boolean = false,
    highlightDiffColor: Int? = null
  ): CharSequence {
    return if ((diff1.old != diff1.new || diff2.old != diff2.new) && !isNewOrDeleted) {
      if (highlightDiffColor != null) {
        val highlightedPair1 =
          getHighlightDifferences(diff1.old.toString(), diff1.new.toString(), highlightDiffColor)
        val highlightedPair2 =
          getHighlightDifferences(diff2.old.toString(), diff2.new.toString(), highlightDiffColor)
        val allText = highlightedPair1.first.toString() + highlightedPair1.second + highlightedPair2.first + highlightedPair2.second + diff1Suffix + diff2Suffix
        val isRtl = BidiFormatter.getInstance().isRtl(allText)
        buildSpannedString {
          append(highlightedPair1.first)
          append(diff1Suffix)
          append(" (")
          append(highlightedPair2.first)
          append(diff2Suffix)
          append(")")
          append(" ${getArrow(isRtl)} ")
          append(highlightedPair1.second)
          append(diff1Suffix)
          append(" (")
          append(highlightedPair2.second)
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

  fun getHighlightDifferences(
    oldString: String,
    newString: String,
    highlightDiffColor: Int
  ): Pair<SpannableString, SpannableString> {
    val oldSpannable = SpannableString(oldString)
    val newSpannable = SpannableString(newString)
    val minLength = minOf(oldString.length, newString.length)

    for (i in 0 until minLength) {
      if (oldString[i] != newString[i]) {
        oldSpannable.setSpan(
          ForegroundColorSpan(highlightDiffColor),
          i,
          i + 1,
          Spannable.SPAN_INCLUSIVE_EXCLUSIVE
        )
        newSpannable.setSpan(
          ForegroundColorSpan(highlightDiffColor),
          i,
          i + 1,
          Spannable.SPAN_INCLUSIVE_EXCLUSIVE
        )
      }
    }

    if (oldString.length > newString.length) {
      oldSpannable.setSpan(
        ForegroundColorSpan(highlightDiffColor),
        minLength,
        oldString.length,
        Spannable.SPAN_INCLUSIVE_EXCLUSIVE
      )
    } else if (newString.length > oldString.length) {
      newSpannable.setSpan(
        ForegroundColorSpan(highlightDiffColor),
        minLength,
        newString.length,
        Spannable.SPAN_INCLUSIVE_EXCLUSIVE
      )
    }

    return Pair(oldSpannable, newSpannable)
  }

  private fun getArrow(isRtl: Boolean): String {
    return if (isRtl) {
      ARROW_REVERT
    } else {
      ARROW
    }
  }
}
