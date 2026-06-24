package com.absinthe.libchecker.domain.app

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetInstalledAppComparisonPackageUseCase(
  private val installedAppRepository: InstalledAppRepository
) {

  suspend fun isAvailable(packageName: String): Boolean = withContext(Dispatchers.IO) {
    installedAppRepository.isPackageInstalled(packageName)
  }

  suspend operator fun invoke(packageName: String): PackageInfo? = withContext(Dispatchers.IO) {
    installedAppRepository.getPackageInfo(
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
