package com.absinthe.libchecker.domain.snapshot

import android.content.pm.PackageInfo
import com.absinthe.libchecker.domain.app.InstalledAppRepository

class GetSnapshotPackageIconSourcesUseCase(
  private val installedAppRepository: InstalledAppRepository
) {

  operator fun invoke(packageNames: Collection<String>): Map<String, SnapshotPackageIconSource> {
    return packageNames.asSequence()
      .distinct()
      .associateWith { packageName ->
        installedAppRepository.getPackageInfo(packageName)
          ?.let(SnapshotPackageIconSource::InstalledPackage)
          ?: SnapshotPackageIconSource.Fallback
      }
  }
}

sealed interface SnapshotPackageIconSource {
  data class InstalledPackage(val packageInfo: PackageInfo) : SnapshotPackageIconSource
  data object Fallback : SnapshotPackageIconSource
}
