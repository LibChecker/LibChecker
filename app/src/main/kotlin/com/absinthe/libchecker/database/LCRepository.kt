package com.absinthe.libchecker.database

import androidx.lifecycle.LiveData
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.database.entity.*

class LCRepository(private val lcDao: LCDao) {

    val allDatabaseItems: LiveData<List<LCItem>> = lcDao.getItems()
    val allSnapshotItems: LiveData<List<SnapshotItem>> = lcDao.getSnapshotsLiveData(GlobalValues.snapshotTimestamp)

    fun getItem(packageName: String): LCItem? {
        return lcDao.getItem(packageName)
    }

    suspend fun getSnapshots() = lcDao.getSnapshots()

    suspend fun getSnapshots(timestamp: Long) = lcDao.getSnapshots(timestamp)

    fun getTimeStamps(): List<TimeStampItem> = lcDao.getTimeStamps()

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
        lcDao.delete(TimeStampItem(timestamp))
    }

    fun deleteAllSnapshots() {
        lcDao.deleteAllSnapshots()
    }

    fun deleteAllItems() {
        lcDao.deleteAllItems()
    }

    suspend fun getRule(name: String) = lcDao.getRule(name)

    suspend fun insertRules(rules: List<RuleEntity>) {
        lcDao.insertRules(rules)
    }

    fun deleteAllRules() {
        lcDao.deleteAllRules()
    }

    suspend fun getAllRules() = lcDao.getAllRules()

    suspend fun getRegexRules() = lcDao.getRegexRules()

    suspend fun insertSnapshotDiffItems(item: SnapshotDiffStoringItem) {
        lcDao.insertSnapshotDiff(item)
    }

    suspend fun updateSnapshotDiff(item: SnapshotDiffStoringItem) {
        lcDao.updateSnapshotDiff(item)
    }

    fun deleteAllSnapshotDiffItems() {
        lcDao.deleteAllSnapshotDiffItems()
    }

    suspend fun getSnapshotDiff(packageName: String): SnapshotDiffStoringItem? = lcDao.getSnapshotDiff(packageName)
}