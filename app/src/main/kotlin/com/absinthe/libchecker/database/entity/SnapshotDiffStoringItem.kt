package com.absinthe.libchecker.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diff_table")
data class SnapshotDiffStoringItem(
  @PrimaryKey @ColumnInfo(name = "packageName") val packageName: String,
  @ColumnInfo(name = "lastUpdatedTime") val lastUpdatedTime: Long,
  @ColumnInfo(name = "diffContent") val diffContent: String
)
