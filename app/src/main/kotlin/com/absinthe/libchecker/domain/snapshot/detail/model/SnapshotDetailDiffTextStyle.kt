package com.absinthe.libchecker.domain.snapshot.detail.model

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.annotation.ColorInt

data class SnapshotDetailDiffTextStyle(
  @ColorInt val highlightColor: Int?,
  val emphasizeDiffs: Boolean = false,
  @ColorInt val arrowColor: Int,
  @ColorInt val metricDeltaColor: Int? = null
)

internal fun CharSequence.colorSnapshotDetailMetricDeltas(
  @ColorInt color: Int?
): CharSequence {
  if (color == null) return this
  val ranges = snapshotDetailMetricDeltaRanges(this)
  if (ranges.isEmpty()) return this

  return SpannableStringBuilder(this).apply {
    ranges.forEach { range ->
      setSpan(
        ForegroundColorSpan(color),
        range.first,
        range.last + 1,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
      )
    }
  }
}

internal fun CharSequence.emphasizeSnapshotDetailDiffArrows(
  @ColorInt arrowColor: Int
): CharSequence {
  val arrowIndices = snapshotDetailDiffArrowIndices(this)
  if (arrowIndices.isEmpty()) return this

  return SpannableStringBuilder(this).apply {
    arrowIndices.forEach { index ->
      setSpan(
        StyleSpan(Typeface.BOLD),
        index,
        index + SNAPSHOT_DETAIL_DIFF_ARROW.length,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
      )
      setSpan(
        ForegroundColorSpan(arrowColor),
        index,
        index + SNAPSHOT_DETAIL_DIFF_ARROW.length,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
      )
    }
  }
}

internal fun snapshotDetailDiffArrowIndices(text: CharSequence): List<Int> {
  return text.indices.filter { text[it] == SNAPSHOT_DETAIL_DIFF_ARROW[0] }
}

internal fun snapshotDetailMetricDeltaRanges(text: CharSequence): List<IntRange> {
  val result = mutableListOf<IntRange>()
  var lineStart = 0
  while (lineStart < text.length) {
    val lineEnd = text.indexOf('\n', lineStart).takeIf { it >= 0 } ?: text.length
    if (text[lineStart] == '+' || text[lineStart] == '-') {
      result += lineStart until lineEnd
    }
    lineStart = lineEnd + 1
  }
  return result
}

internal const val SNAPSHOT_DETAIL_DIFF_ARROW = "→"
