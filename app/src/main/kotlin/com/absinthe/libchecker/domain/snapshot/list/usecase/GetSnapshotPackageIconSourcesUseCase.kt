package com.absinthe.libchecker.domain.snapshot.list.usecase

import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import com.absinthe.libchecker.domain.app.repository.PackageListLoadException
import com.absinthe.libchecker.domain.snapshot.model.SnapshotPackageIconSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetSnapshotPackageIconSourcesUseCase(
  private val installedAppRepository: InstalledAppRepository
) {

  suspend operator fun invoke(
    packageNames: Collection<String>
  ): Map<String, SnapshotPackageIconSource> = withContext(Dispatchers.IO) {
    val installedApps = try {
      installedAppRepository.getApplicationMap()
    } catch (_: PackageListLoadException) {
      emptyMap()
    }
    packageNames.asSequence()
      .distinct()
      .associateWith { packageName ->
        installedApps[packageName]
          ?.let(SnapshotPackageIconSource::InstalledPackage)
          ?: SnapshotPackageIconSource.Fallback
      }
  }
}
