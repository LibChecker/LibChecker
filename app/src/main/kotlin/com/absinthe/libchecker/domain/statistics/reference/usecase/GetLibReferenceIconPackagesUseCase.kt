package com.absinthe.libchecker.domain.statistics.reference.usecase

import android.content.pm.PackageInfo
import com.absinthe.libchecker.domain.app.InstalledAppRepository

class GetLibReferenceIconPackagesUseCase(
  private val installedAppRepository: InstalledAppRepository
) {

  operator fun invoke(
    packageNames: Iterable<String>,
    limit: Int = DEFAULT_LIMIT
  ): List<PackageInfo> {
    return packageNames.asSequence()
      .distinct()
      .mapNotNull(installedAppRepository::getPackageInfo)
      .take(limit)
      .toList()
  }

  private companion object {
    const val DEFAULT_LIMIT = 4
  }
}
