package com.absinthe.libchecker.compat

import java.nio.charset.Charset
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ZipFileCompatTest {

  @get:Rule
  val temporaryFolder = TemporaryFolder()

  @Test
  fun `looks up an exact entry without enumerating the archive`() {
    val archive = temporaryFolder.newFile("lookup.zip")
    ZipOutputStream(archive.outputStream()).use { zip ->
      repeat(100) { index ->
        zip.putNextEntry(ZipEntry("assets/entry-$index.txt"))
        zip.write("value-$index".toByteArray())
        zip.closeEntry()
      }
    }

    ZipFileCompat(archive).use { zip ->
      val entry = checkNotNull(zip.getEntry("assets/entry-73.txt"))

      assertEquals("value-73", zip.getInputStream(entry).bufferedReader().use { it.readText() })
      assertNull(zip.getEntry("assets/missing.txt"))
    }
  }

  @Test
  fun `falls back to Apache for a ZIP name rejected by the platform decoder`() {
    val archive = temporaryFolder.newFile("legacy-name.zip")
    ZipOutputStream(archive.outputStream(), Charset.forName("ISO-8859-1")).use { zip ->
      zip.putNextEntry(ZipEntry("caf\u00e9.txt"))
      zip.write("fallback content".toByteArray())
      zip.closeEntry()
    }

    assertThrows(Exception::class.java) { ZipFile(archive).close() }
    ZipFileCompat(archive).use { zip ->
      val entry = zip.getZipEntries().nextElement()
      assertEquals("fallback content", zip.getInputStream(entry).bufferedReader().use { it.readText() })
    }
  }

  @Test
  fun `retains the platform failure when Apache also rejects a corrupt archive`() {
    val archive = temporaryFolder.newFile("corrupt.zip").apply { writeText("not a ZIP") }

    val error = assertThrows(Exception::class.java) { ZipFileCompat(archive).close() }

    assertEquals(1, error.suppressed.size)
    assertTrue(error.suppressed.single() is java.util.zip.ZipException)
  }

  @Test
  fun `does not fall back to ApacheZipFileDelegate when archive is unreadable`() {
    val archive = temporaryFolder.newFile("unreadable.apk").apply {
      writeBytes("not-readable".toByteArray())
    }
    assumeTrue(archive.setReadable(false))
    try {
      val error = assertThrows(Exception::class.java) {
        ZipFileCompat(archive).close()
      }
      assertTrue(
        "Unreadable files must fail directly without ApacheZipFileDelegate fallback: $error (${error.suppressed.toList()})",
        error.suppressed.isEmpty() && error.stackTrace.none { it.className.contains("ApacheZipFileDelegate") }
      )
    } finally {
      archive.setReadable(true)
    }
  }
}
