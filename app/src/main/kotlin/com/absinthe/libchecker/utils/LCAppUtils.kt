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
import com.absinthe.libchecker.domain.snapshot.list.ui.adapter.ARROW
import com.absinthe.libchecker.domain.snapshot.list.ui.adapter.ARROW_REVERT
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.utils.extensions.getDrawable
import com.absinthe.libchecker.view.span.CenterAlignImageSpan
import com.absinthe.libchecker.view.span.DiffHighlightSpan

object LCAppUtils {

  fun buildAppTitle(context: Context): Spannable {
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
    highlightDiffColor: Int? = null,
    emphasizeDiffs: Boolean = false
  ): CharSequence {
    return if (diff.old != diff.new && diff.new != null && !isNewOrDeleted) {
      val oldString = format.format(diff.old)
      val newString = format.format(diff.new)

      if (highlightDiffColor != null || emphasizeDiffs) {
        val pair = getHighlightDifferences(oldString, newString, highlightDiffColor, emphasizeDiffs)
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
    highlightDiffColor: Int? = null,
    emphasizeDiffs: Boolean = false
  ): CharSequence {
    return if ((diff1.old != diff1.new || diff2.old != diff2.new) && !isNewOrDeleted) {
      if (highlightDiffColor != null || emphasizeDiffs) {
        val highlightedPair1 =
          getHighlightDifferences(diff1.old.toString(), diff1.new.toString(), highlightDiffColor, emphasizeDiffs)
        val highlightedPair2 =
          getHighlightDifferences(diff2.old.toString(), diff2.new.toString(), highlightDiffColor, emphasizeDiffs)
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
    highlightDiffColor: Int?,
    emphasizeDiffs: Boolean = false
  ): Pair<SpannableString, SpannableString> {
    val oldSpannable = SpannableString(oldString)
    val newSpannable = SpannableString(newString)
    var oldIndex = 0
    var newIndex = 0

    while (oldIndex < oldString.length && newIndex < newString.length) {
      val oldEnd = oldString.offsetByCodePoints(oldIndex, 1)
      val newEnd = newString.offsetByCodePoints(newIndex, 1)
      if (oldString.codePointAt(oldIndex) != newString.codePointAt(newIndex)) {
        oldSpannable.setDiffHighlight(oldIndex, oldEnd, highlightDiffColor, emphasizeDiffs)
        newSpannable.setDiffHighlight(newIndex, newEnd, highlightDiffColor, emphasizeDiffs)
      }
      oldIndex = oldEnd
      newIndex = newEnd
    }

    if (oldIndex < oldString.length) {
      oldSpannable.setDiffHighlight(oldIndex, oldString.length, highlightDiffColor, emphasizeDiffs)
    } else if (newIndex < newString.length) {
      newSpannable.setDiffHighlight(newIndex, newString.length, highlightDiffColor, emphasizeDiffs)
    }

    return Pair(oldSpannable, newSpannable)
  }

  private fun SpannableString.setDiffHighlight(
    start: Int,
    end: Int,
    @androidx.annotation.ColorInt color: Int?,
    emphasizeDiffs: Boolean
  ) {
    if (emphasizeDiffs) {
      var current = start
      while (current < end) {
        val next = Character.offsetByCodePoints(this, current, 1)
        setSpan(
          DiffHighlightSpan(color),
          current,
          next,
          Spannable.SPAN_INCLUSIVE_EXCLUSIVE
        )
        current = next
      }
    } else if (color != null) {
      setSpan(
        ForegroundColorSpan(color),
        start,
        end,
        Spannable.SPAN_INCLUSIVE_EXCLUSIVE
      )
    }
  }

  private fun getArrow(isRtl: Boolean): String {
    return if (isRtl) {
      ARROW_REVERT
    } else {
      ARROW
    }
  }
}
