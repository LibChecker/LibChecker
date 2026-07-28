package com.absinthe.libchecker.utils.apk

import com.absinthe.libchecker.compat.ZipFileCompat
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipEntry
import okio.Buffer
import okio.buffer
import okio.sink
import okio.source

internal object ApkArchiveStager {

  private const val MAX_APK_ENTRIES = 256
  private const val MAX_ENTRY_BYTES = 4L * 1024 * 1024 * 1024
  private const val MAX_TOTAL_BYTES = 8L * 1024 * 1024 * 1024
  private const val MAX_CACHE_BYTES = 12L * 1024 * 1024 * 1024
  private const val CACHE_MAX_AGE_MILLIS = 7L * 24 * 60 * 60 * 1000

  @Synchronized
  fun stage(
    archive: File,
    cacheRoot: File,
    zipFile: ZipFileCompat,
    entries: List<Pair<ZipEntry, String>>
  ): List<File> {
    require(entries.isNotEmpty()) { "Archive contains no APK entries" }
    require(entries.size <= MAX_APK_ENTRIES) { "Archive contains too many APK entries" }

    val normalized = entries.map { (entry, outputName) ->
      require(!entry.isDirectory) { "APK entry is a directory: ${entry.name}" }
      val safeName = outputName.substringAfterLast('/').substringAfterLast('\\')
      require(safeName.isNotBlank() && safeName.endsWith(".apk", ignoreCase = true)) {
        "Invalid APK output name"
      }
      entry to safeName
    }
    require(normalized.map { it.second }.distinct().size == normalized.size) {
      "Archive contains duplicate APK filenames"
    }
    require(normalized.all { (entry, _) -> entry.size in -1..MAX_ENTRY_BYTES }) {
      "Archive contains an oversized APK entry"
    }
    val declaredTotal = normalized.sumOf { (entry, _) -> entry.size.coerceAtLeast(0L) }
    require(declaredTotal <= MAX_TOTAL_BYTES) { "Expanded APK archive is too large" }

    val key = archive.cacheKey()
    check(cacheRoot.mkdirs() || cacheRoot.isDirectory) { "Unable to create APK cache directory" }
    pruneCache(cacheRoot, key)
    val targetDir = File(cacheRoot, key)
    if (targetDir.isDirectory) {
      val cached = normalized.map { File(targetDir, it.second) }
      if (cached.all { it.isFile }) return cached
    }

    val stagingDir = File(cacheRoot, ".$key-${UUID.randomUUID()}.tmp")
    stagingDir.deleteRecursively()
    check(stagingDir.mkdirs()) { "Unable to create APK staging directory" }
    check(stagingDir.usableSpace >= declaredTotal) { "Insufficient space to stage APK archive" }
    try {
      var totalBytes = 0L
      val staged = normalized.map { (entry, safeName) ->
        val output = File(stagingDir, safeName)
        var entryBytes = 0L
        zipFile.getInputStream(entry).source().buffer().use { source ->
          output.sink().buffer().use { sink ->
            val buffer = Buffer()
            while (true) {
              val read = source.read(buffer, DEFAULT_BUFFER_SIZE.toLong())
              if (read < 0) break
              totalBytes += read
              entryBytes += read
              check(entryBytes <= MAX_ENTRY_BYTES) { "Expanded APK entry is too large" }
              check(totalBytes <= MAX_TOTAL_BYTES) { "Expanded APK archive is too large" }
              sink.write(buffer, read)
            }
          }
        }
        check(entry.size < 0 || output.length() == entry.size) {
          "Expanded APK entry size does not match its ZIP metadata"
        }
        output
      }
      targetDir.deleteRecursively()
      check(stagingDir.renameTo(targetDir)) { "Unable to publish APK staging directory" }
      return staged.map { File(targetDir, it.name) }
    } finally {
      stagingDir.deleteRecursively()
    }
  }

  private fun pruneCache(cacheRoot: File, activeKey: String) {
    val now = System.currentTimeMillis()
    val directories = cacheRoot.listFiles().orEmpty()
      .filter { it.isDirectory && it.name != activeKey }
      .sortedByDescending { it.lastModified() }
    var retainedBytes = 0L
    directories.forEach { directory ->
      val size = directory.walkBottomUp().filter { it.isFile }.sumOf { it.length() }
      val expired = now - directory.lastModified() > CACHE_MAX_AGE_MILLIS
      if (expired || retainedBytes + size > MAX_CACHE_BYTES || directory.name.startsWith('.')) {
        directory.deleteRecursively()
      } else {
        retainedBytes += size
      }
    }
  }

  private fun File.cacheKey(): String {
    val identity = "$canonicalPath\u0000${length()}\u0000${lastModified()}"
    return MessageDigest.getInstance("SHA-256")
      .digest(identity.toByteArray())
      .take(16)
      .joinToString("") { byte -> "%02x".format(byte) }
  }
}
