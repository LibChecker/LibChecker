package com.absinthe.libchecker.utils.apk

import android.os.Trace
import java.io.File
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipFile
import timber.log.Timber

internal object ZipDataOffsetReader {

  fun read(file: File, entryNames: Set<String>): Map<String, Long> {
    if (entryNames.isEmpty()) {
      return emptyMap()
    }
    val offsets = traceSection(TRACE_ZIP_DATA_OFFSET_CENTRAL_DIRECTORY) {
      readFromCentralDirectory(file, entryNames)
    }
    if (offsets.keys.containsAll(entryNames)) {
      return offsets
    }
    return readWithApache(file, entryNames)
  }

  internal fun readFromCentralDirectory(file: File, entryNames: Set<String>): Map<String, Long> {
    return runCatching {
      FileSeekableInput(file).use { input ->
        val eocdOffset = input.findEndOfCentralDirectoryOffset()
        if (eocdOffset < 0L) {
          return@runCatching emptyMap()
        }

        val eocd = ByteArray(ZIP_EOCD_MIN_SIZE)
        input.position = eocdOffset
        input.readFully(eocd)
        val directory = input.readCentralDirectoryLocation(eocdOffset, eocd)
          ?: return@runCatching emptyMap()

        val offsets = mutableMapOf<String, Long>()
        val remainingEntryNames = entryNames.toMutableSet()
        var remainingBytes = directory.size
        input.position = directory.offset
        while (remainingBytes >= ZIP_CENTRAL_DIRECTORY_FIXED_SIZE && remainingEntryNames.isNotEmpty()) {
          val entryOffset = input.position
          val header = ByteArray(ZIP_CENTRAL_DIRECTORY_FIXED_SIZE)
          input.readFully(header)
          if (header.readUInt32Le(0) != ZIP_CENTRAL_DIRECTORY_SIGNATURE) {
            return@runCatching offsets
          }

          val nameSize = header.readUInt16Le(ZIP_CENTRAL_DIRECTORY_NAME_SIZE_OFFSET)
          val extraSize = header.readUInt16Le(ZIP_CENTRAL_DIRECTORY_EXTRA_SIZE_OFFSET)
          val commentSize = header.readUInt16Le(ZIP_CENTRAL_DIRECTORY_COMMENT_SIZE_OFFSET)
          val nameBytes = ByteArray(nameSize)
          input.readFully(nameBytes)
          val extra = ByteArray(extraSize)
          input.readFully(extra)
          val nextEntryOffset = entryOffset +
            ZIP_CENTRAL_DIRECTORY_FIXED_SIZE +
            nameSize +
            extraSize +
            commentSize

          val name = nameBytes.toString(Charsets.UTF_8)
          if (name in remainingEntryNames) {
            val localHeaderOffset = header.readLocalHeaderOffset(extra)
            val dataOffset = localHeaderOffset?.let { offset -> input.readLocalDataOffset(offset) } ?: -1L
            if (dataOffset > 0L) {
              offsets[name] = dataOffset
              remainingEntryNames.remove(name)
            }
          }

          input.position = nextEntryOffset
          remainingBytes -= nextEntryOffset - entryOffset
        }
        offsets
      }
    }.onFailure {
      Timber.w(it, "Failed to read ZIP data offsets from ${file.absolutePath}")
    }.getOrDefault(emptyMap())
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

  private fun SeekableInput.findEndOfCentralDirectoryOffset(): Long {
    val fileSize = size
    val searchSize = minOf(fileSize, ZIP_EOCD_MAX_SEARCH_SIZE.toLong()).toInt()
    if (searchSize < ZIP_EOCD_MIN_SIZE) {
      return -1L
    }
    val buffer = ByteArray(searchSize)
    position = fileSize - searchSize
    readFully(buffer)
    for (offset in searchSize - ZIP_EOCD_MIN_SIZE downTo 0) {
      if (
        buffer.readUInt32Le(offset) == ZIP_EOCD_SIGNATURE &&
        offset + ZIP_EOCD_MIN_SIZE + buffer.readUInt16Le(offset + ZIP_EOCD_COMMENT_SIZE_OFFSET) == searchSize
      ) {
        return fileSize - searchSize + offset
      }
    }
    return -1L
  }

  private fun SeekableInput.readCentralDirectoryLocation(
    eocdOffset: Long,
    eocd: ByteArray
  ): CentralDirectoryLocation? {
    val size = eocd.readUInt32Le(ZIP_EOCD_CENTRAL_DIRECTORY_SIZE_OFFSET)
    val offset = eocd.readUInt32Le(ZIP_EOCD_CENTRAL_DIRECTORY_OFFSET_OFFSET)
    if (size != ZIP_UINT32_MAX && offset != ZIP_UINT32_MAX) {
      return CentralDirectoryLocation(offset, size)
    }
    if (eocdOffset < ZIP64_EOCD_LOCATOR_SIZE) {
      return null
    }

    val locator = ByteArray(ZIP64_EOCD_LOCATOR_SIZE)
    position = eocdOffset - ZIP64_EOCD_LOCATOR_SIZE
    readFully(locator)
    if (locator.readUInt32Le(0) != ZIP64_EOCD_LOCATOR_SIGNATURE) {
      return null
    }
    val zip64EocdOffset = locator.readUInt64Le(ZIP64_EOCD_LOCATOR_OFFSET) ?: return null
    val zip64Eocd = ByteArray(ZIP64_EOCD_MIN_SIZE)
    position = zip64EocdOffset
    readFully(zip64Eocd)
    if (
      zip64Eocd.readUInt32Le(0) != ZIP64_EOCD_SIGNATURE ||
      (zip64Eocd.readUInt64Le(ZIP64_EOCD_RECORD_SIZE_OFFSET) ?: return null) < ZIP64_EOCD_MIN_RECORD_SIZE
    ) {
      return null
    }
    return CentralDirectoryLocation(
      offset = zip64Eocd.readUInt64Le(ZIP64_EOCD_CENTRAL_DIRECTORY_OFFSET_OFFSET) ?: return null,
      size = zip64Eocd.readUInt64Le(ZIP64_EOCD_CENTRAL_DIRECTORY_SIZE_OFFSET) ?: return null
    )
  }

  private fun ByteArray.readLocalHeaderOffset(extra: ByteArray): Long? {
    val legacyOffset = readUInt32Le(ZIP_CENTRAL_DIRECTORY_LOCAL_HEADER_OFFSET)
    if (legacyOffset != ZIP_UINT32_MAX) {
      return legacyOffset
    }
    var extraOffset = 0
    while (extraOffset + ZIP_EXTRA_FIELD_HEADER_SIZE <= extra.size) {
      val headerId = extra.readUInt16Le(extraOffset)
      val dataSize = extra.readUInt16Le(extraOffset + 2)
      val dataOffset = extraOffset + ZIP_EXTRA_FIELD_HEADER_SIZE
      val nextOffset = dataOffset + dataSize
      if (nextOffset > extra.size) {
        return null
      }
      if (headerId == ZIP64_EXTRA_FIELD_ID) {
        var valueOffset = dataOffset
        if (readUInt32Le(ZIP_CENTRAL_DIRECTORY_UNCOMPRESSED_SIZE_OFFSET) == ZIP_UINT32_MAX) {
          valueOffset += Long.SIZE_BYTES
        }
        if (readUInt32Le(ZIP_CENTRAL_DIRECTORY_COMPRESSED_SIZE_OFFSET) == ZIP_UINT32_MAX) {
          valueOffset += Long.SIZE_BYTES
        }
        return extra.readUInt64Le(valueOffset)
      }
      extraOffset = nextOffset
    }
    return null
  }

  private fun SeekableInput.readLocalDataOffset(localHeaderOffset: Long): Long {
    val header = ByteArray(ZIP_LOCAL_FILE_HEADER_FIXED_SIZE)
    position = localHeaderOffset
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

  private fun ByteArray.readUInt64Le(offset: Int): Long? {
    if (offset < 0 || offset + Long.SIZE_BYTES > size || this[offset + 7].toInt() and 0x80 != 0) {
      return null
    }
    var result = 0L
    for (index in 0 until Long.SIZE_BYTES) {
      result = result or ((this[offset + index].toLong() and 0xff) shl (index * 8))
    }
    return result
  }

  private data class CentralDirectoryLocation(
    val offset: Long,
    val size: Long
  )

  private const val ZIP_EOCD_SIGNATURE = 0x06054b50L
  private const val ZIP_EOCD_MIN_SIZE = 22
  private const val ZIP_EOCD_MAX_SEARCH_SIZE = ZIP_EOCD_MIN_SIZE + 0xffff
  private const val ZIP_EOCD_CENTRAL_DIRECTORY_SIZE_OFFSET = 12
  private const val ZIP_EOCD_CENTRAL_DIRECTORY_OFFSET_OFFSET = 16
  private const val ZIP_EOCD_COMMENT_SIZE_OFFSET = 20
  private const val ZIP64_EOCD_LOCATOR_SIGNATURE = 0x07064b50L
  private const val ZIP64_EOCD_LOCATOR_SIZE = 20
  private const val ZIP64_EOCD_LOCATOR_OFFSET = 8
  private const val ZIP64_EOCD_SIGNATURE = 0x06064b50L
  private const val ZIP64_EOCD_MIN_SIZE = 56
  private const val ZIP64_EOCD_MIN_RECORD_SIZE = 44L
  private const val ZIP64_EOCD_RECORD_SIZE_OFFSET = 4
  private const val ZIP64_EOCD_CENTRAL_DIRECTORY_SIZE_OFFSET = 40
  private const val ZIP64_EOCD_CENTRAL_DIRECTORY_OFFSET_OFFSET = 48
  private const val ZIP_CENTRAL_DIRECTORY_SIGNATURE = 0x02014b50L
  private const val ZIP_CENTRAL_DIRECTORY_FIXED_SIZE = 46
  private const val ZIP_CENTRAL_DIRECTORY_COMPRESSED_SIZE_OFFSET = 20
  private const val ZIP_CENTRAL_DIRECTORY_UNCOMPRESSED_SIZE_OFFSET = 24
  private const val ZIP_CENTRAL_DIRECTORY_NAME_SIZE_OFFSET = 28
  private const val ZIP_CENTRAL_DIRECTORY_EXTRA_SIZE_OFFSET = 30
  private const val ZIP_CENTRAL_DIRECTORY_COMMENT_SIZE_OFFSET = 32
  private const val ZIP_CENTRAL_DIRECTORY_LOCAL_HEADER_OFFSET = 42
  private const val ZIP_LOCAL_FILE_HEADER_SIGNATURE = 0x04034b50L
  private const val ZIP_LOCAL_FILE_HEADER_FIXED_SIZE = 30
  private const val ZIP_LOCAL_FILE_HEADER_NAME_SIZE_OFFSET = 26
  private const val ZIP_LOCAL_FILE_HEADER_EXTRA_SIZE_OFFSET = 28
  private const val ZIP_EXTRA_FIELD_HEADER_SIZE = 4
  private const val ZIP64_EXTRA_FIELD_ID = 0x0001
  private const val ZIP_UINT32_MAX = 0xffffffffL
  private const val TRACE_ZIP_DATA_OFFSET_APACHE = "LC ZipDataOffset apache"
  private const val TRACE_ZIP_DATA_OFFSET_CENTRAL_DIRECTORY = "LC ZipDataOffset central"
  private val getDataOffsetMethod by lazy {
    ZipFile::class.java.getDeclaredMethod("getDataOffset", ZipArchiveEntry::class.java).apply {
      isAccessible = true
    }
  }
}
