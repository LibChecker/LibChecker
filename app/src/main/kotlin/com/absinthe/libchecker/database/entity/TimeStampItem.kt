package com.absinthe.libchecker.database.entity

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "timestamp_table")
data class TimeStampItem(
  @PrimaryKey val timestamp: Long,
  val topApps: String?
) : Parcelable
