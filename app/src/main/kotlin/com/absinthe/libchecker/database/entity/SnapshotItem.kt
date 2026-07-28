package com.absinthe.libchecker.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "snapshot_table")
data class SnapshotItem(
  @PrimaryKey(autoGenerate = true)
  var id: Int?,
  val packageName: String,
  var timeStamp: Long,
  val label: String,
  val versionName: String,
  val versionCode: Long,
  @ColumnInfo(defaultValue = "0")
  val isArchived: Boolean,
  val installedTime: Long,
  val lastUpdatedTime: Long,
  val isSystem: Boolean,
  val abi: Short,
  val targetApi: Short,
  val nativeLibs: String,
  val services: String,
  val activities: String,
  val receivers: String,
  val providers: String,
  val permissions: String,
  val metadata: String,
  val packageSize: Long,
  val compileSdk: Short,
  val minSdk: Short,
  @ColumnInfo(defaultValue = "'[]'")
  val dexInfo: String = "[]",
  @ColumnInfo(defaultValue = "'[]'")
  val resourceInfo: String = "[]",
  @ColumnInfo(defaultValue = "0")
  val resourcesSize: Long = 0,
  @ColumnInfo(defaultValue = "0")
  val statsVersion: Int = 0,
  @ColumnInfo(defaultValue = "0")
  val dexStatsAvailable: Boolean = false,
  @ColumnInfo(defaultValue = "0")
  val resourceStatsAvailable: Boolean = false
) {
  fun hasDexStats(): Boolean = statsVersion == CURRENT_STATS_VERSION && dexStatsAvailable

  fun hasResourceStats(): Boolean = statsVersion == CURRENT_STATS_VERSION && resourceStatsAvailable

  companion object {
    const val CURRENT_STATS_VERSION = 2
  }
}
