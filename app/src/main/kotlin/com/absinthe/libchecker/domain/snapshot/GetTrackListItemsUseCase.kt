package com.absinthe.libchecker.domain.snapshot

import android.content.pm.PackageManager
import com.absinthe.libchecker.domain.app.InstalledAppRepository
import com.absinthe.libchecker.utils.extensions.getAppName

class GetTrackListItemsUseCase(
  private val packageManager: PackageManager,
  private val installedAppRepository: InstalledAppRepository,
  private val snapshotRepository: SnapshotRepository
) {

  suspend operator fun invoke(): List<TrackedAppListItem> {
    val trackedPackageNames = snapshotRepository.getTrackItems()
      .asSequence()
      .map { it.packageName }
      .toSet()

    return installedAppRepository.getApplicationList()
      .asSequence()
      .map {
        TrackedAppListItem(
          packageInfo = it,
          label = it.getAppName(packageManager).toString(),
          packageName = it.packageName,
          switchState = it.packageName in trackedPackageNames
        )
      }
      .sortedByDescending { it.switchState }
      .toList()
  }
}
