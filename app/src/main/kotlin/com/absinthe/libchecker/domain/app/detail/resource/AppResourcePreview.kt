package com.absinthe.libchecker.domain.app.detail.resource

import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt

data class AppResourceReference(
  val id: Int,
  val type: String
) {
  init {
    require(isAppResourcePreviewSupported(type))
  }

  companion object {
    fun create(resourceId: Int?, resourceType: String?): AppResourceReference? {
      return if (resourceId != null && resourceType != null && isAppResourcePreviewSupported(resourceType)) {
        AppResourceReference(resourceId, resourceType)
      } else {
        null
      }
    }
  }
}

sealed interface AppResourcePreview {
  data object Original : AppResourcePreview

  data class Text(val value: CharSequence) : AppResourcePreview

  data class DrawableValue(val drawable: Drawable) : AppResourcePreview

  data class ColorValue(@ColorInt val color: Int) : AppResourcePreview
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
