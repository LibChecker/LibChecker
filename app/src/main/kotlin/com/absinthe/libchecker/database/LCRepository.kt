package com.absinthe.libchecker.database

import android.database.sqlite.SQLiteBlobTooBigException
import android.view.ContextThemeWrapper
import androidx.appcompat.app.AlertDialog
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.database.entity.SnapshotDiffStoringItem
import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.database.entity.TrackItem
import com.absinthe.libchecker.utils.UiUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import timber.log.Timber

class LCRepository(private val lcDao: LCDao) {
  val allLCItemsFlow: Flow<List<LCItem>> = lcDao.getItemsFlow()

  fun getSnapshotsCountFlow(timestamp: Long): Flow<Int> {
    return lcDao.getSnapshotsCountFlow(timestamp)
  }

  suspend fun getLCItems(): List<LCItem> = lcDao.getItems()

  suspend fun getUninitializedFeaturePackageNames(): List<String> {
    return lcDao.getUninitializedFeaturePackageNames()
  }

  suspend fun getItem(packageName: String): LCItem? {
    return lcDao.getItem(packageName)
  }

  suspend fun getSnapshots(): List<SnapshotItem> {
    return try {
      lcDao.getSnapshots()
    } catch (e: SQLiteBlobTooBigException) {
      Timber.w(e, "Snapshot rows are too large, fallback to summaries")
      lcDao.getSnapshotSummaries().map { it.toSnapshotItem() }
    }
  }

  suspend fun getSnapshots(timestamp: Long): List<SnapshotItem> {
    return try {
      lcDao.getSnapshots(timestamp)
    } catch (e: SQLiteBlobTooBigException) {
      Timber.w(e, "Snapshot rows are too large, fallback to summaries: $timestamp")
      lcDao.getSnapshotSummaries(timestamp).map { it.toSnapshotItem() }
    }
  }

  suspend fun getSnapshot(timestamp: Long, packageName: String): SnapshotItem? {
    return try {
      lcDao.getSnapshot(timestamp, packageName)
    } catch (e: SQLiteBlobTooBigException) {
      Timber.w(e, "Snapshot row is too large, fallback to summary: $timestamp, $packageName")
      lcDao.getSnapshotSummary(timestamp, packageName)?.toSnapshotItem()
    }
  }

  suspend fun getTimeStamps(): List<TimeStampItem> = lcDao.getTimeStamps()

  suspend fun getTimeStamp(timestamp: Long): TimeStampItem? = lcDao.getTimeStamp(timestamp)

  suspend fun getTrackItems(): List<TrackItem> = lcDao.getTrackItems()

  suspend fun insert(item: LCItem) {
    lcDao.insert(item)
  }

  suspend fun insert(list: List<LCItem>) {
    lcDao.insert(list)
  }

  suspend fun insert(item: SnapshotItem) {
    lcDao.insert(item)
  }

  suspend fun insert(item: TrackItem) {
    lcDao.insert(item)
  }

  suspend fun insert(item: TimeStampItem) {
    lcDao.insert(item)
  }

  suspend fun insertSnapshots(items: List<SnapshotItem>) {
    lcDao.insertSnapshots(items)
  }

  suspend fun update(item: LCItem) {
    lcDao.update(item)
  }

  suspend fun update(item: SnapshotItem) {
    lcDao.update(item)
  }

  suspend fun update(items: List<SnapshotItem>) {
    lcDao.update(items)
  }

  suspend fun delete(item: SnapshotItem) {
    lcDao.delete(item)
  }

  suspend fun delete(item: LCItem) {
    lcDao.delete(item)
  }

  suspend fun deleteLCItemByPackageName(packageName: String) {
    lcDao.deleteLCItemByPackageName(packageName)
  }

  suspend fun delete(item: TrackItem) {
    lcDao.delete(item)
  }

  suspend fun deleteSnapshotsAndTimeStamp(timestamp: Long) {
    val list = getSnapshots(timestamp)
    var count = 0
    val chunk = mutableListOf<SnapshotItem>()

    list.forEach {
      chunk.add(it)
      count++

      if (count == 50) {
        lcDao.deleteSnapshots(chunk)
        chunk.clear()
        count = 0
      }
    }

    lcDao.deleteSnapshots(chunk)
    chunk.clear()
    lcDao.deleteByTimeStamp(timestamp)
  }

  suspend fun retainLatestSnapshotsAndRemoveOld(count: Int, forceShowLoading: Boolean, context: ContextThemeWrapper? = null) {
    Timber.d("Retain latest $count snapshots and remove old")
    var loadingDialog: AlertDialog? = null
    if (forceShowLoading) {
      withContext(Dispatchers.Main) {
        loadingDialog = UiUtils.createLoadingDialog(context!!)
        loadingDialog.show()
      }
    }
    getTimeStamps()
      .sortedBy { it.timestamp }
      .reversed()
      .drop(count)
      .forEach {
        deleteSnapshotsAndTimeStamp(it.timestamp)
      }
    if (forceShowLoading) {
      withContext(Dispatchers.Main) {
        loadingDialog?.dismiss()
      }
    }
  }

  suspend fun updateTimeStampItem(item: TimeStampItem) {
    lcDao.update(item)
  }

  suspend fun deleteDuplicateSnapshotItems() {
    lcDao.deleteDuplicateSnapshotItems()
  }

  suspend fun deleteAllSnapshots() {
    lcDao.deleteAllSnapshots()
  }

  suspend fun deleteAllItems() {
    lcDao.deleteAllItems()
  }

  suspend fun insertSnapshotDiffItems(item: SnapshotDiffStoringItem) {
    lcDao.insertSnapshotDiff(item)
  }

  suspend fun updateSnapshotDiff(item: SnapshotDiffStoringItem) {
    lcDao.updateSnapshotDiff(item)
  }

  suspend fun deleteSnapshotDiff(packageName: String) {
    lcDao.deleteSnapshotDiff(packageName)
  }

  suspend fun deleteAllSnapshotDiffItems() {
    lcDao.deleteAllSnapshotDiffItems()
  }

  suspend fun getSnapshotDiff(packageName: String): SnapshotDiffStoringItem? = lcDao.getSnapshotDiff(packageName)

  suspend fun updateFeatures(packageName: String, features: Int) {
    lcDao.updateFeatures(packageName, features)
  }

  suspend fun updateFeatures(map: Map<String, Int>) {
    lcDao.updateFeatures(map)
  }
}
