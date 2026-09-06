package com.absinthe.libchecker.data.snapshot

import android.database.sqlite.SQLiteBlobTooBigException
import com.absinthe.libchecker.database.LCDao
import com.absinthe.libchecker.database.entity.SnapshotDiffStoringItem
import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.database.entity.TrackItem
import com.absinthe.libchecker.domain.snapshot.SnapshotRepository
import com.absinthe.libchecker.domain.snapshot.selection.SnapshotSelectionRepository
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

class LocalSnapshotRepository(
  private val dao: LCDao,
  private val selectionRepository: SnapshotSelectionRepository
) : SnapshotRepository {

  override val currentSnapshotCount: Flow<Int> =
    dao.getSnapshotsCountFlow(selectionRepository.currentTimestamp)

  override suspend fun getTimeStamps(): List<TimeStampItem> {
    return dao.getTimeStamps()
  }

  override suspend fun getTimeStamp(timestamp: Long): TimeStampItem? {
    return dao.getTimeStamp(timestamp)
  }

  override suspend fun getSnapshotCountsByTimestamp(): Map<Long, Int> {
    return dao.getSnapshotCountsByTimestamp().associate { it.timestamp to it.count }
  }

  override suspend fun getSnapshots(timestamp: Long): List<SnapshotItem> {
    return try {
      dao.getSnapshots(timestamp)
    } catch (e: SQLiteBlobTooBigException) {
      Timber.w(e, "Snapshot rows are too large, fallback to summaries: $timestamp")
      getSnapshotSummaries(timestamp)
    }
  }

  override suspend fun getSnapshots(timestamp: Long, packageNames: List<String>): List<SnapshotItem> {
    if (packageNames.isEmpty()) return emptyList()
    return try {
      dao.getSnapshots(timestamp, packageNames)
    } catch (e: SQLiteBlobTooBigException) {
      // Preserve the per-row summary fallback for oversized archive rows.
      packageNames.mapNotNull { getSnapshot(timestamp, it) }
    }
  }

  override suspend fun getSnapshotSummaries(timestamp: Long): List<SnapshotItem> {
    return dao.getSnapshotSummaries(timestamp).map { it.toSnapshotItem() }
  }

  override suspend fun getSnapshot(timestamp: Long, packageName: String): SnapshotItem? {
    return try {
      dao.getSnapshot(timestamp, packageName)
    } catch (e: SQLiteBlobTooBigException) {
      Timber.w(e, "Snapshot row is too large, fallback to summary: $timestamp, $packageName")
      dao.getSnapshotSummary(timestamp, packageName)?.toSnapshotItem()
    }
  }

  override suspend fun getSnapshotDiff(packageName: String): SnapshotDiffStoringItem? {
    return dao.getSnapshotDiff(packageName)
  }

  override suspend fun getTrackItems(): List<TrackItem> {
    return dao.getTrackItems()
  }

  override suspend fun insertSnapshots(items: List<SnapshotItem>) {
    dao.insertSnapshots(items)
  }

  override suspend fun insertTimeStamp(item: TimeStampItem) {
    dao.insert(item)
  }

  override suspend fun insertSnapshotDiff(item: SnapshotDiffStoringItem) {
    dao.insertSnapshotDiff(item)
  }

  override suspend fun insertTrackItem(item: TrackItem) {
    dao.insert(item)
  }

  override suspend fun updateTimeStamp(item: TimeStampItem) {
    dao.update(item)
  }

  override suspend fun deleteSnapshotsAndTimeStamp(timestamp: Long) {
    dao.deleteSnapshots(timestamp)
    dao.deleteByTimeStamp(timestamp)
  }

  override suspend fun deleteTrackItem(item: TrackItem) {
    dao.delete(item)
  }

  override suspend fun retainLatestSnapshots(count: Int) {
    Timber.d("Retain latest $count snapshots and remove old")
    getTimeStamps().drop(count).forEach { deleteSnapshotsAndTimeStamp(it.timestamp) }
  }

  override suspend fun deleteDuplicateSnapshotItems() {
    dao.deleteDuplicateSnapshotItems()
  }

  override suspend fun deleteSnapshotDiff(packageName: String) {
    dao.deleteSnapshotDiff(packageName)
  }

  override suspend fun deleteAllSnapshotDiffItems() {
    dao.deleteAllSnapshotDiffItems()
  }
}
