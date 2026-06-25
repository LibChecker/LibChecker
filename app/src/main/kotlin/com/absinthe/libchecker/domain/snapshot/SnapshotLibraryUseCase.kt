package com.absinthe.libchecker.domain.snapshot

import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.database.entity.TimeStampItem

class SnapshotLibraryUseCase(
  private val repository: SnapshotRepository
) {

  fun getTimeStamps(): List<TimeStampItem> {
    return repository.getTimeStamps()
  }

  suspend fun getSnapshots(timestamp: Long, packageName: String? = null): List<SnapshotItem> {
    val snapshots = repository.getSnapshots(timestamp)
    return packageName?.let { targetPackage ->
      snapshots.filter { it.packageName == targetPackage }
    } ?: snapshots
  }

  suspend fun deleteTimeStamp(timestamp: Long) {
    repository.deleteSnapshotsAndTimeStamp(timestamp)
  }

  suspend fun retainLatestSnapshotsAndGetTimeStamps(count: Int): List<TimeStampItem> {
    repository.retainLatestSnapshots(count)
    return repository.getTimeStamps()
  }
}
