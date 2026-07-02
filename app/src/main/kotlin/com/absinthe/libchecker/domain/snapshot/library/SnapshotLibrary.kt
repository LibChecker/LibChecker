package com.absinthe.libchecker.domain.snapshot.library

import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.domain.snapshot.SnapshotRepository

class SnapshotLibrary(
  private val repository: SnapshotRepository
) {

  suspend fun getTimeStamps(): List<TimeStampItem> {
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
