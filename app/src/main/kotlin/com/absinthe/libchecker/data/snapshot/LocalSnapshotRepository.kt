package com.absinthe.libchecker.data.snapshot

import com.absinthe.libchecker.database.LCRepository
import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.database.entity.TrackItem
import com.absinthe.libchecker.domain.snapshot.SnapshotRepository

class LocalSnapshotRepository(
  private val repository: LCRepository
) : SnapshotRepository {

  override fun getTimeStamps(): List<TimeStampItem> {
    return repository.getTimeStamps()
  }

  override suspend fun getTimeStamp(timestamp: Long): TimeStampItem? {
    return repository.getTimeStamp(timestamp)
  }

  override suspend fun getSnapshots(timestamp: Long): List<SnapshotItem> {
    return repository.getSnapshots(timestamp)
  }

  override suspend fun getSnapshot(timestamp: Long, packageName: String): SnapshotItem? {
    return repository.getSnapshot(timestamp, packageName)
  }

  override suspend fun getTrackItems(): List<TrackItem> {
    return repository.getTrackItems()
  }

  override suspend fun insertSnapshots(items: List<SnapshotItem>) {
    repository.insertSnapshots(items)
  }

  override suspend fun insertTimeStamp(item: TimeStampItem) {
    repository.insert(item)
  }

  override suspend fun insertTrackItem(item: TrackItem) {
    repository.insert(item)
  }

  override suspend fun deleteSnapshotsAndTimeStamp(timestamp: Long) {
    repository.deleteSnapshotsAndTimeStamp(timestamp)
  }

  override suspend fun deleteTrackItem(item: TrackItem) {
    repository.delete(item)
  }

  override suspend fun retainLatestSnapshots(count: Int) {
    repository.retainLatestSnapshotsAndRemoveOld(
      count = count,
      forceShowLoading = false
    )
  }

  override suspend fun deleteDuplicateSnapshotItems() {
    repository.deleteDuplicateSnapshotItems()
  }

  override suspend fun deleteSnapshotDiff(packageName: String) {
    repository.deleteSnapshotDiff(packageName)
  }

  override suspend fun deleteAllSnapshotDiffItems() {
    repository.deleteAllSnapshotDiffItems()
  }
}
