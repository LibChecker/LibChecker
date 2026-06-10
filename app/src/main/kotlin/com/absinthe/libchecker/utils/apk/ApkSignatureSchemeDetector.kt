package com.absinthe.libchecker.utils.apk

import java.io.File
import java.io.RandomAccessFile

object ApkSignatureSchemeDetector {

  fun detect(apk: File): List<String> {
    if (apk.exists().not() || apk.canRead().not()) {
      return emptyList()
    }

    val signatureProbe = readApkSignatureProbe(apk)
    return buildList {
      if (signatureProbe.hasJarSignature) add("V1")
      if (APK_SIGNATURE_SCHEME_V2_BLOCK_ID in signatureProbe.signingBlockIds) add("V2")
      if (APK_SIGNATURE_SCHEME_V3_BLOCK_ID in signatureProbe.signingBlockIds) add("V3")
      if (APK_SIGNATURE_SCHEME_V31_BLOCK_ID in signatureProbe.signingBlockIds) add("V3.1")
      if (File("${apk.absolutePath}.idsig").exists()) add("V4")
    }
  }

  private fun readApkSignatureProbe(apk: File): ApkSignatureProbe {
    return RandomAccessFile(apk, "r").use { file ->
      val centralDirectory = file.findCentralDirectory() ?: return@use ApkSignatureProbe.EMPTY
      ApkSignatureProbe(
        hasJarSignature = file.hasJarSignature(centralDirectory),
        signingBlockIds = file.readApkSigningBlockIds(centralDirectory.offset)
      )
    }
  }

  private fun RandomAccessFile.hasJarSignature(centralDirectory: ZipCentralDirectory): Boolean {
    if (centralDirectory.size <= 0L) {
      return false
    }
    if (
      centralDirectory.size > Int.MAX_VALUE ||
      centralDirectory.size > MAX_ZIP_CENTRAL_DIRECTORY_SIZE
    ) {
      return hasJarSignatureStreamed(centralDirectory)
    }

    val directory = ByteArray(centralDirectory.size.toInt())
    seek(centralDirectory.offset)
    readFully(directory)
    var offset = 0
    while (offset + ZIP_CENTRAL_DIRECTORY_HEADER_SIZE <= directory.size) {
      if (directory.readIntLe(offset) != ZIP_CENTRAL_DIRECTORY_SIGNATURE) {
        return false
      }

      val fileNameLength = directory.readUnsignedShortLe(offset + ZIP_CENTRAL_DIRECTORY_FILE_NAME_LENGTH_OFFSET)
      val extraLength = directory.readUnsignedShortLe(offset + ZIP_CENTRAL_DIRECTORY_EXTRA_LENGTH_OFFSET)
      val commentLength = directory.readUnsignedShortLe(offset + ZIP_CENTRAL_DIRECTORY_COMMENT_LENGTH_OFFSET)
      val entrySize = ZIP_CENTRAL_DIRECTORY_HEADER_SIZE + fileNameLength + extraLength + commentLength
      if (offset + entrySize > directory.size) {
        return false
      }

      if (directory.isJarSignatureEntryName(offset + ZIP_CENTRAL_DIRECTORY_HEADER_SIZE, fileNameLength)) {
        return true
      }

      offset += entrySize
    }
    return false
  }

  private fun RandomAccessFile.hasJarSignatureStreamed(centralDirectory: ZipCentralDirectory): Boolean {
    val header = ByteArray(ZIP_CENTRAL_DIRECTORY_HEADER_SIZE)
    var remaining = centralDirectory.size
    seek(centralDirectory.offset)
    while (remaining >= ZIP_CENTRAL_DIRECTORY_HEADER_SIZE) {
      readFully(header)
      if (header.readIntLe(0) != ZIP_CENTRAL_DIRECTORY_SIGNATURE) {
        return false
      }

      val fileNameLength = header.readUnsignedShortLe(ZIP_CENTRAL_DIRECTORY_FILE_NAME_LENGTH_OFFSET)
      val extraLength = header.readUnsignedShortLe(ZIP_CENTRAL_DIRECTORY_EXTRA_LENGTH_OFFSET)
      val commentLength = header.readUnsignedShortLe(ZIP_CENTRAL_DIRECTORY_COMMENT_LENGTH_OFFSET)
      val entrySize = ZIP_CENTRAL_DIRECTORY_HEADER_SIZE + fileNameLength + extraLength + commentLength
      if (entrySize > remaining) {
        return false
      }

      val fileName = ByteArray(fileNameLength)
      readFully(fileName)
      if (fileName.isJarSignatureEntryName(0, fileNameLength)) {
        return true
      }

      seek(filePointer + extraLength + commentLength)
      remaining -= entrySize
    }
    return false
  }

