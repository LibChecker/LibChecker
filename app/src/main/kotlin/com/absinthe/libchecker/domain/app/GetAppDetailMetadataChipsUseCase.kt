package com.absinthe.libchecker.domain.app

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.absinthe.libchecker.features.statistics.bean.LibStringItem
import com.absinthe.libchecker.features.statistics.bean.LibStringItemChip
import com.absinthe.libchecker.utils.apk.ApkPreviewInfo
import com.absinthe.libchecker.utils.extensions.maybeResourceId

class GetAppDetailMetadataChipsUseCase(
  private val packageManager: PackageManager
) {

  operator fun invoke(
    packageInfo: PackageInfo,
    apkPreviewInfo: ApkPreviewInfo?,
    isApkPreview: Boolean
  ): List<LibStringItemChip> {
    val items = if (!isApkPreview && apkPreviewInfo == null) {
      getInstalledMetadataChips(packageInfo)
    } else {
      apkPreviewInfo!!.metadata
        .map { metadata ->
          var flag = 0L
          val value = if (metadata.value is Long || (metadata.value as? String)?.maybeResourceId() == true) {
            flag = -1
            null
          } else {
            metadata.value.toString()
          }
          LibStringItemChip(
            LibStringItem(metadata.key, flag, value),
            null
          )
        }
    }.toMutableList()

    items.sortByDescending { it.item.name }
    return items
  }

  private fun getInstalledMetadataChips(packageInfo: PackageInfo): List<LibStringItemChip> {
    val applicationInfo = packageInfo.applicationInfo ?: return emptyList()
    val metadata = applicationInfo.metaData ?: return emptyList()
    val appResources = runCatching {
      packageManager.getResourcesForApplication(applicationInfo)
    }.getOrNull()

    return metadata.keySet().asSequence()
      .map { key ->
        @Suppress("DEPRECATION")
        val value = metadata.get(key).toString()
        val resourceId = value.takeIf(String::maybeResourceId)?.toLongOrNull()
        val displayValue = resourceId?.let { id ->
          runCatching {
            appResources?.getResourceName(id.toInt())
          }.getOrNull()
        } ?: value
        val resourceType = resourceId?.let { id ->
          runCatching {
            appResources?.getResourceTypeName(id.toInt())
          }.getOrNull()
        }

        LibStringItemChip(
          LibStringItem(key, resourceId ?: 0L, displayValue),
          null,
          listOfNotNull(resourceType)
        )
      }
      .toList()
  }
}
