package com.absinthe.libchecker.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "snapshot_table")
data class SnapshotItem(
  @PrimaryKey(autoGenerate = true)
  var id: Int?,
  @ColumnInfo val packageName: String,
  @ColumnInfo var timeStamp: Long,
  @ColumnInfo val label: String,
  @ColumnInfo val versionName: String,
  @ColumnInfo val versionCode: Long,
  @ColumnInfo val installedTime: Long,
  @ColumnInfo val lastUpdatedTime: Long,
  @ColumnInfo val isSystem: Boolean,
  @ColumnInfo val abi: Short,
  @ColumnInfo val targetApi: Short,
  @ColumnInfo val nativeLibs: String,
  @ColumnInfo val services: String,
  @ColumnInfo val activities: String,
  @ColumnInfo val receivers: String,
  @ColumnInfo val providers: String,
  @ColumnInfo val permissions: String,
  @ColumnInfo val metadata: String,
  @ColumnInfo val packageSize: Long
)
