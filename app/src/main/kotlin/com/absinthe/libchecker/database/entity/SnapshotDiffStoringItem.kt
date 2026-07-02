package com.absinthe.libchecker.database.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "diff_table")
data class SnapshotDiffStoringItem(
  @PrimaryKey val packageName: String,
  val lastUpdatedTime: Long,
  val diffContent: String
)
