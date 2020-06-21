package com.absinthe.libchecker.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "item_table")
data class LCItem(
    @PrimaryKey @ColumnInfo(name = "packageName") val packageName: String,
    @ColumnInfo(name = "label") val label: String,
    @ColumnInfo(name = "versionName") val versionName: String,
    @ColumnInfo(name = "versionCode") val versionCode: Long,
    @ColumnInfo(name = "installedTime") val installedTime: Long,
    @ColumnInfo(name = "lastUpdatedTime") val lastUpdatedTime: Long,
    @ColumnInfo(name = "isSystem") val isSystem: Boolean,
    @ColumnInfo(name = "abi") val abi: Short,
    @ColumnInfo(name = "isSplitApk") val isSplitApk: Boolean,
    @ColumnInfo(name = "isKotlinUsed") val isKotlinUsed: Boolean
)