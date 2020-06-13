package com.absinthe.libchecker.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface LCDao {

    @Query("SELECT * from item_table ORDER BY label ASC")
    fun getItems(): LiveData<List<LCItem>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: LCItem)

    @Update
    suspend fun update(item: LCItem)

    @Delete
    suspend fun delete(item: LCItem)

    @Query("DELETE FROM item_table")
    fun deleteAll()

    @Query("SELECT * from snapshot_table ORDER BY packageName ASC")
    fun getSnapshots(): LiveData<List<SnapshotItem>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: SnapshotItem)

    @Update
    suspend fun update(item: SnapshotItem)

    @Delete
    suspend fun delete(item: SnapshotItem)
}