package com.absinthe.libchecker.domain.snapshot

import com.absinthe.libchecker.database.entity.SnapshotDiffStoringItem
import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.database.entity.TrackItem
import kotlinx.coroutines.flow.Flow

interface SnapshotRepository {
  val currentSnapshotCount: Flow<Int>

  fun getTimeStamps(): List<TimeStampItem>
  suspend fun getTimeStamp(timestamp: Long): TimeStampItem?
  suspend fun getSnapshots(timestamp: Long): List<SnapshotItem>
  suspend fun getSnapshot(timestamp: Long, packageName: String): SnapshotItem?
  suspend fun getSnapshotDiff(packageName: String): SnapshotDiffStoringItem?
  suspend fun getTrackItems(): List<TrackItem>
  suspend fun insertSnapshots(items: List<SnapshotItem>)
  suspend fun insertTimeStamp(item: TimeStampItem)
  suspend fun insertSnapshotDiff(item: SnapshotDiffStoringItem)
  suspend fun insertTrackItem(item: TrackItem)
  suspend fun updateTimeStamp(item: TimeStampItem)
  suspend fun deleteSnapshotsAndTimeStamp(timestamp: Long)
  suspend fun deleteTrackItem(item: TrackItem)
  suspend fun retainLatestSnapshots(count: Int)
  suspend fun deleteDuplicateSnapshotItems()
  suspend fun deleteSnapshotDiff(packageName: String)
  suspend fun deleteAllSnapshotDiffItems()
}
