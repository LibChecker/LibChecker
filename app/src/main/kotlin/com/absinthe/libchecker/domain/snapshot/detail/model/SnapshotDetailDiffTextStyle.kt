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
  @ColorInt val arrowColor: Int
)

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
  val result = mutableListOf<Int>()
  var searchStart = 0
  while (searchStart < text.length) {
    val index = text.indexOf(SNAPSHOT_DETAIL_DIFF_ARROW, searchStart)
    if (index < 0) break
    result += index
    searchStart = index + SNAPSHOT_DETAIL_DIFF_ARROW.length
  }
  return result
}

internal const val SNAPSHOT_DETAIL_DIFF_ARROW = "→"
