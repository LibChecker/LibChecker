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
    val content = "Hello LibChecker MD5 Stream Test".toByteArray(Charsets.UTF_8)
    file.writeBytes(content)

    val expected = content.md5()
    val actual = file.md5()

    assertEquals(expected, actual)
  }
}
