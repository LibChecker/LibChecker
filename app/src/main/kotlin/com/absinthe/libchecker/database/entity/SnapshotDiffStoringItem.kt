package com.absinthe.libchecker.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diff_table")
data class SnapshotDiffStoringItem(
  @PrimaryKey val packageName: String,
  val lastUpdatedTime: Long,
  val diffContent: String
)
