package com.absinthe.libchecker.domain.app.detail.resource

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.absinthe.libchecker.utils.manifest.ResourceParser

class ResolveAppResourceValueUseCase(
  private val packageManager: PackageManager
) {

  operator fun invoke(request: Request): AppResourceValue? {
    val resources = runCatching {
      request.applicationInfo?.let(packageManager::getResourcesForApplication)
        ?: request.packageName?.let(packageManager::getResourcesForApplication)
    }.getOrNull() ?: return null
    val resourceId = request.resourceId
    val resourceType = request.resourceType ?: runCatching {
      resources.getResourceTypeName(resourceId)
    }.getOrNull() ?: return null

    return runCatching {
      when (resourceType) {
        "string" -> AppResourceValue.Text(resources.getString(resourceId))

        "array" -> AppResourceValue.Text(resources.getStringArray(resourceId).contentToString())

        "bool" -> AppResourceValue.Text(resources.getBoolean(resourceId).toString())

        "xml" -> AppResourceValue.Xml(
          ResourceParser(resources.getXml(resourceId))
            .setMarkColor(true)
            .parse()
        )

        "drawable", "mipmap" -> AppResourceValue.DrawablePreview(
          resources.getDrawable(resourceId, null)
        )

        "color" -> AppResourceValue.ColorPreview(resources.getColor(resourceId, null))

        "dimen" -> AppResourceValue.Text(resources.getDimension(resourceId).toString())

        "integer" -> AppResourceValue.Text(resources.getInteger(resourceId).toString())

        else -> null
      }
    }.getOrNull()
  }

  data class Request(
    val packageName: String? = null,
    val applicationInfo: ApplicationInfo? = null,
    val resourceId: Int,
    val resourceType: String? = null
  )

  sealed class AppResourceValue {
    data class Text(val value: CharSequence) : AppResourceValue()
    data class Xml(val value: CharSequence) : AppResourceValue()
    data class DrawablePreview(val drawable: Drawable) : AppResourceValue()
    data class ColorPreview(val color: Int) : AppResourceValue()
  }

  companion object {
    private val LINKABLE_TYPES = setOf(
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

    fun isLinkableType(type: String?): Boolean {
      return type != null && type in LINKABLE_TYPES
    }
  }
}
