package com.absinthe.libchecker.data.snapshot

import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.domain.snapshot.backup.archive.SnapshotArchiveCodec
import com.absinthe.libchecker.protocol.Snapshot
import com.absinthe.libchecker.utils.dex.DexEntryInfo
import com.absinthe.libchecker.utils.dex.DexStatsCollector
import com.absinthe.libchecker.utils.dex.ResourceEntryInfo
import com.absinthe.libchecker.utils.fromJson
import com.absinthe.libchecker.utils.toJson
import java.io.InputStream
import java.io.OutputStream

class ProtoSnapshotArchiveCodec : SnapshotArchiveCodec {

  override fun read(inputStream: InputStream): SnapshotItem? {
    return readDelimitedSnapshot(inputStream)?.toSnapshotItem()
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
      isArchived = this@toSnapshotMessage.isArchived
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
      dexInfo = this@toSnapshotMessage.dexInfo
      resourceInfo = this@toSnapshotMessage.resourceInfo
      resourcesSize = this@toSnapshotMessage.resourcesSize
      statsVersion = this@toSnapshotMessage.statsVersion
      dexStatsAvailable = this@toSnapshotMessage.dexStatsAvailable
      resourceStatsAvailable = this@toSnapshotMessage.resourceStatsAvailable
    }.build()
  }

  private fun Snapshot.toSnapshotItem(): SnapshotItem {
    val restored = SnapshotItem(
      id = null,
      packageName = packageName,
      timeStamp = timeStamp,
      label = label,
      versionName = versionName,
      versionCode = versionCode,
      isArchived = isArchived,
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
      minSdk = minSdk.toShort(),
      dexInfo = dexInfo,
      resourceInfo = resourceInfo,
      resourcesSize = resourcesSize,
      statsVersion = statsVersion,
      dexStatsAvailable = dexStatsAvailable,
      resourceStatsAvailable = resourceStatsAvailable
    )
    if (restored.statsVersion != SnapshotItem.CURRENT_STATS_VERSION) {
      return restored.copy(
        dexInfo = "[]",
        resourceInfo = "[]",
        resourcesSize = 0,
        statsVersion = 0,
        dexStatsAvailable = false,
        resourceStatsAvailable = false
      )
    }

    val dexEntries = if (
      restored.dexStatsAvailable &&
      restored.dexInfo.length <= DexStatsCollector.MAX_STORED_STATS_JSON_LENGTH
    ) {
      restored.dexInfo.fromJson<List<DexEntryInfo>>(
        List::class.java,
        DexEntryInfo::class.java
      )?.takeIf(DexStatsCollector::isValidStoredDexStats)
    } else {
      null
    }
    val resourceEntries = if (
      restored.resourceStatsAvailable &&
      restored.resourceInfo.length <= DexStatsCollector.MAX_STORED_STATS_JSON_LENGTH
    ) {
      restored.resourceInfo.fromJson<List<ResourceEntryInfo>>(
        List::class.java,
        ResourceEntryInfo::class.java
      )?.takeIf { entries ->
        DexStatsCollector.isValidStoredResourceStats(entries, restored.resourcesSize)
      }
    } else {
      null
    }
    return restored.copy(
      dexInfo = dexEntries?.toJson().orEmpty().ifEmpty { "[]" },
      resourceInfo = resourceEntries?.toJson().orEmpty().ifEmpty { "[]" },
      resourcesSize = resourceEntries?.sumOf(ResourceEntryInfo::size) ?: 0,
      dexStatsAvailable = dexEntries != null,
      resourceStatsAvailable = resourceEntries != null
    )
  }

  private fun readDelimitedSnapshot(inputStream: InputStream): Snapshot? {
    val messageSize = readRawVarint32(inputStream) ?: return null
    require(messageSize in 0..MAX_SNAPSHOT_MESSAGE_SIZE)
    val message = ByteArray(messageSize)
    var offset = 0
    while (offset < message.size) {
      val read = inputStream.read(message, offset, message.size - offset)
      require(read >= 0)
      offset += read
    }
    return Snapshot.parseFrom(message)
  }

  private fun readRawVarint32(inputStream: InputStream): Int? {
    var result = 0
    for (shift in 0 until 32 step 7) {
      val next = inputStream.read()
      if (next < 0) {
        return if (shift == 0) null else throw IllegalArgumentException("Truncated size")
      }
      result = result or ((next and 0x7f) shl shift)
      if (next and 0x80 == 0) {
        return result
      }
    }
    repeat(5) {
      val next = inputStream.read()
      require(next >= 0)
      if (next and 0x80 == 0) {
        return result
      }
    }
    throw IllegalArgumentException("Malformed size")
  }

  private companion object {
    const val MAX_SNAPSHOT_MESSAGE_SIZE = 16 * 1024 * 1024
  }
}
