package com.absinthe.libchecker.domain.app

import android.content.Context
import android.content.pm.PackageInfo
import com.absinthe.libchecker.constant.AndroidVersions
import com.absinthe.libchecker.utils.apk.ApkPreviewInfo
import com.absinthe.libchecker.utils.extensions.getCompileSdkVersion
import com.absinthe.libchecker.utils.extensions.getCompileSdkVersionString
import com.absinthe.libchecker.utils.extensions.getTargetApiString
import com.absinthe.libchecker.utils.extensions.sizeToString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BuildAppDetailHeaderExtraInfoUseCase(
  private val context: Context,
  private val getAppDetailPackageSize: GetAppDetailPackageSizeUseCase
) {

  suspend operator fun invoke(
    packageInfo: PackageInfo,
    apkPreviewInfo: ApkPreviewInfo?,
    isApkPreview: Boolean,
    showAndroidVersion: Boolean
  ): AppDetailHeaderExtraInfo = withContext(Dispatchers.IO) {
    val applicationInfo = packageInfo.applicationInfo!!
    val targetSdkVersion = apkPreviewInfo?.targetSdkVersion ?: applicationInfo.targetSdkVersion
    val minSdkVersion = apkPreviewInfo?.minSdkVersion ?: applicationInfo.minSdkVersion
    val compileSdkVersion = apkPreviewInfo?.compileSdkVersion ?: packageInfo.getCompileSdkVersion()
    val packageSize = getAppDetailPackageSize(packageInfo, apkPreviewInfo, isApkPreview)

    AppDetailHeaderExtraInfo(
      targetSdkInfo = formatSdkInfo(
        value = apkPreviewInfo?.targetSdkVersion?.toString() ?: packageInfo.getTargetApiString(),
        version = targetSdkVersion,
        showAndroidVersion = showAndroidVersion
      ),
      minSdkInfo = formatSdkInfo(
        value = minSdkVersion.toString(),
        version = minSdkVersion,
        showAndroidVersion = showAndroidVersion
      ),
      compileSdkInfo = formatSdkInfo(
        value = apkPreviewInfo?.compileSdkVersion?.toString() ?: packageInfo.getCompileSdkVersionString(),
        version = compileSdkVersion,
        showAndroidVersion = showAndroidVersion
      ),
      sizeInfo = formatPackageSize(packageSize),
      sharedUserId = packageInfo.sharedUserId
    )
  }

  private fun formatSdkInfo(
    value: String,
    version: Int,
    showAndroidVersion: Boolean
  ): String {
    return if (showAndroidVersion) {
      "$value (${AndroidVersions.simpleVersions[version]})"
    } else {
      value
    }
  }

  private fun formatPackageSize(packageSize: AppDetailPackageSize): String {
    val baseFormattedApkSize = packageSize.baseSize.sizeToString(context, showBytes = false)
    val splitApkSizeList = packageSize.splitSizes.map {
      it.sizeToString(context, showBytes = false)
    }
    if (splitApkSizeList.isEmpty()) {
      return baseFormattedApkSize
    }
    val totalSize = packageSize.totalSize.sizeToString(context, showBytes = false)
    return splitApkSizeList
      .toMutableList()
      .apply { add(0, baseFormattedApkSize) }
      .joinToString(
        separator = " + ",
        prefix = "(",
        postfix = " = $totalSize)"
      )
  }
}

data class AppDetailHeaderExtraInfo(
  val targetSdkInfo: String,
  val minSdkInfo: String,
  val compileSdkInfo: String,
  val sizeInfo: String,
  val sharedUserId: String?
)