  private fun RandomAccessFile.readApkSigningBlockIds(centralDirOffset: Long): Set<Int> {
    if (centralDirOffset < APK_SIGNING_BLOCK_FOOTER_SIZE) {
      return emptySet()
    }

    val footer = ByteArray(APK_SIGNING_BLOCK_FOOTER_SIZE)
    seek(centralDirOffset - APK_SIGNING_BLOCK_FOOTER_SIZE)
    readFully(footer)
    if (footer.hasApkSigningBlockMagic().not()) {
      return emptySet()
    }

    val blockSize = footer.readLongLe(0)
    val totalSize = blockSize + Long.SIZE_BYTES
    if (
      blockSize < APK_SIGNING_BLOCK_FOOTER_SIZE ||
      totalSize > centralDirOffset ||
      totalSize > MAX_APK_SIGNING_BLOCK_SIZE
    ) {
      return emptySet()
    }

    val block = ByteArray(totalSize.toInt())
    seek(centralDirOffset - totalSize)
    readFully(block)
    if (block.readLongLe(0) != blockSize) {
      return emptySet()
    }

    val ids = mutableSetOf<Int>()
    var offset = Long.SIZE_BYTES
    val pairsEnd = block.size - APK_SIGNING_BLOCK_FOOTER_SIZE
    while (offset < pairsEnd) {
      if (offset + Long.SIZE_BYTES > pairsEnd) break
      val pairSize = block.readLongLe(offset)
      offset += Long.SIZE_BYTES
      if (pairSize < Integer.BYTES || pairSize > (pairsEnd - offset).toLong()) break
      ids += block.readIntLe(offset)
      offset += pairSize.toInt()
    }
    return ids
  }

  private fun RandomAccessFile.findCentralDirectory(): ZipCentralDirectory? {
    val fileLength = length()
    if (fileLength < ZIP_EOCD_MIN_SIZE) {
      return null
    }

    val readSize = minOf(fileLength, (ZIP_EOCD_MIN_SIZE + ZIP_MAX_COMMENT_SIZE).toLong()).toInt()
    val buffer = ByteArray(readSize)
    seek(fileLength - readSize)
    readFully(buffer)

    for (offset in readSize - ZIP_EOCD_MIN_SIZE downTo 0) {
      if (
        buffer.readIntLe(offset) == ZIP_EOCD_SIGNATURE &&
        buffer.readUnsignedShortLe(offset + ZIP_EOCD_COMMENT_LENGTH_OFFSET) == readSize - offset - ZIP_EOCD_MIN_SIZE
      ) {
        return ZipCentralDirectory(
          offset = buffer.readUnsignedIntLe(offset + ZIP_EOCD_CENTRAL_DIR_OFFSET),
          size = buffer.readUnsignedIntLe(offset + ZIP_EOCD_CENTRAL_DIR_SIZE_OFFSET)
        )
      }
    }
    return null
  }

  private fun ByteArray.isJarSignatureEntryName(offset: Int, length: Int): Boolean {
    return startsWithAscii(offset, length, META_INF_PREFIX) &&
      (
        endsWithAsciiIgnoreCase(offset, length, ".RSA") ||
          endsWithAsciiIgnoreCase(offset, length, ".DSA") ||
          endsWithAsciiIgnoreCase(offset, length, ".EC")
        )
  }

  private fun ByteArray.startsWithAscii(offset: Int, length: Int, prefix: String): Boolean {
    if (length < prefix.length) {
      return false
    }
    return prefix.indices.all { index -> this[offset + index].toInt() and 0xff == prefix[index].code }
  }

  private fun ByteArray.endsWithAsciiIgnoreCase(offset: Int, length: Int, suffix: String): Boolean {
    if (length < suffix.length) {
      return false
    }
    val start = offset + length - suffix.length
    return suffix.indices.all { index ->
      (this[start + index].toInt() and 0xff).toAsciiUppercase() == suffix[index].code
    }
  }

