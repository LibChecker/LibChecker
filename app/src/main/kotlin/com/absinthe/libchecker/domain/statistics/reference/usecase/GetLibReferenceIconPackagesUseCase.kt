package com.absinthe.libchecker.domain.statistics.reference.usecase

import android.content.pm.PackageInfo
import com.absinthe.libchecker.domain.app.InstalledAppRepository

class GetLibReferenceIconPackagesUseCase(
  private val installedAppRepository: InstalledAppRepository
) {

  operator fun invoke(
    packageNames: Iterable<String>,
    packageInfoByName: Map<String, PackageInfo>,
    limit: Int = DEFAULT_LIMIT
  ): List<PackageInfo> {
    return packageNames.asSequence()
      .distinct()
      .mapNotNull { packageInfoByName[it] ?: installedAppRepository.getPackageInfo(it) }
      .take(limit)
      .toList()
  }

  private companion object {
    const val DEFAULT_LIMIT = 4
  }
}
