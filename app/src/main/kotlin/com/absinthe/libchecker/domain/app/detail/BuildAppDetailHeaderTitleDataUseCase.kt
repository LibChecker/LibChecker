package com.absinthe.libchecker.domain.app.detail

import android.content.Context
import android.content.pm.PackageInfo
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.InstalledAppRepository
import com.absinthe.libchecker.utils.apk.ApkPreviewInfo
import com.absinthe.libchecker.utils.extensions.getAppName
import com.absinthe.libchecker.utils.extensions.getVersionString

class BuildAppDetailHeaderTitleDataUseCase(
  private val context: Context,
  private val installedAppRepository: InstalledAppRepository
) {

  operator fun invoke(
    packageInfo: PackageInfo,
    apkPreviewInfo: ApkPreviewInfo?,
    apkAnalyticsMode: Boolean
  ): AppDetailHeaderTitleData {
    val packageName = apkPreviewInfo?.packageName ?: packageInfo.packageName
    val appName = apkPreviewInfo
      ?.let { context.getString(R.string.apk_preview) }
      ?: packageInfo.getAppName(context.packageManager)
    val title = appName ?: context.getString(R.string.detail_label)

    return AppDetailHeaderTitleData(
      packageName = packageName,
      appName = appName,
      title = title,
      versionInfo = apkPreviewInfo
        ?.let { "${it.versionName} (${it.versionCode})" }
        ?: packageInfo.getVersionString(),
      isAppInfoAvailable = !apkAnalyticsMode || installedAppRepository.isPackageInstalled(packageName)
    )
  }
}

data class AppDetailHeaderTitleData(
  val packageName: String,
  val appName: String?,
  val title: String,
  val versionInfo: String,
  val isAppInfoAvailable: Boolean
)
