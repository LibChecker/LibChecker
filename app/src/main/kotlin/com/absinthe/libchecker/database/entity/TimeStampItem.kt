package com.absinthe.libchecker.database.entity

import android.os.Parcelable
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "timestamp_table")
data class TimeStampItem(
  @PrimaryKey val timestamp: Long,
  val topApps: String?,
  val systemProps: String?
) : Parcelable