  private fun Int.toAsciiUppercase(): Int {
    return if (this in ASCII_LOWERCASE_A..ASCII_LOWERCASE_Z) {
      this - ASCII_CASE_DISTANCE
    } else {
      this
    }
  }

  private fun ByteArray.hasApkSigningBlockMagic(): Boolean {
    if (size < APK_SIGNING_BLOCK_FOOTER_SIZE) {
      return false
    }
    return APK_SIGNING_BLOCK_MAGIC.indices.all { index ->
      this[Long.SIZE_BYTES + index] == APK_SIGNING_BLOCK_MAGIC[index]
    }
  }

  private fun ByteArray.readLongLe(offset: Int): Long {
    var value = 0L
    for (i in 0 until Long.SIZE_BYTES) {
      value = value or ((this[offset + i].toLong() and 0xffL) shl (8 * i))
    }
    return value
  }

  private fun ByteArray.readIntLe(offset: Int): Int {
    return (this[offset].toInt() and 0xff) or
      ((this[offset + 1].toInt() and 0xff) shl 8) or
      ((this[offset + 2].toInt() and 0xff) shl 16) or
      ((this[offset + 3].toInt() and 0xff) shl 24)
  }

  private fun ByteArray.readUnsignedIntLe(offset: Int): Long {
    return readIntLe(offset).toLong() and 0xffffffffL
  }

  private fun ByteArray.readUnsignedShortLe(offset: Int): Int {
    return (this[offset].toInt() and 0xff) or ((this[offset + 1].toInt() and 0xff) shl 8)
  }

  private data class ApkSignatureProbe(
    val hasJarSignature: Boolean,
    val signingBlockIds: Set<Int>
  ) {
    companion object {
      val EMPTY = ApkSignatureProbe(false, emptySet())
    }
  }

  private data class ZipCentralDirectory(
    val offset: Long,
    val size: Long
  )

  private const val ZIP_EOCD_SIGNATURE = 0x06054b50
  private const val ZIP_CENTRAL_DIRECTORY_SIGNATURE = 0x02014b50
  private const val ZIP_CENTRAL_DIRECTORY_HEADER_SIZE = 46
  private const val ZIP_CENTRAL_DIRECTORY_FILE_NAME_LENGTH_OFFSET = 28
  private const val ZIP_CENTRAL_DIRECTORY_EXTRA_LENGTH_OFFSET = 30
  private const val ZIP_CENTRAL_DIRECTORY_COMMENT_LENGTH_OFFSET = 32
  private const val ZIP_EOCD_MIN_SIZE = 22
  private const val ZIP_EOCD_CENTRAL_DIR_SIZE_OFFSET = 12
  private const val ZIP_EOCD_CENTRAL_DIR_OFFSET = 16
  private const val ZIP_EOCD_COMMENT_LENGTH_OFFSET = 20
  private const val ZIP_MAX_COMMENT_SIZE = 65535
  private const val MAX_ZIP_CENTRAL_DIRECTORY_SIZE = 64L * 1024L * 1024L
  private const val APK_SIGNING_BLOCK_FOOTER_SIZE = 24
  private const val MAX_APK_SIGNING_BLOCK_SIZE = 32L * 1024L * 1024L
  private const val APK_SIGNATURE_SCHEME_V2_BLOCK_ID = 0x7109871a
  private const val APK_SIGNATURE_SCHEME_V3_BLOCK_ID = -262969152
  private const val APK_SIGNATURE_SCHEME_V31_BLOCK_ID = 0x1b93ad61
  private const val ASCII_LOWERCASE_A = 0x61
  private const val ASCII_LOWERCASE_Z = 0x7a
  private const val ASCII_CASE_DISTANCE = 0x20
  private const val META_INF_PREFIX = "META-INF/"

  private val APK_SIGNING_BLOCK_MAGIC = byteArrayOf(
    0x41,
    0x50,
    0x4b,
    0x20,
    0x53,
    0x69,
    0x67,
    0x20,
    0x42,
    0x6c,
    0x6f,
    0x63,
    0x6b,
    0x20,
    0x34,
    0x32
  )
}
