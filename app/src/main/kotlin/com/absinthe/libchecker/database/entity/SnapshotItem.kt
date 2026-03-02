package com.absinthe.libchecker.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "snapshot_table")
data class SnapshotItem(
  @PrimaryKey(autoGenerate = true)
  var id: Int?,
  val packageName: String,
  var timeStamp: Long,
  val label: String,
  val versionName: String,
  val versionCode: Long,
  val installedTime: Long,
  val lastUpdatedTime: Long,
  val isSystem: Boolean,
  val abi: Short,
  val targetApi: Short,
  val nativeLibs: String,
  val services: String,
  val activities: String,
  val receivers: String,
  val providers: String,
  val permissions: String,
  val metadata: String,
  val packageSize: Long,
  val compileSdk: Short,
  val minSdk: Short
)
