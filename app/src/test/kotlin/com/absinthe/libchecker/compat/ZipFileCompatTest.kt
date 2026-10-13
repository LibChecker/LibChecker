package com.absinthe.libchecker.compat

import java.util.zip.ZipEntry
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
