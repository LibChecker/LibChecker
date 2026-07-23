package com.absinthe.libchecker.domain.snapshot.track.presentation

import android.content.pm.PackageManager
import com.absinthe.libchecker.database.entity.TrackItem
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import com.absinthe.libchecker.domain.snapshot.SnapshotRepository
import com.absinthe.libchecker.domain.snapshot.track.model.TrackedAppListItem
import com.absinthe.libchecker.domain.snapshot.track.model.buildTrackedAppListItemDescription
import com.absinthe.libchecker.domain.snapshot.track.repository.SnapshotTrackChangeRepository
import com.absinthe.libchecker.utils.extensions.getAppName

class TrackWorkflow(
  private val packageManager: PackageManager,
  private val installedAppRepository: InstalledAppRepository,
  private val snapshotRepository: SnapshotRepository,
  private val snapshotTrackChangeRepository: SnapshotTrackChangeRepository
) {
  suspend fun getItems(): List<TrackedAppListItem> {
    val trackedPackageNames = snapshotRepository.getTrackItems()
      .asSequence()
      .map { it.packageName }
      .toSet()
    return installedAppRepository.getApplicationList()
      .asSequence()
      .map {
        val label = it.getAppName(packageManager).toString()
        TrackedAppListItem(
          packageInfo = it,
          label = label,
          packageName = it.packageName,
          description = buildTrackedAppListItemDescription(label, it.packageName),
          switchState = it.packageName in trackedPackageNames
        )
      }
      .sortedByDescending { it.switchState }
      .toList()
  }

  suspend fun setPackageTracked(packageName: String, tracked: Boolean) {
    snapshotTrackChangeRepository.markChanged()
    val item = TrackItem(packageName)
    if (tracked) {
      snapshotRepository.insertTrackItem(item)
    } else {
      snapshotRepository.deleteTrackItem(item)
      snapshotRepository.deleteSnapshotDiff(packageName)
    }
  }
}
