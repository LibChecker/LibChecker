package com.absinthe.libchecker.domain.app.detail.model

import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt

data class AppPropItem(
  val key: String,
  val originalDisplayValue: String,
  val resource: AppPropResourceReference?,
  val preview: AppPropPreview = AppPropPreview.Original
) {
  val visibleValue: CharSequence = when (preview) {
    is AppPropPreview.Text -> preview.value
    else -> originalDisplayValue
  }

  val contentDescription: String = buildDetailItemDescription(key, visibleValue)

  val isTransformed: Boolean = preview != AppPropPreview.Original

  fun restore(): AppPropItem = copy(preview = AppPropPreview.Original)
}

data class AppPropResourceReference(
  val id: Int,
  val type: String
) {
  init {
    require(isAppResourcePreviewSupported(type))
  }

  companion object {
    fun create(resourceId: Int?, resourceType: String?): AppPropResourceReference? {
      return if (resourceId != null && resourceType != null && isAppResourcePreviewSupported(resourceType)) {
        AppPropResourceReference(resourceId, resourceType)
      } else {
        null
      }
    }
  }
}

sealed interface AppPropPreview {
  data object Original : AppPropPreview

  data class Text(val value: CharSequence) : AppPropPreview

  data class DrawableValue(val drawable: Drawable) : AppPropPreview

  data class ColorValue(@ColorInt val color: Int) : AppPropPreview
}

private val APP_RESOURCE_PREVIEW_TYPES = setOf(
  "array",
  "bool",
  "color",
  "dimen",
  "drawable",
  "integer",
  "mipmap",
  "string",
  "xml"
)

fun isAppResourcePreviewSupported(type: String?): Boolean {
  return type != null && type in APP_RESOURCE_PREVIEW_TYPES
}
