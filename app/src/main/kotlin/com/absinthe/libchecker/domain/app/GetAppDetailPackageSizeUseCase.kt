package com.absinthe.libchecker.domain.app

import android.content.pm.PackageInfo
import com.absinthe.libchecker.utils.FileUtils
import com.absinthe.libchecker.utils.apk.ApkPreviewInfo

class GetAppDetailPackageSizeUseCase(
  private val getAppBundleItems: GetAppBundleItemsUseCase
) {

  operator fun invoke(
    packageInfo: PackageInfo,
    apkPreviewInfo: ApkPreviewInfo?,
    isApkPreview: Boolean
  ): AppDetailPackageSize {
    val baseSize = apkPreviewInfo?.packageSize
      ?: FileUtils.getFileSize(packageInfo.applicationInfo!!.sourceDir)
    val splitSizes = if (isApkPreview) {
      emptyList()
    } else {
      getAppBundleItems(packageInfo).map { it.size }
    }

    return AppDetailPackageSize(baseSize, splitSizes)
  }
}

data class AppDetailPackageSize(
  val baseSize: Long,
  val splitSizes: List<Long>
) {
  val totalSize: Long = baseSize + splitSizes.sum()
}
