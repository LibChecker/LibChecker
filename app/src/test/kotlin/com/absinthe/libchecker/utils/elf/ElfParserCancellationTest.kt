package com.absinthe.libchecker.utils.elf

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ElfParserCancellationTest {
  @get:Rule
  val temporaryFolder = TemporaryFolder()

  @Test
  fun `page size scan checks cancellation between program headers`() {
    val bytes = ByteBuffer.allocate(64 + 1000 * 56).order(ByteOrder.LITTLE_ENDIAN)
    bytes.putInt(0, 0x464c457f)
    bytes.put(4, 2)
    bytes.put(5, 1)
    bytes.putLong(0x20, 64)
    bytes.putShort(0x36, 56)
    bytes.putShort(0x38, 1000)
    val file = temporaryFolder.newFile("headers.so").apply { writeBytes(bytes.array()) }
    var checks = 0
    var scanning = false
    ElfParser(file) {
      if (scanning && ++checks == 10) throw CancellationException()
    }.use { parser ->
      parser.parseHeader()
      scanning = true
      assertThrows(CancellationException::class.java) { parser.getMinPageSize() }
      assertEquals(10, checks)
    }
  }
}
