package com.absinthe.libchecker.domain.statistics.reference.ui.view

import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.text.buildSpannedString
import androidx.core.text.italic
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.statistics.reference.model.LibReferenceItemDisplay
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.utils.extensions.setOrHighlightText
import com.absinthe.libchecker.view.app.TextColumnRowView

class LibReferenceItemView(context: Context) : TextColumnRowView(context) {

  private val icon = AppCompatImageButton(context).apply {
    id = android.R.id.icon
    val size = context.getDimensionPixelSize(R.dimen.lib_reference_icon_size)
    layoutParams = ViewGroup.LayoutParams(size, size)
    scaleType = ImageView.ScaleType.CENTER_INSIDE
    setBackgroundResource(R.drawable.bg_circle_secondary_container)
  }
  private val labelName = addTextLine(R.style.TextView_SansSerifMedium) {
    setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface))
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
  }
  private val libName = addTextLine(R.style.TextView_SansSerif) {
    setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface))
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
  }
  private val count = createTextView(R.style.TextView_SansSerif) {
    setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceHeadlineMedium))
  }

  init {
    val padding = context.getDimensionPixelSize(R.dimen.main_card_padding)
    setPadding(padding, padding, padding, padding)
    setBackgroundResource(context.getResourceIdByAttr(android.R.attr.selectableItemBackground))
    setLeadingView(icon)
    setTrailingView(count)
  }

  fun bind(display: LibReferenceItemDisplay, highlightText: String) {
    icon.apply {
      setImageResource(display.iconRes)
      contentDescription = display.iconContentDescription
      importantForAccessibility = if (display.canOpenDetail) {
        View.IMPORTANT_FOR_ACCESSIBILITY_YES
      } else {
        View.IMPORTANT_FOR_ACCESSIBILITY_NO
      }
      isClickable = display.canOpenDetail
      isFocusable = display.canOpenDetail
      drawable?.mutate()?.colorFilter = if (display.desaturateIcon) {
        ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
      } else {
        null
      }
    }
    if (display.italicLabel) {
      labelName.text = buildSpannedString {
        italic { append(display.label) }
        append(" ")
      }
    } else {
      labelName.setOrHighlightText(display.label, highlightText)
    }
    libName.setOrHighlightText(display.libName, highlightText)
    count.text = display.count
    contentDescription = display.contentDescription
  }
}
