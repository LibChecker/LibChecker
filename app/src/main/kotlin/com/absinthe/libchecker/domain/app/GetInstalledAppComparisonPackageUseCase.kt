package com.absinthe.libchecker.domain.app

import android.content.pm.PackageInfo
import android.content.pm.PackageManager

class GetInstalledAppComparisonPackageUseCase(
  private val installedAppRepository: InstalledAppRepository
) {

  fun isAvailable(packageName: String): Boolean {
    return installedAppRepository.isPackageInstalled(packageName)
  }

  operator fun invoke(packageName: String): PackageInfo? {
    return installedAppRepository.getPackageInfo(
      packageName = packageName,
      flags = PackageManager.GET_ACTIVITIES
        or PackageManager.GET_RECEIVERS
        or PackageManager.GET_SERVICES
        or PackageManager.GET_PROVIDERS
        or PackageManager.GET_META_DATA
        or PackageManager.GET_PERMISSIONS
    )
  }
}
