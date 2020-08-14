package com.absinthe.libchecker.database

import androidx.lifecycle.LiveData

class LCRepository(private val lcDao: LCDao) {

    val allItems: LiveData<List<LCItem>> = lcDao.getItems()
    val allSnapshotItems: LiveData<List<SnapshotItem>> = lcDao.getSnapshots()

    suspend fun insert(item: LCItem) {
        lcDao.insert(item)
    }

    suspend fun update(item: LCItem) {
        lcDao.update(item)
    }

    suspend fun delete(item: LCItem) {
        lcDao.delete(item)
    }

    fun deleteAllItems() {
        lcDao.deleteAllItems()
    }

    fun getItem(packageName: String): LCItem? {
        return lcDao.getItem(packageName)
    }

    suspend fun insert(item: SnapshotItem) {
        lcDao.insert(item)
    }

    suspend fun insertSnapshots(items: List<SnapshotItem>) {
        lcDao.insertSnapshots(items)
    }

    suspend fun update(item: SnapshotItem) {
        lcDao.update(item)
    }

    suspend fun delete(item: SnapshotItem) {
        lcDao.delete(item)
    }

    fun deleteAllSnapshots() {
        lcDao.deleteAllSnapshots()
    }
}