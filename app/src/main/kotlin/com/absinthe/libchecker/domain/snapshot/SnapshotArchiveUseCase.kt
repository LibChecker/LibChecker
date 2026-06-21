package com.absinthe.libchecker.domain.snapshot

import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.protocol.Snapshot
import java.io.InputStream
import java.io.OutputStream

class SnapshotArchiveUseCase(
  private val repository: SnapshotArchiveRepository
) {

  suspend fun backup(outputStream: OutputStream) {
    outputStream.use { os ->
      repository.getTimeStamps().forEach { (timestamp, _, _) ->
        val backupList = repository.getSnapshots(timestamp)
        backupList.forEach {
          it.toSnapshotMessage().writeDelimitedTo(os)
        }
      }
    }
  }

  suspend fun restore(inputStream: InputStream): RestoreResult {
    val timeStampMap = mutableMapOf<Long, Int>()

    inputStream.use { stream ->
      val list = mutableListOf<Snapshot>()

      while (true) {
        val snapshot = Snapshot.parseDelimitedFrom(stream) ?: break
        list.add(snapshot)
        timeStampMap[snapshot.timeStamp] = timeStampMap.getOrDefault(snapshot.timeStamp, 0) + 1
        if (list.size == RESTORE_BATCH_SIZE) {
          restoreBatch(list)
          list.clear()
        }
      }

      restoreBatch(list)
      list.clear()
    }

    repository.deleteDuplicateSnapshotItems()
    timeStampMap.keys.forEach {
      repository.insertTimeStamp(TimeStampItem(it, null, null))
    }

    return RestoreResult(timeStampMap)
  }

  private suspend fun restoreBatch(list: List<Snapshot>) {
    if (list.isEmpty()) {
      return
    }
    repository.insertSnapshots(list.map { it.toSnapshotItem() })
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

  data class RestoreResult(
    val timeStampCounts: Map<Long, Int>
  ) {
    val latestTimeStamp: Long?
      get() = timeStampCounts.keys.maxOrNull()
  }

  private companion object {
    const val RESTORE_BATCH_SIZE = 200
  }
}
