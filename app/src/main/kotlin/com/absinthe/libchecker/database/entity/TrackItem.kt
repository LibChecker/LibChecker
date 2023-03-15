package com.absinthe.libchecker.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "track_table")
data class TrackItem(
  @PrimaryKey val packageName: String
)
