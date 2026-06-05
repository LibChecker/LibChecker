package com.absinthe.libchecker.database.entity

data class SnapshotSummaryItem(
  val id: Int?,
  val packageName: String,
  val timeStamp: Long,
  val label: String,
  val versionName: String,
  val versionCode: Long,
  val installedTime: Long,
  val lastUpdatedTime: Long,
  val isSystem: Boolean,
  val abi: Short,
  val targetApi: Short,
  val packageSize: Long,
  val compileSdk: Short,
  val minSdk: Short
) {
  fun toSnapshotItem(): SnapshotItem {
    return SnapshotItem(
      id,
      packageName,
      timeStamp,
      label,
      versionName,
      versionCode,
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
      compileSdk = compileSdk,
      minSdk = minSdk
    )
  }
}
