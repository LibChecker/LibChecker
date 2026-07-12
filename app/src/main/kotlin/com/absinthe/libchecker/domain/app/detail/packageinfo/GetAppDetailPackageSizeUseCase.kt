package com.absinthe.libchecker.domain.app.detail.packageinfo

import android.content.pm.PackageInfo
import com.absinthe.libchecker.domain.app.detail.content.GetAppBundleItemsUseCase
import com.absinthe.libchecker.utils.FileUtils
import com.absinthe.libchecker.utils.apk.ApkPreviewInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetAppDetailPackageSizeUseCase(
  private val getAppBundleItems: GetAppBundleItemsUseCase
) {

  suspend operator fun invoke(
    packageInfo: PackageInfo,
    apkPreviewInfo: ApkPreviewInfo?,
    isApkPreview: Boolean
  ): AppDetailPackageSize = withContext(Dispatchers.IO) {
    val baseSize = apkPreviewInfo?.packageSize
      ?: FileUtils.getFileSize(packageInfo.applicationInfo!!.sourceDir)
    val splitSizes = if (isApkPreview) {
      emptyList()
    } else {
      getAppBundleItems(packageInfo).map { it.size }
    }

    AppDetailPackageSize(baseSize, splitSizes)
  }
}

data class AppDetailPackageSize(
  val baseSize: Long,
  val splitSizes: List<Long>
) {
  val totalSize: Long = baseSize + splitSizes.sum()
}
