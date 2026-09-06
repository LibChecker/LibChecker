package com.absinthe.libchecker.domain.snapshot.timenode.usecase

import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.domain.snapshot.SnapshotRepository
import com.absinthe.libchecker.domain.snapshot.SnapshotSettingsRepository

class UpdateSnapshotAutoRemoveThresholdUseCase(
  private val snapshotSettingsRepository: SnapshotSettingsRepository,
  private val snapshotRepository: SnapshotRepository
) {

  val currentThreshold: Int
    get() = snapshotSettingsRepository.autoRemoveThreshold

  fun disable() {
    snapshotSettingsRepository.autoRemoveThreshold = DISABLED_THRESHOLD
  }

  suspend fun enableAndRetainLatest(threshold: Int): List<TimeStampItem> {
    snapshotSettingsRepository.autoRemoveThreshold = threshold
    snapshotRepository.retainLatestSnapshots(threshold)
    return snapshotRepository.getTimeStamps()
  }

  private companion object {
    private const val DISABLED_THRESHOLD = -1
  }
}
