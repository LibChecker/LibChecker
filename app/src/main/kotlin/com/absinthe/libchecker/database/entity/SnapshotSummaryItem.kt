package com.absinthe.libchecker.database.entity

data class SnapshotSummaryItem(
  val id: Int?,
  val packageName: String,
  val timeStamp: Long,
  val label: String,
  val versionName: String,
  val versionCode: Long,
  val isArchived: Boolean,
  val installedTime: Long,
  val lastUpdatedTime: Long,
  val isSystem: Boolean,
  val abi: Short,
  val targetApi: Short,
  val packageSize: Long,
  val compileSdk: Short,
  val minSdk: Short,
  val dexInfo: String = "[]",
  val resourceInfo: String = "[]",
  val resourcesSize: Long = 0,
  val statsVersion: Int = 0,
  val dexStatsAvailable: Boolean = false,
  val resourceStatsAvailable: Boolean = false
) {
  fun toSnapshotItem(): SnapshotItem {
    return SnapshotItem(
      id,
      packageName,
      timeStamp,
      label,
      versionName,
      versionCode,
      isArchived,
      installedTime,
      lastUpdatedTime,
      isSystem,
      abi,
      targetApi,
      nativeLibs = "",
      services = "",
      activities = "",
      receivers = "",
      providers = "",
      permissions = "",
      metadata = "",
      packageSize = packageSize,
      dexInfo = dexInfo,
      resourceInfo = resourceInfo,
      resourcesSize = resourcesSize,
      statsVersion = statsVersion,
      dexStatsAvailable = dexStatsAvailable,
      resourceStatsAvailable = resourceStatsAvailable,
      compileSdk = compileSdk,
      minSdk = minSdk
    )
  }
}
