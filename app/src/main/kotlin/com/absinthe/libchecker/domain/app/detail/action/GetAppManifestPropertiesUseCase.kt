package com.absinthe.libchecker.domain.app.detail.action

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.absinthe.libchecker.domain.app.detail.model.AppPropItem
import com.absinthe.libchecker.domain.app.detail.model.AppPropResourceReference
import com.absinthe.libchecker.utils.extensions.maybeResourceId
import com.absinthe.libchecker.utils.manifest.ApplicationReader
import com.absinthe.libchecker.utils.manifest.PropertiesMap
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pxb.android.axml.ValueWrapper

class GetAppManifestPropertiesUseCase(
  private val packageManager: PackageManager
) {

  suspend operator fun invoke(
    packageInfo: PackageInfo?,
    properties: Map<String, *>? = null
  ): List<AppPropItem> = withContext(Dispatchers.IO) {
    val propertyMap = properties ?: packageInfo?.applicationInfo?.sourceDir
      ?.let { sourceDir ->
        runCatching {
          ApplicationReader.getManifestProperties(File(sourceDir))
        }.getOrNull()
      }
    val appResources = packageInfo?.applicationInfo?.let { applicationInfo ->
      runCatching {
        packageManager.getResourcesForApplication(applicationInfo)
      }.getOrNull()
    }

    propertyMap.orEmpty()
      .map { property ->
        val value = property.value.toPropertyValue()
        val resourceId = value.toResourceIdOrNull()
        val resourceName = resourceId?.let { id ->
          runCatching {
            appResources?.getResourceName(id)
          }.getOrNull()
        }
        val resourceType = resourceId?.let { id ->
          runCatching {
            appResources?.getResourceTypeName(id)
          }.getOrNull()
        }
        AppPropItem(
          key = property.key,
          originalDisplayValue = resourceName ?: PropertiesMap.parseProperty(property.key, value),
          resource = AppPropResourceReference.create(resourceId, resourceType)
        )
      }
      .sortedBy(AppPropItem::key)
  }

  private fun Any?.toPropertyValue(): String {
    return when (this) {
      is ValueWrapper -> ref.toString()
      else -> this?.toString().orEmpty()
    }
  }

  private fun String.toResourceIdOrNull(): Int? {
    return takeIf(String::maybeResourceId)?.toIntOrNull()
  }
}
