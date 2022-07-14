package com.absinthe.libchecker.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diff_table")
data class SnapshotDiffStoringItem(
  @PrimaryKey val packageName: String,
  @ColumnInfo val lastUpdatedTime: Long,
  @ColumnInfo val diffContent: String
)
