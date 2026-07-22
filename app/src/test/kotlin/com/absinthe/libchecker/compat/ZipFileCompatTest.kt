package com.absinthe.libchecker.compat

import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
