package com.absinthe.libchecker.domain.app.detail.ui.view

import android.content.Context
import android.util.TypedValue
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.model.LibStringMetadataItemDisplay
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.utils.extensions.setOrHighlightText
import com.absinthe.libchecker.view.app.ResourcePreviewButton
import com.absinthe.libchecker.view.app.TextColumnRowView

class MetadataLibItemView(context: Context) : TextColumnRowView(context) {

  private val libName = addTextLine(R.style.TextView_SansSerifMedium) {
    setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface))
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
  }
  private val libSize = addTextLine(R.style.TextView_SansSerifCondensed) {
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
  }
  private val resourcePreview = ResourcePreviewButton(context)

  init {
    isClickable = true
    isFocusable = true
    clipToPadding = false
    val horizontalPadding = context.getDimensionPixelSize(R.dimen.normal_padding)
    setPadding(horizontalPadding, 4.dp, horizontalPadding, 4.dp)
    minimumHeight = 40.dp
    setTrailingView(
      resourcePreview,
      spacing = horizontalPadding + 8.dp,
      touchTargetSize = 48.dp
    )
  }

  fun bind(
    display: LibStringMetadataItemDisplay,
    highlightText: String,
    onResourceClick: ((LibStringMetadataItemDisplay) -> Unit)?
  ) {
    libName.setLibStringItemName(display.name, highlightText)
    libSize.setOrHighlightText(display.visibleValue, highlightText)
    contentDescription = display.contentDescription
    resourcePreview.bind(
      preview = display.preview,
      onClick = if (display.resource != null && onResourceClick != null) {
        { onResourceClick(display) }
      } else {
        null
      }
    )
  }
}
