package com.absinthe.libchecker.domain.snapshot

import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.database.entity.TimeStampItem
import java.io.InputStream
import java.io.OutputStream

class SnapshotArchiveUseCase(
  private val repository: SnapshotArchiveRepository,
  private val codec: SnapshotArchiveCodec
) {

  suspend fun backup(outputStream: OutputStream) {
    outputStream.use { os ->
      repository.getTimeStamps().forEach { (timestamp, _, _) ->
        val backupList = repository.getSnapshots(timestamp)
        backupList.forEach {
          codec.write(it, os)
        }
      }
    }
  }

  suspend fun restore(inputStream: InputStream): RestoreResult {
    val timeStampMap = mutableMapOf<Long, Int>()

    inputStream.use { stream ->
      val list = mutableListOf<SnapshotItem>()

      while (true) {
        val snapshot = codec.read(stream) ?: break
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

  private suspend fun restoreBatch(list: List<SnapshotItem>) {
    if (list.isEmpty()) {
      return
    }
    repository.insertSnapshots(list)
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
