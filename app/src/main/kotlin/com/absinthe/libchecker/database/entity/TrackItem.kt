package com.absinthe.libchecker.database.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "track_table")
data class TrackItem(
  @PrimaryKey val packageName: String
)
