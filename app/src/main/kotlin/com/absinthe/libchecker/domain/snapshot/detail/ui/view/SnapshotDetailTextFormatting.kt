package com.absinthe.libchecker.domain.snapshot.detail.ui.view

import android.text.SpannableStringBuilder
import android.widget.TextView
import com.absinthe.libchecker.domain.snapshot.detail.model.SNAPSHOT_DETAIL_DIFF_ARROW

internal class SnapshotDetailLineBreaker(
  private val textView: TextView
) {

  private var sourceText: CharSequence? = null
  private var secondaryBreakStart = -1

  fun setText(text: CharSequence, secondaryBreakStart: Int = -1) {
    sourceText = text
    this.secondaryBreakStart = secondaryBreakStart
    textView.text = text
  }

  fun clear() {
    sourceText = null
    secondaryBreakStart = -1
    textView.text = null
  }

  fun apply(maxWidth: Int) {
    val source = sourceText ?: return
    val breakIndices = planSnapshotDetailLineBreaks(
      text = source.toString(),
      secondaryBreakStart = secondaryBreakStart,
      maxWidth = maxWidth.toFloat(),
      measureText = textView.paint::measureText
    )
    val target = if (breakIndices.isEmpty()) {
      source
    } else {
      SpannableStringBuilder(source).apply {
        breakIndices.asReversed().forEach { index ->
          if (this[index] == ' ') {
            replace(index, index + 1, "\n")
          } else {
            insert(index, "\n")
          }
        }
      }
    }
    if (textView.text.toString() != target.toString()) {
      textView.text = target
    }
  }
}

internal fun planSnapshotDetailLineBreaks(
  text: String,
  secondaryBreakStart: Int,
  maxWidth: Float,
  measureText: (String) -> Float
): List<Int> {
  if (text.isEmpty() || maxWidth <= 0f) return emptyList()

  val result = mutableListOf<Int>()
  val hasSecondaryValue = secondaryBreakStart in 1 until text.length
  val primaryEnd = if (hasSecondaryValue) secondaryBreakStart else text.length
  val primaryText = text.substring(0, primaryEnd).trimEnd()
  val arrowIndex = text.indexOf(SNAPSHOT_DETAIL_DIFF_ARROW)
  if (
    arrowIndex in 1 until primaryEnd &&
    measureText(primaryText) > maxWidth
  ) {
    result += lineBreakIndexBefore(text, arrowIndex)
  }
  if (hasSecondaryValue && measureText(text) > maxWidth) {
    result += lineBreakIndexBefore(text, secondaryBreakStart)
  }
  return result.distinct().sorted()
}

internal fun CharSequence.withSnapshotTechnicalPathBreakOpportunities(): CharSequence {
  val offsets = snapshotTechnicalPathBreakOffsets(this)
  if (offsets.isEmpty()) return this

  return SpannableStringBuilder(this).apply {
    offsets.asReversed().forEach { insert(it, ZERO_WIDTH_SPACE) }
  }
}

internal fun snapshotTechnicalPathBreakOffsets(text: CharSequence): List<Int> {
  return text.indices
    .filter { index -> text[index] == '.' && index < text.lastIndex }
    .map { it + 1 }
}

private fun lineBreakIndexBefore(text: String, index: Int): Int {
  return if (index > 0 && text[index - 1] == ' ') index - 1 else index
}

private const val ZERO_WIDTH_SPACE = "\u200B"
