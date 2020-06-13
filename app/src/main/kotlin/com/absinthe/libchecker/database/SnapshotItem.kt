package com.absinthe.libchecker.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "snapshot_table")
data class SnapshotItem(
    @PrimaryKey @ColumnInfo(name = "packageName") val packageName: String,
    @ColumnInfo(name = "label") val label: String,
    @ColumnInfo(name = "versionName") val versionName: String,
    @ColumnInfo(name = "versionCode") val versionCode: Long,
    @ColumnInfo(name = "installedTime") val installedTime: Long,
    @ColumnInfo(name = "lastUpdatedTime") val lastUpdatedTime: Long,
    @ColumnInfo(name = "isSystem") val isSystem: Boolean,
    @ColumnInfo(name = "abi") val abi: Short,
    @ColumnInfo(name = "nativeLibs") val nativeLibs: String,
    @ColumnInfo(name = "services") val services: String,
    @ColumnInfo(name = "activities") val activities: String,
    @ColumnInfo(name = "receivers") val receivers: String,
    @ColumnInfo(name = "providers") val providers: String
)