package com.absinthe.libchecker.domain.snapshot

import android.content.pm.PackageInfo
import com.absinthe.libchecker.domain.app.InstalledAppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetSnapshotPackageIconSourcesUseCase(
  private val installedAppRepository: InstalledAppRepository
) {

  suspend operator fun invoke(
    packageNames: Collection<String>
  ): Map<String, SnapshotPackageIconSource> = withContext(Dispatchers.IO) {
    packageNames.asSequence()
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
