package com.absinthe.libchecker.domain.snapshot

import com.absinthe.libchecker.database.entity.TimeStampItem

class SnapshotSelectionUseCase(
  private val repository: SnapshotSelectionRepository
) {

  fun getCurrentTimestamp(): Long {
    return repository.currentTimestamp
  }

  fun setCurrentTimestamp(timestamp: Long) {
    repository.currentTimestamp = timestamp
  }

  fun selectLatestOrNone(timeStamps: List<TimeStampItem>) {
    setCurrentTimestamp(timeStamps.firstOrNull()?.timestamp ?: 0L)
  }
}
