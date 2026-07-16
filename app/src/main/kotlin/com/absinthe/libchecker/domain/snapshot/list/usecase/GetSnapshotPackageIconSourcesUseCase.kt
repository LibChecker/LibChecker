package com.absinthe.libchecker.domain.snapshot.list.usecase

import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import com.absinthe.libchecker.domain.snapshot.model.SnapshotPackageIconSource
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
