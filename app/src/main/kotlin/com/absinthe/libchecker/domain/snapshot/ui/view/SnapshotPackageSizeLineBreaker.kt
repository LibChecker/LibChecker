package com.absinthe.libchecker.domain.snapshot.ui.view

import android.text.SpannableStringBuilder
import android.widget.TextView

internal class SnapshotPackageSizeLineBreaker(
  private val textView: TextView
) {

  private var singleLineText: CharSequence? = null
  private var breakStart = -1
  private var wrappedText: CharSequence? = null

  fun setText(text: CharSequence, breakStart: Int) {
    singleLineText = text
    this.breakStart = breakStart
    wrappedText = null
    textView.text = text
  }

  fun clear() {
    singleLineText = null
    breakStart = -1
    wrappedText = null
    textView.text = null
  }

  fun apply(maxWidth: Int) {
    val text = singleLineText ?: return
    val targetText = if (shouldWrap(text, maxWidth)) {
      wrappedText ?: text.withLineBreakAt(breakStart).also {
        wrappedText = it
      }
    } else {
      text
    }

    if (textView.text.toString() != targetText.toString()) {
      textView.text = targetText
    }
  }

  private fun shouldWrap(text: CharSequence, maxWidth: Int): Boolean {
    return maxWidth > 0 &&
      breakStart in 1 until text.length &&
      textView.paint.measureText(text, 0, text.length) > maxWidth
  }

  private fun CharSequence.withLineBreakAt(index: Int): CharSequence {
    val builder = SpannableStringBuilder(this)
    val replaceStart = if (this[index - 1] == ' ') index - 1 else index
    builder.replace(replaceStart, index, "\n")
    return builder
  }
}
