package com.absinthe.libchecker.utils.apk

import android.os.Trace
import java.io.File
import java.io.RandomAccessFile
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipFile
import timber.log.Timber

internal object ZipDataOffsetReader {

  fun read(file: File, entryNames: Set<String>): Map<String, Long> {
    if (entryNames.isEmpty()) {
      return emptyMap()
    }
    val offsets = readFromCentralDirectory(file, entryNames)
    if (offsets.keys.containsAll(entryNames)) {
      return offsets
    }
    return readWithApache(file, entryNames)
  }

  private fun readFromCentralDirectory(file: File, entryNames: Set<String>): Map<String, Long> {
    return traceSection(TRACE_ZIP_DATA_OFFSET_CENTRAL_DIRECTORY) {
      runCatching {
        RandomAccessFile(file, "r").use { randomAccessFile ->
          val eocdOffset = randomAccessFile.findEndOfCentralDirectoryOffset()
          if (eocdOffset < 0L) {
            return@runCatching emptyMap()
          }

          val eocd = ByteArray(ZIP_EOCD_MIN_SIZE)
          randomAccessFile.seek(eocdOffset)
          randomAccessFile.readFully(eocd)
          val centralDirectorySize = eocd.readUInt32Le(ZIP_EOCD_CENTRAL_DIRECTORY_SIZE_OFFSET)
          val centralDirectoryOffset = eocd.readUInt32Le(ZIP_EOCD_CENTRAL_DIRECTORY_OFFSET_OFFSET)
          if (
            centralDirectorySize == ZIP_UINT32_MAX ||
            centralDirectoryOffset == ZIP_UINT32_MAX
          ) {
            return@runCatching emptyMap()
          }

          val offsets = mutableMapOf<String, Long>()
          val remainingEntryNames = entryNames.toMutableSet()
          var remainingBytes = centralDirectorySize
          randomAccessFile.seek(centralDirectoryOffset)
          while (remainingBytes >= ZIP_CENTRAL_DIRECTORY_FIXED_SIZE && remainingEntryNames.isNotEmpty()) {
            val entryOffset = randomAccessFile.filePointer
            val header = ByteArray(ZIP_CENTRAL_DIRECTORY_FIXED_SIZE)
            randomAccessFile.readFully(header)
            if (header.readUInt32Le(0) != ZIP_CENTRAL_DIRECTORY_SIGNATURE) {
              return@runCatching offsets
            }

            val nameSize = header.readUInt16Le(ZIP_CENTRAL_DIRECTORY_NAME_SIZE_OFFSET)
            val extraSize = header.readUInt16Le(ZIP_CENTRAL_DIRECTORY_EXTRA_SIZE_OFFSET)
            val commentSize = header.readUInt16Le(ZIP_CENTRAL_DIRECTORY_COMMENT_SIZE_OFFSET)
            val localHeaderOffset = header.readUInt32Le(ZIP_CENTRAL_DIRECTORY_LOCAL_HEADER_OFFSET)
            val nameBytes = ByteArray(nameSize)
            randomAccessFile.readFully(nameBytes)
            val nextEntryOffset = entryOffset +
              ZIP_CENTRAL_DIRECTORY_FIXED_SIZE +
              nameSize +
              extraSize +
              commentSize

            val name = nameBytes.toString(Charsets.UTF_8)
            if (name in remainingEntryNames && localHeaderOffset != ZIP_UINT32_MAX) {
              val dataOffset = randomAccessFile.readLocalDataOffset(localHeaderOffset)
              if (dataOffset > 0L) {
                offsets[name] = dataOffset
                remainingEntryNames.remove(name)
              }
            }

            randomAccessFile.seek(nextEntryOffset)
            remainingBytes -= randomAccessFile.filePointer - entryOffset
          }
          offsets
        }
      }.onFailure {
        Timber.w(it, "Failed to read ZIP data offsets from ${file.absolutePath}")
      }.getOrDefault(emptyMap())
    }
  }

  private fun readWithApache(file: File, entryNames: Set<String>): Map<String, Long> {
    return traceSection(TRACE_ZIP_DATA_OFFSET_APACHE) {
      runCatching {
        ZipFile.Builder().setFile(file).get().use { zipFile ->
          entryNames.associateWith { entryName ->
            val entry = zipFile.getEntry(entryName)
              ?: throw IllegalArgumentException("ZIP entry $entryName was not found in ${file.absolutePath}")
            getDataOffsetMethod.invoke(zipFile, entry) as Long
          }
        }
      }.onFailure {
        Timber.w(it, "Failed to read ZIP data offsets with Commons Compress from ${file.absolutePath}")
      }.getOrDefault(emptyMap())
    }
  }

