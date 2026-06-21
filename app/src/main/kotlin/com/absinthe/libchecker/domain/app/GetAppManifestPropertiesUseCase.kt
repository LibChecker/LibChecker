package com.absinthe.libchecker.domain.app

import android.content.pm.PackageInfo
import com.absinthe.libchecker.utils.manifest.ApplicationReader
import java.io.File
import pxb.android.axml.ValueWrapper

class GetAppManifestPropertiesUseCase {

  operator fun invoke(
    packageInfo: PackageInfo?,
    properties: Map<String, *>? = null
  ): List<AppManifestProperty> {
    val propertyMap = properties ?: packageInfo?.applicationInfo?.sourceDir
      ?.let { sourceDir ->
        runCatching {
          ApplicationReader.getManifestProperties(File(sourceDir))
        }.getOrNull()
      }

    return propertyMap.orEmpty()
      .map { property ->
        AppManifestProperty(
          key = property.key,
          value = property.value.toPropertyValue()
        )
      }
      .sortedBy { property -> property.key }
  }

  private fun Any?.toPropertyValue(): String {
    return when (this) {
      is ValueWrapper -> ref.toString()
      else -> this?.toString().orEmpty()
    }
  }
}

data class AppManifestProperty(
  val key: String,
  val value: String
)
