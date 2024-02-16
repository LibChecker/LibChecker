package com.absinthe.libchecker.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.database.entity.SnapshotDiffStoringItem
import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.database.entity.TrackItem
import kotlinx.coroutines.flow.Flow

@Dao
interface LCDao {

  // Item Table
  @Query("SELECT * from item_table ORDER BY label ASC")
  fun getItemsFlow(): Flow<List<LCItem>>

  @Query("SELECT * from item_table ORDER BY label ASC")
  suspend fun getItems(): List<LCItem>

  @Query("SELECT * from item_table WHERE packageName LIKE :packageName")
  suspend fun getItem(packageName: String): LCItem?

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insert(item: LCItem)

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insert(list: List<LCItem>)

  @Update
  suspend fun update(item: LCItem)

  @Delete
  suspend fun delete(item: LCItem)

  @Query("DELETE FROM item_table WHERE packageName = :packageName")
  fun deleteLCItemByPackageName(packageName: String)

  @Query("DELETE FROM item_table")
  fun deleteAllItems()

  @Query("UPDATE item_table SET features = :features WHERE packageName = :packageName")
  fun updateFeatures(packageName: String, features: Int)

  @Transaction
  fun updateFeatures(map: Map<String, Int>) {
    map.forEach { updateFeatures(it.key, it.value) }
  }

  // Snapshot Table
  @Transaction
  @Query("SELECT * from snapshot_table ORDER BY packageName ASC")
  suspend fun getSnapshots(): List<SnapshotItem>

  @Transaction
  @Query("SELECT * from snapshot_table WHERE timeStamp LIKE :timestamp ORDER BY packageName ASC")
  suspend fun getSnapshots(timestamp: Long): List<SnapshotItem>

  @Query("SELECT * from snapshot_table WHERE timeStamp LIKE :timestamp AND packageName LIKE :packageName ORDER BY packageName ASC")
  suspend fun getSnapshot(timestamp: Long, packageName: String): SnapshotItem?

  @Transaction
  @Query("SELECT * from snapshot_table WHERE timeStamp LIKE :timestamp ORDER BY packageName ASC")
  fun getSnapshotsFlow(timestamp: Long): Flow<List<SnapshotItem>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(item: SnapshotItem)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSnapshots(items: List<SnapshotItem>)

  @Update
  suspend fun update(item: SnapshotItem)

  @Update
  suspend fun update(items: List<SnapshotItem>)

  @Delete
  suspend fun delete(item: SnapshotItem)

  @Transaction
  @Query("DELETE FROM snapshot_table WHERE id NOT IN (SELECT id FROM snapshot_table GROUP BY packageName, timeStamp, versionCode, lastUpdatedTime, packageSize)")
  suspend fun deleteDuplicateSnapshotItems()

  @Transaction
  @Query("DELETE FROM snapshot_table")
  fun deleteAllSnapshots()

  @Transaction
  @Query("DELETE FROM snapshot_table WHERE timeStamp = :timestamp")
  fun deleteSnapshots(timestamp: Long)

  @Transaction
  @Delete
  fun deleteSnapshots(list: List<SnapshotItem>)

  // TimeStamp Table
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(item: TimeStampItem)

  @Query("SELECT * from timestamp_table ORDER BY timestamp DESC")
  fun getTimeStamps(): List<TimeStampItem>

  @Delete
  fun delete(item: TimeStampItem)

  @Query("DELETE from timestamp_table WHERE timestamp LIKE :timestamp")
  suspend fun deleteByTimeStamp(timestamp: Long)

  @Update
  suspend fun update(item: TimeStampItem)

  // Track table
  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insert(item: TrackItem)

  @Delete
  suspend fun delete(item: TrackItem)

  @Query("SELECT * from track_table")
  suspend fun getTrackItems(): List<TrackItem>

  // Diff
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSnapshotDiff(item: SnapshotDiffStoringItem)

  @Update
  suspend fun updateSnapshotDiff(item: SnapshotDiffStoringItem)

  @Query("DELETE FROM diff_table WHERE packageName = :packageName")
  suspend fun deleteSnapshotDiff(packageName: String)

  @Query("DELETE FROM diff_table")
  fun deleteAllSnapshotDiffItems()

  @Query("SELECT * from diff_table WHERE packageName LIKE :packageName")
  suspend fun getSnapshotDiff(packageName: String): SnapshotDiffStoringItem?
}
