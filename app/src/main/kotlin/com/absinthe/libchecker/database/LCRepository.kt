package com.absinthe.libchecker.database

import android.view.ContextThemeWrapper
import androidx.appcompat.app.AlertDialog
import com.absinthe.libchecker.constant.GlobalValues
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
  val allSnapshotItemsFlow: Flow<List<SnapshotItem>> =
    lcDao.getSnapshotsFlow(GlobalValues.snapshotTimestamp)

  private fun checkDatabaseStatus(): Boolean {
    if (LCDatabase.isClosed()) {
      Timber.w("Database is closed")
      return false
    }
    return true
  }

  suspend fun getLCItems(): List<LCItem> = lcDao.getItems()

  suspend fun getItem(packageName: String): LCItem? {
    if (checkDatabaseStatus().not()) return null
    return lcDao.getItem(packageName)
  }

  suspend fun getSnapshots() = lcDao.getSnapshots()

  suspend fun getSnapshots(timestamp: Long) = lcDao.getSnapshots(timestamp)

  suspend fun getSnapshot(timestamp: Long, packageName: String) = lcDao.getSnapshot(timestamp, packageName)

  fun getTimeStamps(): List<TimeStampItem> = lcDao.getTimeStamps()

  suspend fun getTrackItems(): List<TrackItem> = lcDao.getTrackItems()

  suspend fun insert(item: LCItem) {
    if (checkDatabaseStatus().not()) return
    lcDao.insert(item)
  }

  suspend fun insert(list: List<LCItem>) {
    if (checkDatabaseStatus().not()) return
    lcDao.insert(list)
  }

  suspend fun insert(item: SnapshotItem) {
    if (checkDatabaseStatus().not()) return
    lcDao.insert(item)
  }

  suspend fun insert(item: TrackItem) {
    if (checkDatabaseStatus().not()) return
    lcDao.insert(item)
  }

  suspend fun insert(item: TimeStampItem) {
    if (checkDatabaseStatus().not()) return
    lcDao.insert(item)
  }

  suspend fun insertSnapshots(items: List<SnapshotItem>) {
    if (checkDatabaseStatus().not()) return
    lcDao.insertSnapshots(items)
  }

  suspend fun update(item: LCItem) {
    if (checkDatabaseStatus().not()) return
    lcDao.update(item)
  }

  suspend fun update(item: SnapshotItem) {
    if (checkDatabaseStatus().not()) return
    lcDao.update(item)
  }

  suspend fun update(items: List<SnapshotItem>) {
    if (checkDatabaseStatus().not()) return
    lcDao.update(items)
  }

  suspend fun delete(item: SnapshotItem) {
    if (checkDatabaseStatus().not()) return
    lcDao.delete(item)
  }

  suspend fun delete(item: LCItem) {
    if (checkDatabaseStatus().not()) return
    lcDao.delete(item)
  }

  fun deleteLCItemByPackageName(packageName: String) {
    if (checkDatabaseStatus().not()) return
    lcDao.deleteLCItemByPackageName(packageName)
  }

  suspend fun delete(item: TrackItem) {
    if (checkDatabaseStatus().not()) return
    lcDao.delete(item)
  }

  suspend fun deleteSnapshotsAndTimeStamp(timestamp: Long) {
    if (checkDatabaseStatus().not()) return
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
    if (checkDatabaseStatus().not()) return
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
    if (checkDatabaseStatus().not()) return
    lcDao.update(item)
  }

  suspend fun deleteDuplicateSnapshotItems() {
    if (checkDatabaseStatus().not()) return
    lcDao.deleteDuplicateSnapshotItems()
  }

  fun deleteAllSnapshots() {
    if (checkDatabaseStatus().not()) return
    lcDao.deleteAllSnapshots()
  }

  fun deleteAllItems() {
    if (checkDatabaseStatus().not()) return
    lcDao.deleteAllItems()
  }

  suspend fun insertSnapshotDiffItems(item: SnapshotDiffStoringItem) {
    if (checkDatabaseStatus().not()) return
    lcDao.insertSnapshotDiff(item)
  }

  suspend fun updateSnapshotDiff(item: SnapshotDiffStoringItem) {
    if (checkDatabaseStatus().not()) return
    lcDao.updateSnapshotDiff(item)
  }

  suspend fun deleteSnapshotDiff(packageName: String) {
    if (checkDatabaseStatus().not()) return
    lcDao.deleteSnapshotDiff(packageName)
  }

  fun deleteAllSnapshotDiffItems() {
    if (checkDatabaseStatus().not()) return
    lcDao.deleteAllSnapshotDiffItems()
  }

  suspend fun getSnapshotDiff(packageName: String): SnapshotDiffStoringItem? = lcDao.getSnapshotDiff(packageName)

  fun updateFeatures(packageName: String, features: Int) {
    if (checkDatabaseStatus().not()) return
    lcDao.updateFeatures(packageName, features)
  }

  fun updateFeatures(map: Map<String, Int>) {
    if (checkDatabaseStatus().not()) return
    lcDao.updateFeatures(map)
  }
}