  private inline fun <T> traceSection(sectionName: String, block: () -> T): T {
    Trace.beginSection(sectionName)
    return try {
      block()
    } finally {
      Trace.endSection()
    }
  }

  private fun RandomAccessFile.findEndOfCentralDirectoryOffset(): Long {
    val fileSize = length()
    val searchSize = minOf(fileSize, ZIP_EOCD_MAX_SEARCH_SIZE.toLong()).toInt()
    if (searchSize < ZIP_EOCD_MIN_SIZE) {
      return -1L
    }
    val buffer = ByteArray(searchSize)
    seek(fileSize - searchSize)
    readFully(buffer)
    for (offset in searchSize - ZIP_EOCD_MIN_SIZE downTo 0) {
      if (buffer.readUInt32Le(offset) == ZIP_EOCD_SIGNATURE) {
        return fileSize - searchSize + offset
      }
    }
    return -1L
  }

  private fun RandomAccessFile.readLocalDataOffset(localHeaderOffset: Long): Long {
    val header = ByteArray(ZIP_LOCAL_FILE_HEADER_FIXED_SIZE)
    seek(localHeaderOffset)
    readFully(header)
    if (header.readUInt32Le(0) != ZIP_LOCAL_FILE_HEADER_SIGNATURE) {
      return -1L
    }
    val nameSize = header.readUInt16Le(ZIP_LOCAL_FILE_HEADER_NAME_SIZE_OFFSET)
    val extraSize = header.readUInt16Le(ZIP_LOCAL_FILE_HEADER_EXTRA_SIZE_OFFSET)
    return localHeaderOffset + ZIP_LOCAL_FILE_HEADER_FIXED_SIZE + nameSize + extraSize
  }

  private fun ByteArray.readUInt16Le(offset: Int): Int {
    return (this[offset].toInt() and 0xff) or
      ((this[offset + 1].toInt() and 0xff) shl 8)
  }

  private fun ByteArray.readUInt32Le(offset: Int): Long {
    return (this[offset].toLong() and 0xff) or
      ((this[offset + 1].toLong() and 0xff) shl 8) or
      ((this[offset + 2].toLong() and 0xff) shl 16) or
      ((this[offset + 3].toLong() and 0xff) shl 24)
  }

  private const val ZIP_EOCD_SIGNATURE = 0x06054b50L
  private const val ZIP_EOCD_MIN_SIZE = 22
  private const val ZIP_EOCD_MAX_SEARCH_SIZE = ZIP_EOCD_MIN_SIZE + 0xffff
  private const val ZIP_EOCD_CENTRAL_DIRECTORY_SIZE_OFFSET = 12
  private const val ZIP_EOCD_CENTRAL_DIRECTORY_OFFSET_OFFSET = 16
  private const val ZIP_CENTRAL_DIRECTORY_SIGNATURE = 0x02014b50L
  private const val ZIP_CENTRAL_DIRECTORY_FIXED_SIZE = 46
  private const val ZIP_CENTRAL_DIRECTORY_NAME_SIZE_OFFSET = 28
  private const val ZIP_CENTRAL_DIRECTORY_EXTRA_SIZE_OFFSET = 30
  private const val ZIP_CENTRAL_DIRECTORY_COMMENT_SIZE_OFFSET = 32
  private const val ZIP_CENTRAL_DIRECTORY_LOCAL_HEADER_OFFSET = 42
  private const val ZIP_LOCAL_FILE_HEADER_SIGNATURE = 0x04034b50L
  private const val ZIP_LOCAL_FILE_HEADER_FIXED_SIZE = 30
  private const val ZIP_LOCAL_FILE_HEADER_NAME_SIZE_OFFSET = 26
  private const val ZIP_LOCAL_FILE_HEADER_EXTRA_SIZE_OFFSET = 28
  private const val ZIP_UINT32_MAX = 0xffffffffL
  private const val TRACE_ZIP_DATA_OFFSET_APACHE = "LC ZipDataOffset apache"
  private const val TRACE_ZIP_DATA_OFFSET_CENTRAL_DIRECTORY = "LC ZipDataOffset central"
  private val getDataOffsetMethod by lazy {
    ZipFile::class.java.getDeclaredMethod("getDataOffset", ZipArchiveEntry::class.java).apply {
      isAccessible = true
    }
  }
}
