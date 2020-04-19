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
}