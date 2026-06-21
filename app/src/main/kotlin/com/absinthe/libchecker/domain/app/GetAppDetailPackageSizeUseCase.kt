package com.absinthe.libchecker.domain.app

import android.content.pm.PackageInfo
import com.absinthe.libchecker.utils.FileUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.apk.ApkPreviewInfo

class GetAppDetailPackageSizeUseCase {

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
      PackageUtils.getSplitsSourceDir(packageInfo)
        ?.map { FileUtils.getFileSize(it) }
        .orEmpty()
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
