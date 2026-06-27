package com.absinthe.libchecker.domain.snapshot.timenode.usecase

import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.domain.snapshot.SnapshotLibraryUseCase
import com.absinthe.libchecker.domain.snapshot.SnapshotSettingsRepository

class UpdateSnapshotAutoRemoveThresholdUseCase(
  private val snapshotSettingsRepository: SnapshotSettingsRepository,
  private val snapshotLibraryUseCase: SnapshotLibraryUseCase
) {

  val currentThreshold: Int
    get() = snapshotSettingsRepository.autoRemoveThreshold

  fun disable() {
    snapshotSettingsRepository.autoRemoveThreshold = DISABLED_THRESHOLD
  }

  suspend fun enableAndRetainLatest(threshold: Int): List<TimeStampItem> {
    snapshotSettingsRepository.autoRemoveThreshold = threshold
    return snapshotLibraryUseCase.retainLatestSnapshotsAndGetTimeStamps(threshold)
  }

  private companion object {
    private const val DISABLED_THRESHOLD = -1
  }
}
