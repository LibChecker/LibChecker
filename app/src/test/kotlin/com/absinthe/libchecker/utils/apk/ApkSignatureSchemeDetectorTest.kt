package com.absinthe.libchecker.utils.apk

import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ApkSignatureSchemeDetectorTest {

  @get:Rule
  val temporaryFolder = TemporaryFolder()

  @Test
  fun `detects the hybrid v3_2 block alongside v3 and v3_1`() {
    val apk = signedApk(
      "hybrid.apk",
      jarSigned = false,
      blockIds = listOf(V3_BLOCK_ID, V31_BLOCK_ID, V32_BLOCK_ID)
    )

    assertEquals(listOf("V3", "V3.1", "V3.2"), ApkSignatureSchemeDetector.detect(apk))
  }

  @Test
  fun `reports every scheme present in the signing block`() {
    val apk = signedApk(
      "all.apk",
      jarSigned = true,
      blockIds = listOf(V2_BLOCK_ID, V3_BLOCK_ID, V31_BLOCK_ID, V32_BLOCK_ID)
    )
    File("${apk.absolutePath}.idsig").writeBytes(byteArrayOf(0x1))

    assertEquals(
      listOf("V1", "V2", "V3", "V3.1", "V3.2", "V4"),
      ApkSignatureSchemeDetector.detect(apk)
    )
  }

  @Test
  fun `omits v3_2 when only the classical v3 block is present`() {
    val apk = signedApk("classical.apk", jarSigned = false, blockIds = listOf(V3_BLOCK_ID))

    assertEquals(listOf("V3"), ApkSignatureSchemeDetector.detect(apk))
  }

  /**
   * Builds a ZIP whose central directory is preceded by a synthetic APK Signing
   * Block carrying [blockIds] as ID-value pairs, which is all the detector reads.
   */
  private fun signedApk(name: String, jarSigned: Boolean, blockIds: List<Int>): File {
    val payload = ByteArrayOutputStream()
    ZipOutputStream(payload).use { zip ->
      zip.addStoredEntry("AndroidManifest.xml", "manifest".toByteArray())
      if (jarSigned) {
        zip.addStoredEntry("META-INF/CERT.RSA", "cert".toByteArray())
      }
    }

    val archive = payload.toByteArray()
    val eocdOffset = archive.findEocdOffset()
    val centralDirOffset = archive.readUInt32Le(eocdOffset + ZIP_EOCD_CENTRAL_DIR_OFFSET).toInt()
    val signingBlock = buildSigningBlock(blockIds)

    val output = ByteArrayOutputStream()
    output.write(archive, 0, centralDirOffset)
    output.write(signingBlock)
    output.write(archive, centralDirOffset, eocdOffset - centralDirOffset)
    output.write(archive, eocdOffset, ZIP_EOCD_CENTRAL_DIR_OFFSET)
    output.writeUInt32(centralDirOffset.toLong() + signingBlock.size)
    output.write(
      archive,
      eocdOffset + ZIP_EOCD_CENTRAL_DIR_OFFSET + Int.SIZE_BYTES,
      archive.size - eocdOffset - ZIP_EOCD_CENTRAL_DIR_OFFSET - Int.SIZE_BYTES
    )

    return temporaryFolder.newFile(name).apply { writeBytes(output.toByteArray()) }
  }

  private fun buildSigningBlock(blockIds: List<Int>): ByteArray {
    val pairs = ByteArrayOutputStream()
    blockIds.forEach { id ->
      val value = ByteArray(PAIR_VALUE_SIZE)
      pairs.writeUInt64((Int.SIZE_BYTES + value.size).toLong())
      pairs.writeUInt32Raw(id)
      pairs.write(value)
    }

    val pairBytes = pairs.toByteArray()
    // size field + magic + trailing size field, excluding the leading size field itself
    val blockSize = (pairBytes.size + Long.SIZE_BYTES + APK_SIGNING_BLOCK_MAGIC.size).toLong()
    val block = ByteArrayOutputStream()
    block.writeUInt64(blockSize)
    block.write(pairBytes)
    block.writeUInt64(blockSize)
    block.write(APK_SIGNING_BLOCK_MAGIC)
    return block.toByteArray()
  }

  private fun ZipOutputStream.addStoredEntry(name: String, content: ByteArray) {
    putNextEntry(
      ZipEntry(name).apply {
        method = ZipEntry.STORED
        size = content.size.toLong()
        compressedSize = content.size.toLong()
        crc = CRC32().apply { update(content) }.value
      }
    )
    write(content)
    closeEntry()
  }

  private fun ByteArray.findEocdOffset(): Int {
    for (offset in size - ZIP_EOCD_MIN_SIZE downTo 0) {
      if (readUInt32Le(offset) == ZIP_EOCD_SIGNATURE) {
        return offset
      }
    }
    throw AssertionError("EOCD not found in synthetic archive")
  }

  private fun ByteArray.readUInt32Le(offset: Int): Long {
    var value = 0L
    for (index in 0 until Int.SIZE_BYTES) {
      value = value or ((this[offset + index].toLong() and 0xffL) shl (8 * index))
    }
    return value
  }

  private fun ByteArrayOutputStream.writeUInt32(value: Long) {
    for (index in 0 until Int.SIZE_BYTES) {
      write(((value shr (8 * index)) and 0xffL).toInt())
    }
  }

  private fun ByteArrayOutputStream.writeUInt32Raw(value: Int) {
    writeUInt32(value.toLong() and 0xffffffffL)
  }

  private fun ByteArrayOutputStream.writeUInt64(value: Long) {
    for (index in 0 until Long.SIZE_BYTES) {
      write(((value shr (8 * index)) and 0xffL).toInt())
    }
  }

  private companion object {
    const val PAIR_VALUE_SIZE = 8
    const val ZIP_EOCD_SIGNATURE = 0x06054b50L
    const val ZIP_EOCD_MIN_SIZE = 22
    const val ZIP_EOCD_CENTRAL_DIR_OFFSET = 16
    const val V2_BLOCK_ID = 0x7109871a
    const val V3_BLOCK_ID = -262969152
    const val V31_BLOCK_ID = 0x1b93ad61
    const val V32_BLOCK_ID = 0x70e1c89f

    val APK_SIGNING_BLOCK_MAGIC = "APK Sig Block 42".toByteArray()
  }
}
