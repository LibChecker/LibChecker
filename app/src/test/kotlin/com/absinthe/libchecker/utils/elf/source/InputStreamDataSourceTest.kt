package com.absinthe.libchecker.utils.elf.source

import java.io.ByteArrayInputStream
import java.io.FilterInputStream
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class InputStreamDataSourceTest {

  @Test
  fun readsOnlyEnoughOfLargeStreamForRequestedOffset() {
    val data = ByteArray(8 * 1024 * 1024)
    data[5_000] = 0x08
    data[5_001] = 0x07
    data[5_002] = 0x06
    data[5_003] = 0x05
    data[5_004] = 0x04
    data[5_005] = 0x03
    data[5_006] = 0x02
    data[5_007] = 0x01
    val input = CountingInputStream(ByteArrayInputStream(data))

    InputStreamDataSource(input).use { source ->
      assertEquals(0x0102030405060708L, source.readLong(5_000))
    }

    assertTrue("Expected a bounded read, but consumed ${input.bytesRead} bytes", input.bytesRead <= 8 * 1024)
  }

  @Test
  fun rejectsReadsPastTheValidPartOfAShortStream() {
    InputStreamDataSource(ByteArrayInputStream(byteArrayOf(1, 2, 3))).use { source ->
      assertEquals(1, source.readByte(0).toInt())
      assertThrows(IOException::class.java) {
        source.readInt(0)
      }
    }
  }

  @Test
  fun rejectsDistantOffsetWithoutRequestingOneHugeRead() {
    val input = GuardedInputStream(
      input = ByteArrayInputStream(ByteArray(4 * 1024)),
      maxBulkReadLength = 4 * 1024
    )

    InputStreamDataSource(input).use { source ->
      assertThrows(IOException::class.java) {
        source.readInt(4L * 1024 * 1024)
      }
    }

    assertTrue(input.maxRequestedLength <= 4 * 1024)
  }

  private class CountingInputStream(
    input: ByteArrayInputStream
  ) : FilterInputStream(input) {
    var bytesRead: Int = 0
      private set

    override fun read(): Int {
      return super.read().also {
        if (it >= 0) {
          bytesRead++
        }
      }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
      return super.read(buffer, offset, length).also {
        if (it > 0) {
          bytesRead += it
        }
      }
    }
  }

  private class GuardedInputStream(
    input: ByteArrayInputStream,
    private val maxBulkReadLength: Int
  ) : FilterInputStream(input) {
    var maxRequestedLength: Int = 0
      private set

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
      maxRequestedLength = maxOf(maxRequestedLength, length)
      if (length > maxBulkReadLength) {
        throw AssertionError("Attempted a $length-byte bulk read")
      }
      return super.read(buffer, offset, length)
    }
  }
}
