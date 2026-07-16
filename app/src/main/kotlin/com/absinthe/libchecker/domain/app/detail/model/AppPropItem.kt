package com.absinthe.libchecker.domain.app.detail.model

import com.absinthe.libchecker.domain.app.detail.resource.AppResourcePreview
import com.absinthe.libchecker.domain.app.detail.resource.AppResourceReference

data class AppPropItem(
  val key: String,
  val originalDisplayValue: String,
  val resource: AppResourceReference?,
  val preview: AppResourcePreview = AppResourcePreview.Original
) {
  val visibleValue: CharSequence = when (preview) {
    is AppResourcePreview.Text -> preview.value
    else -> originalDisplayValue
  }

  val contentDescription: String = buildDetailItemDescription(key, visibleValue)

  val isTransformed: Boolean = preview != AppResourcePreview.Original

  fun restore(): AppPropItem = copy(preview = AppResourcePreview.Original)
}
