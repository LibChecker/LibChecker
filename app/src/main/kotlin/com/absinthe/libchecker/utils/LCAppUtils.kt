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
import com.absinthe.libchecker.features.applist.detail.ui.view.CenterAlignImageSpan
import com.absinthe.libchecker.features.snapshot.detail.bean.SnapshotDiffItem
import com.absinthe.libchecker.features.snapshot.ui.adapter.ARROW
import com.absinthe.libchecker.features.snapshot.ui.adapter.ARROW_REVERT
import com.absinthe.libchecker.utils.extensions.getDrawable

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
        "${diff1.old} (${diff2.old}$diff2Suffix) $ARROW ${diff1.new} (${diff2.new}$diff2Suffix)"
      }
    } else {
      "${diff1.old} (${diff2.old}$diff2Suffix)"
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
