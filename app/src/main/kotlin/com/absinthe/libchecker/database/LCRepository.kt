package com.absinthe.libchecker.database

import androidx.lifecycle.LiveData

class LCRepository(private val lcDao: LCDao) {
    val allItems: LiveData<List<LCItem>> = lcDao.getItems()

    suspend fun insert(item: LCItem) {
        lcDao.insert(item)
    }

    suspend fun update(item: LCItem) {
        lcDao.update(item)
    }
}