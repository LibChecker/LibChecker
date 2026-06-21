package com.absinthe.libchecker.domain.snapshot

import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.database.entity.TimeStampItem

interface SnapshotRepository {
  fun getTimeStamps(): List<TimeStampItem>
  suspend fun getTimeStamp(timestamp: Long): TimeStampItem?
  suspend fun getSnapshots(timestamp: Long): List<SnapshotItem>
  suspend fun getSnapshot(timestamp: Long, packageName: String): SnapshotItem?
  suspend fun insertSnapshots(items: List<SnapshotItem>)
  suspend fun insertTimeStamp(item: TimeStampItem)
  suspend fun deleteSnapshotsAndTimeStamp(timestamp: Long)
  suspend fun retainLatestSnapshots(count: Int)
  suspend fun deleteDuplicateSnapshotItems()
  suspend fun deleteAllSnapshotDiffItems()
}
