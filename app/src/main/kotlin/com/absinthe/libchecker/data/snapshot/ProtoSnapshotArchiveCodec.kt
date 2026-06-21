package com.absinthe.libchecker.data.snapshot

import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.domain.snapshot.SnapshotArchiveCodec
import com.absinthe.libchecker.protocol.Snapshot
import java.io.InputStream
import java.io.OutputStream

class ProtoSnapshotArchiveCodec : SnapshotArchiveCodec {

  override fun read(inputStream: InputStream): SnapshotItem? {
    return Snapshot.parseDelimitedFrom(inputStream)?.toSnapshotItem()
  }

  override fun write(item: SnapshotItem, outputStream: OutputStream) {
    item.toSnapshotMessage().writeDelimitedTo(outputStream)
  }

  private fun SnapshotItem.toSnapshotMessage(): Snapshot {
    return Snapshot.newBuilder().apply {
      packageName = this@toSnapshotMessage.packageName
      timeStamp = this@toSnapshotMessage.timeStamp
      label = this@toSnapshotMessage.label
      versionName = this@toSnapshotMessage.versionName
      versionCode = this@toSnapshotMessage.versionCode
      installedTime = this@toSnapshotMessage.installedTime
      lastUpdatedTime = this@toSnapshotMessage.lastUpdatedTime
      isSystem = this@toSnapshotMessage.isSystem
      abi = this@toSnapshotMessage.abi.toInt()
      targetApi = this@toSnapshotMessage.targetApi.toInt()
      nativeLibs = this@toSnapshotMessage.nativeLibs
      services = this@toSnapshotMessage.services
      activities = this@toSnapshotMessage.activities
      receivers = this@toSnapshotMessage.receivers
      providers = this@toSnapshotMessage.providers
      permissions = this@toSnapshotMessage.permissions
      metadata = this@toSnapshotMessage.metadata
      packageSize = this@toSnapshotMessage.packageSize
      compileSdk = this@toSnapshotMessage.compileSdk.toInt()
      minSdk = this@toSnapshotMessage.minSdk.toInt()
    }.build()
  }

  private fun Snapshot.toSnapshotItem(): SnapshotItem {
    return SnapshotItem(
      id = null,
      packageName = packageName,
      timeStamp = timeStamp,
      label = label,
      versionName = versionName,
      versionCode = versionCode,
      installedTime = installedTime,
      lastUpdatedTime = lastUpdatedTime,
      isSystem = isSystem,
      abi = abi.toShort(),
      targetApi = targetApi.toShort(),
      nativeLibs = nativeLibs,
      services = services,
      activities = activities,
      receivers = receivers,
      providers = providers,
      permissions = permissions,
      metadata = metadata,
      packageSize = packageSize,
      compileSdk = compileSdk.toShort(),
      minSdk = minSdk.toShort()
    )
  }
}
