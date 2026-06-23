package com.absinthe.libchecker.domain.app

import com.absinthe.libchecker.compat.PackageManagerCompat
import com.absinthe.libchecker.constant.Constants

class CheckRequiredPackageAvailabilityUseCase(
  private val installedAppRepository: InstalledAppRepository
) {

  operator fun invoke(packageName: String): Boolean {
    return if (packageName.endsWith(Constants.TEMP_PACKAGE)) {
      runCatching {
        PackageManagerCompat.getPackageArchiveInfo(packageName, 0) != null
      }.getOrDefault(false)
    } else {
      installedAppRepository.getPackageInfo(packageName) != null
    }
  }
}
