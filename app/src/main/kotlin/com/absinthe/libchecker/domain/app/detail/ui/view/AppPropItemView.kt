package com.absinthe.libchecker.domain.app.detail.ui.view

import android.content.Context
import android.util.TypedValue
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.model.AppPropItem
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.view.app.ResourcePreviewButton
import com.absinthe.libchecker.view.app.TextColumnRowView

class AppPropItemView(context: Context) : TextColumnRowView(context) {

  private val key = addTextLine(R.style.TextView_SansSerifCondensedMedium) {
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
  }
  private val value = addTextLine(R.style.TextView_SansSerifCondensedMedium) {
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
    alpha = 0.65f
  }
  private val resourcePreview = ResourcePreviewButton(context)

  init {
    setTrailingView(resourcePreview, touchTargetSize = 48.dp)
  }

  fun bind(item: AppPropItem, onResourceClick: (AppPropItem) -> Unit) {
    key.text = item.key
    value.text = item.visibleValue
    contentDescription = item.contentDescription
    resourcePreview.bind(
      preview = item.preview,
      onClick = item.resource?.let { { onResourceClick(item) } }
    )
  }
}
