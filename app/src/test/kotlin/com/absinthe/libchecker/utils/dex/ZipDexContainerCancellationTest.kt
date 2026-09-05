package com.absinthe.libchecker.utils.dex

import com.absinthe.libchecker.compat.IZipFile
import java.io.ByteArrayInputStream
import java.io.File
import java.util.Collections
import java.util.concurrent.CancellationException
import java.util.zip.ZipEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ZipDexContainerCancellationTest {
  @Test
  fun `cancellation stops full DEX copy at a block boundary and closes archive and input`() {
    for (limit in listOf(Long.MAX_VALUE, 1024L * 1024)) {
      var consumed = 0
      var streamClosed = false
      var archiveClosed = false
      val entry = ZipEntry("classes.dex").apply { size = 1024L * 1024 }
      val archive = object : IZipFile {
        override fun getZipEntries() = Collections.enumeration(listOf(entry))
        override fun getEntry(name: String) = entry
        override fun getInputStream(entry: ZipEntry) = object : ByteArrayInputStream(ByteArray(1024 * 1024)) {
          override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            return super.read(buffer, offset, length).also { if (it > 0) consumed += it }
          }
          override fun close() {
            streamClosed = true
            super.close()
          }
        }
        override fun close() {
          archiveClosed = true
        }
      }
      val container = object : ZipDexContainer2(
        File("unused"),
        null,
        limit,
        Runnable {
          if (consumed >= 8192) throw CancellationException()
        }
      ) {
        override fun getZipFile(): IZipFile = archive
      }
      assertThrows(CancellationException::class.java) { container.getEntry("classes.dex") }
      assertEquals(8192, consumed)
      assertTrue(streamClosed)
      assertTrue(archiveClosed)
    }
  }
}
