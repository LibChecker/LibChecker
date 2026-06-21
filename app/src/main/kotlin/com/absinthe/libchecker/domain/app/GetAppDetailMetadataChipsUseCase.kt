package com.absinthe.libchecker.domain.app

import android.content.pm.PackageInfo
import com.absinthe.libchecker.features.statistics.bean.LibStringItem
import com.absinthe.libchecker.features.statistics.bean.LibStringItemChip
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.apk.ApkPreviewInfo
import com.absinthe.libchecker.utils.extensions.maybeResourceId

class GetAppDetailMetadataChipsUseCase {

  operator fun invoke(
    packageInfo: PackageInfo,
    apkPreviewInfo: ApkPreviewInfo?,
    isApkPreview: Boolean
  ): List<LibStringItemChip> {
    val items = if (!isApkPreview && apkPreviewInfo == null) {
      PackageUtils.getMetaDataItems(packageInfo)
        .map { LibStringItemChip(it, null) }
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
}
