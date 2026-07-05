package com.absinthe.libchecker.domain.app.maintenance

import com.absinthe.libchecker.compat.PackageManagerCompat
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository

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
