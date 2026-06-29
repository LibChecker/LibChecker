package com.absinthe.libchecker.domain.snapshot.track.usecase

import com.absinthe.libchecker.database.entity.TrackItem
import com.absinthe.libchecker.domain.snapshot.SnapshotRepository
import com.absinthe.libchecker.domain.snapshot.track.repository.SnapshotTrackChangeRepository

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
