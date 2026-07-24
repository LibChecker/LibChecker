package com.absinthe.libchecker.domain.app.detail.ui.view

import android.graphics.Typeface
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.widget.TextView
import androidx.core.text.buildSpannedString
import androidx.core.text.inSpans
import androidx.core.text.strikeThrough
import com.absinthe.libchecker.domain.app.detail.model.LibStringItemNameDisplay
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.setOrHighlightText

internal fun TextView.setLibStringItemName(
  display: LibStringItemNameDisplay,
  highlightText: String
) {
  setOrHighlightText(renderLibStringItemName(display), highlightText)
}

private fun TextView.renderLibStringItemName(display: LibStringItemNameDisplay): CharSequence {
  return when (display.decoration) {
    LibStringItemNameDisplay.Decoration.Plain -> display.text

    LibStringItemNameDisplay.Decoration.Disabled -> buildSpannedString {
      strikeThrough {
        inSpans(StyleSpan(Typeface.BOLD_ITALIC)) {
          append(display.text)
        }
      }
      // Keep the strike-through glyphs from clipping at the end of the line.
      append(" ")
    }

    LibStringItemNameDisplay.Decoration.Exported -> buildSpannedString {
      append(display.text)
      setSpan(
        ForegroundColorSpan(context.getColorByAttr(androidx.appcompat.R.attr.colorPrimary)),
        0,
        display.text.length,
        Spannable.SPAN_INCLUSIVE_EXCLUSIVE
      )
    }
  }
}
