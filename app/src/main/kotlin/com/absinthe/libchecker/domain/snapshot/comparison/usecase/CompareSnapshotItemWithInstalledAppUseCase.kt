package com.absinthe.libchecker.domain.snapshot.comparison.usecase

import android.content.pm.PackageManager
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import com.absinthe.libchecker.domain.snapshot.SnapshotItemFactory
import com.absinthe.libchecker.domain.snapshot.SnapshotRepository
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem

class CompareSnapshotItemWithInstalledAppUseCase(
  private val packageManager: PackageManager,
  private val snapshotRepository: SnapshotRepository,
  private val installedAppRepository: InstalledAppRepository,
  private val snapshotItemFactory: SnapshotItemFactory,
  private val compareSnapshotItems: CompareSnapshotItemsUseCase
) {

  suspend operator fun invoke(
    timestamp: Long,
    packageName: String
  ): SnapshotDiffItem? {
    val presentInfo = installedAppRepository.getApplicationMap(forceUpdate = true)[packageName]
      ?.let { snapshotItemFactory.create(packageManager, it) }
    val snapshotInfo = snapshotRepository.getSnapshot(timestamp, packageName)
    val trackPackageNames = snapshotRepository.getTrackItems()
      .asSequence()
      .map { it.packageName }
      .toSet()

    return compareSnapshotItems(snapshotInfo, presentInfo, trackPackageNames)
  }
}
