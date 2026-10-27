package com.absinthe.libchecker.data.snapshot

import com.absinthe.libchecker.domain.snapshot.detail.repository.SnapshotDetailPayloadRepository
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.utils.JsonUtil
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.ForwardingSink
import okio.buffer
import okio.sink
import okio.source

class FileSnapshotDetailPayloadRepository(
  private val directory: File
) : SnapshotDetailPayloadRepository {
  private val adapter = JsonUtil.moshi.adapter(SnapshotDiffItem::class.java)
  private val mutex = Mutex()

  override suspend fun save(item: SnapshotDiffItem): String = withContext(Dispatchers.IO) {
    mutex.withLock {
      directory.mkdirs()
      check(directory.isDirectory) { "Cannot create snapshot detail cache" }
      // Retain recent payloads for activity/process recreation, including concurrent detail pages.
      val cutoff = System.currentTimeMillis() - RETENTION_MILLIS
      directory.listFiles()?.filter { it.lastModified() < cutoff }?.forEach { it.delete() }
      val id = UUID.randomUUID().toString()
      val file = File(directory, id)
      try {
        object : ForwardingSink(file.sink()) {
          private var written = 0L
          override fun write(source: Buffer, byteCount: Long) {
            if (byteCount > MAX_CACHE_BYTES - written) throw IOException("Snapshot detail payload exceeds cache budget")
            super.write(source, byteCount)
            written += byteCount
          }
        }.buffer().use { adapter.toJson(it, item) }
        val olderFiles = directory.listFiles().orEmpty().filter { it != file }.sortedBy { it.lastModified() }
        var bytes = olderFiles.sumOf { it.length() } + file.length()
        var count = olderFiles.size + 1
        for (old in olderFiles) {
          if (bytes <= MAX_CACHE_BYTES && count <= MAX_CACHE_FILES) break
          val size = old.length()
          check(old.delete()) { "Cannot prune snapshot detail cache" }
          bytes -= size
          count--
        }
        id
      } catch (e: Exception) {
        file.delete()
        throw e
      }
    }
  }

  override suspend fun load(id: String): SnapshotDiffItem? = withContext(Dispatchers.IO) {
    mutex.withLock {
      if (!ID_PATTERN.matches(id)) return@withContext null
      val file = File(directory, id)
      if (!file.isFile) return@withContext null
      file.source().buffer().use { adapter.fromJson(it) }
    }
  }

  internal companion object {
    private val ID_PATTERN = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
    private const val RETENTION_MILLIS = 7L * 24 * 60 * 60 * 1000
    const val MAX_CACHE_BYTES = 32L * 1024 * 1024
    const val MAX_CACHE_FILES = 64
  }
}
