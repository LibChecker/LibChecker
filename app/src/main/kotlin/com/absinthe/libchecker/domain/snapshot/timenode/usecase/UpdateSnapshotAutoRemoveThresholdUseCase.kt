package com.absinthe.libchecker.domain.snapshot.timenode.usecase

import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.domain.snapshot.SnapshotSettingsRepository
import com.absinthe.libchecker.domain.snapshot.library.SnapshotLibrary

class UpdateSnapshotAutoRemoveThresholdUseCase(
  private val snapshotSettingsRepository: SnapshotSettingsRepository,
  private val snapshotLibrary: SnapshotLibrary
) {

  val currentThreshold: Int
    get() = snapshotSettingsRepository.autoRemoveThreshold

  fun disable() {
    snapshotSettingsRepository.autoRemoveThreshold = DISABLED_THRESHOLD
  }

  suspend fun enableAndRetainLatest(threshold: Int): List<TimeStampItem> {
    snapshotSettingsRepository.autoRemoveThreshold = threshold
    return snapshotLibrary.retainLatestSnapshotsAndGetTimeStamps(threshold)
  }

  private companion object {
    private const val DISABLED_THRESHOLD = -1
  }
}
