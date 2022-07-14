package com.absinthe.libchecker.database.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "timestamp_table")
data class TimeStampItem(
  @PrimaryKey val timestamp: Long,
  @ColumnInfo val topApps: String?
) : Parcelable
