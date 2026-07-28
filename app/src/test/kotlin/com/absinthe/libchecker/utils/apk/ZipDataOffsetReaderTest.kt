package com.absinthe.libchecker.utils.apk

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ZipDataOffsetReaderTest {

  @get:Rule
  val temporaryFolder = TemporaryFolder()

  @Test
  fun `reads a standard ZIP entry offset and ignores EOCD signatures in comments`() {
    val entryName = "lib/arm64-v8a/libsample.so"
    val content = "standard-entry".toByteArray()
    val archive = temporaryFolder.newFile("standard.zip")
    ZipOutputStream(archive.outputStream()).use { zip ->
      zip.setComment("noise-PK\u0005\u0006-noise")
      zip.putNextEntry(
        ZipEntry(entryName).apply {
          method = ZipEntry.STORED
          size = content.size.toLong()
          compressedSize = content.size.toLong()
          crc = CRC32().apply { update(content) }.value
        }
      )
      zip.write(content)
      zip.closeEntry()
    }

    val offset = checkNotNull(ZipDataOffsetReader.readFromCentralDirectory(archive, setOf(entryName))[entryName])

    assertEntryContent(archive, offset, content)
  }

  @Test
  fun `reads a ZIP64 local header offset from the central directory extra field`() {
    val entryName = "lib/arm64-v8a/libsample.so"
    val content = "zip64-entry".toByteArray()
    val archiveBytes = createZip64Archive(entryName, content)
    val archive = temporaryFolder.newFile("zip64.zip").apply { writeBytes(archiveBytes) }

    val offset = checkNotNull(ZipDataOffsetReader.readFromCentralDirectory(archive, setOf(entryName))[entryName])

    assertEquals((LOCAL_FILE_HEADER_SIZE + entryName.toByteArray().size).toLong(), offset)
    assertEntryContent(archive, offset, content)
  }

  private fun assertEntryContent(file: File, offset: Long, expected: ByteArray) {
    val actual = ByteArray(expected.size)
    RandomAccessFile(file, "r").use { input ->
      input.seek(offset)
      input.readFully(actual)
    }
    assertArrayEquals(expected, actual)
  }

  private fun createZip64Archive(entryName: String, content: ByteArray): ByteArray {
    val name = entryName.toByteArray()
    val output = ByteArrayOutputStream()

    output.writeUInt32(LOCAL_FILE_HEADER_SIGNATURE)
    output.writeUInt16(ZIP64_VERSION)
    output.writeUInt16(0)
    output.writeUInt16(STORED_METHOD)
    output.writeUInt16(0)
    output.writeUInt16(0)
    output.writeUInt32(0)
    output.writeUInt32(content.size.toLong())
    output.writeUInt32(content.size.toLong())
    output.writeUInt16(name.size)
    output.writeUInt16(0)
    output.write(name)
    output.write(content)

    val centralDirectoryOffset = output.size().toLong()
    val zip64ExtraSize = Short.SIZE_BYTES * 2 + Long.SIZE_BYTES
    output.writeUInt32(CENTRAL_DIRECTORY_SIGNATURE)
    output.writeUInt16(ZIP64_VERSION)
    output.writeUInt16(ZIP64_VERSION)
    output.writeUInt16(0)
    output.writeUInt16(STORED_METHOD)
    output.writeUInt16(0)
    output.writeUInt16(0)
    output.writeUInt32(0)
    output.writeUInt32(content.size.toLong())
    output.writeUInt32(content.size.toLong())
    output.writeUInt16(name.size)
    output.writeUInt16(zip64ExtraSize)
    output.writeUInt16(0)
    output.writeUInt16(0)
    output.writeUInt16(0)
    output.writeUInt32(0)
    output.writeUInt32(UINT32_MAX)
    output.write(name)
    output.writeUInt16(ZIP64_EXTRA_FIELD_ID)
    output.writeUInt16(Long.SIZE_BYTES)
    output.writeUInt64(0)
    val centralDirectorySize = output.size() - centralDirectoryOffset

    val zip64EocdOffset = output.size().toLong()
    output.writeUInt32(ZIP64_EOCD_SIGNATURE)
    output.writeUInt64(ZIP64_EOCD_RECORD_SIZE)
    output.writeUInt16(ZIP64_VERSION)
    output.writeUInt16(ZIP64_VERSION)
    output.writeUInt32(0)
    output.writeUInt32(0)
    output.writeUInt64(1)
    output.writeUInt64(1)
    output.writeUInt64(centralDirectorySize)
    output.writeUInt64(centralDirectoryOffset)

    output.writeUInt32(ZIP64_EOCD_LOCATOR_SIGNATURE)
    output.writeUInt32(0)
    output.writeUInt64(zip64EocdOffset)
    output.writeUInt32(1)

    output.writeUInt32(EOCD_SIGNATURE)
    output.writeUInt16(0)
    output.writeUInt16(0)
    output.writeUInt16(UINT16_MAX)
    output.writeUInt16(UINT16_MAX)
    output.writeUInt32(UINT32_MAX)
    output.writeUInt32(UINT32_MAX)
    output.writeUInt16(0)
    return output.toByteArray()
  }

  private fun ByteArrayOutputStream.writeUInt16(value: Int) {
    repeat(Short.SIZE_BYTES) { index -> write(value ushr (index * 8) and 0xff) }
  }

  private fun ByteArrayOutputStream.writeUInt32(value: Long) {
    repeat(Int.SIZE_BYTES) { index -> write((value ushr (index * 8) and 0xff).toInt()) }
  }

  private fun ByteArrayOutputStream.writeUInt64(value: Long) {
    repeat(Long.SIZE_BYTES) { index -> write((value ushr (index * 8) and 0xff).toInt()) }
  }

  private companion object {
    private const val LOCAL_FILE_HEADER_SIGNATURE = 0x04034b50L
    private const val CENTRAL_DIRECTORY_SIGNATURE = 0x02014b50L
    private const val ZIP64_EOCD_SIGNATURE = 0x06064b50L
    private const val ZIP64_EOCD_LOCATOR_SIGNATURE = 0x07064b50L
    private const val EOCD_SIGNATURE = 0x06054b50L
    private const val ZIP64_EOCD_RECORD_SIZE = 44L
    private const val UINT32_MAX = 0xffffffffL
    private const val UINT16_MAX = 0xffff
    private const val ZIP64_EXTRA_FIELD_ID = 0x0001
    private const val ZIP64_VERSION = 45
    private const val STORED_METHOD = 0
    private const val LOCAL_FILE_HEADER_SIZE = 30
  }
}
