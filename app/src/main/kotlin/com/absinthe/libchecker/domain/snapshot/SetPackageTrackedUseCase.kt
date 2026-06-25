package com.absinthe.libchecker.domain.snapshot

import com.absinthe.libchecker.database.entity.TrackItem

class SetPackageTrackedUseCase(
  private val snapshotRepository: SnapshotRepository,
  private val snapshotTrackChangeRepository: SnapshotTrackChangeRepository
) {

  suspend operator fun invoke(packageName: String, tracked: Boolean) {
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
