package com.absinthe.libchecker.utils.extensions

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MessageDigestExtensionsTest {

  @get:Rule
  val temporaryFolder = TemporaryFolder()

  @Test
  fun testFileMd5MatchesByteArrayMd5() {
    val file = temporaryFolder.newFile("test.txt")
    val content = ByteArray(16385) { it.toByte() }
    file.writeBytes(content)

    val expected = content.md5()
    val actual = file.md5()

    assertEquals(expected, actual)
  }

  @Test
  fun preservesHexCasePaddingAndSeparators() {
    val bytes = byteArrayOf(0, 15, 128.toByte(), 255.toByte())
    assertEquals("000F80FF", bytes.toHexString())
    assertEquals("00:0F:80:FF", bytes.toHexString(":"))
    assertEquals("00SS0FSS80SSFF", bytes.toHexString("ß"))
    assertEquals("", byteArrayOf().toHexString(":"))
    val file = temporaryFolder.newFile("abc.txt").apply { writeText("abc") }
    assertEquals("900150983CD24FB0D6963F7D28E17F72", file.md5())
  }
}
