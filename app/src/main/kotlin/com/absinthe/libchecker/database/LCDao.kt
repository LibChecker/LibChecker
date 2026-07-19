package com.absinthe.libchecker.database

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import androidx.room3.Upsert
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.database.entity.SnapshotDiffStoringItem
import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.database.entity.SnapshotSummaryItem
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

  @Query("SELECT packageName from item_table WHERE features = -1")
  suspend fun getUninitializedFeaturePackageNames(): List<String>

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
  suspend fun deleteLCItemByPackageName(packageName: String)

  @Query("DELETE FROM item_table")
  suspend fun deleteAllItems()

  @Query("UPDATE item_table SET features = :features WHERE packageName = :packageName")
  suspend fun updateFeatures(packageName: String, features: Int)

  @Transaction
  suspend fun updateFeatures(map: Map<String, Int>) {
    map.forEach { updateFeatures(it.key, it.value) }
  }

  // Snapshot Table
  @Transaction
  @Query("SELECT * from snapshot_table ORDER BY packageName ASC")
  suspend fun getSnapshots(): List<SnapshotItem>

  @Query("SELECT id, packageName, timeStamp, label, versionName, versionCode, isArchived, installedTime, lastUpdatedTime, isSystem, abi, targetApi, packageSize, compileSdk, minSdk from snapshot_table ORDER BY packageName ASC")
  suspend fun getSnapshotSummaries(): List<SnapshotSummaryItem>

  @Transaction
  @Query("SELECT * from snapshot_table WHERE timeStamp LIKE :timestamp ORDER BY packageName ASC")
  suspend fun getSnapshots(timestamp: Long): List<SnapshotItem>

  @Query("SELECT id, packageName, timeStamp, label, versionName, versionCode, isArchived, installedTime, lastUpdatedTime, isSystem, abi, targetApi, packageSize, compileSdk, minSdk from snapshot_table WHERE timeStamp LIKE :timestamp ORDER BY packageName ASC")
  suspend fun getSnapshotSummaries(timestamp: Long): List<SnapshotSummaryItem>

  @Query("SELECT * from snapshot_table WHERE timeStamp LIKE :timestamp AND packageName LIKE :packageName ORDER BY packageName ASC")
  suspend fun getSnapshot(timestamp: Long, packageName: String): SnapshotItem?

  @Query("SELECT id, packageName, timeStamp, label, versionName, versionCode, isArchived, installedTime, lastUpdatedTime, isSystem, abi, targetApi, packageSize, compileSdk, minSdk from snapshot_table WHERE timeStamp LIKE :timestamp AND packageName LIKE :packageName ORDER BY packageName ASC")
  suspend fun getSnapshotSummary(timestamp: Long, packageName: String): SnapshotSummaryItem?

  @Query("SELECT COUNT(*) from snapshot_table WHERE timeStamp LIKE :timestamp")
  fun getSnapshotsCountFlow(timestamp: Long): Flow<Int>

  @Upsert
  suspend fun insert(item: SnapshotItem)

  @Upsert
  suspend fun insertSnapshots(items: List<SnapshotItem>)

  @Update
  suspend fun update(item: SnapshotItem)

  @Update
  suspend fun update(items: List<SnapshotItem>)

  @Delete
  suspend fun delete(item: SnapshotItem)

  @Transaction
  @Query("DELETE FROM snapshot_table WHERE id NOT IN (SELECT id FROM snapshot_table GROUP BY packageName, timeStamp, versionCode, isArchived, lastUpdatedTime, packageSize)")
  suspend fun deleteDuplicateSnapshotItems()

  @Transaction
  @Query("DELETE FROM snapshot_table")
  suspend fun deleteAllSnapshots()

  @Transaction
  @Query("DELETE FROM snapshot_table WHERE timeStamp = :timestamp")
  suspend fun deleteSnapshots(timestamp: Long)

  @Transaction
  @Delete
  suspend fun deleteSnapshots(list: List<SnapshotItem>)

  // TimeStamp Table
  @Upsert
  suspend fun insert(item: TimeStampItem)

  @Query("SELECT * from timestamp_table ORDER BY timestamp DESC")
  suspend fun getTimeStamps(): List<TimeStampItem>

  @Query("SELECT * from timestamp_table WHERE timeStamp LIKE :timestamp")
  suspend fun getTimeStamp(timestamp: Long): TimeStampItem?

  @Delete
  suspend fun delete(item: TimeStampItem)

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
  @Upsert
  suspend fun insertSnapshotDiff(item: SnapshotDiffStoringItem)

  @Update
  suspend fun updateSnapshotDiff(item: SnapshotDiffStoringItem)

  @Query("DELETE FROM diff_table WHERE packageName = :packageName")
  suspend fun deleteSnapshotDiff(packageName: String)

  @Query("DELETE FROM diff_table")
  suspend fun deleteAllSnapshotDiffItems()

  @Query("SELECT * from diff_table WHERE packageName LIKE :packageName")
  suspend fun getSnapshotDiff(packageName: String): SnapshotDiffStoringItem?
}
