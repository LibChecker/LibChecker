package com.absinthe.libchecker.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "diff_table")
data class SnapshotDiffStoringItem(
  @PrimaryKey val packageName: String,
  val lastUpdatedTime: Long,
  @ColumnInfo(defaultValue = "0")
  val isArchived: Boolean,
  val diffContent: String
)
