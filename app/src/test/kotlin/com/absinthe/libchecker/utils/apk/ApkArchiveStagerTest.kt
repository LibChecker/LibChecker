package com.absinthe.libchecker.utils.apk

import com.absinthe.libchecker.compat.ZipFileCompat
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ApkArchiveStagerTest {

  @get:Rule
  val temporaryFolder = TemporaryFolder()

  @Test
  fun `different source archives use isolated staging directories`() {
    val cache = temporaryFolder.newFolder("cache")
    val first = createArchive("first.apks", mapOf("base.apk" to "one"))
    val second = createArchive("second.apks", mapOf("base.apk" to "two"))

    val firstFile = stage(first, cache).single()
    val secondFile = stage(second, cache).single()

    assertNotEquals(firstFile.parentFile, secondFile.parentFile)
    assertEquals("one", firstFile.readText())
    assertEquals("two", secondFile.readText())
  }

  @Test
  fun `rejects entries that collapse to the same filename`() {
    val cache = temporaryFolder.newFolder("cache")
    val archive = createArchive(
      "duplicate.apks",
      linkedMapOf("first/base.apk" to "one", "second/base.apk" to "two")
    )

    assertThrows(IllegalArgumentException::class.java) {
      stage(archive, cache)
    }
  }

  private fun stage(archive: File, cache: File): List<File> {
    return ZipFileCompat(archive).use { zip ->
      val entries = zip.getZipEntries().asSequence().map { it to it.name }.toList()
      ApkArchiveStager.stage(archive, cache, zip, entries)
    }
  }

  private fun createArchive(name: String, entries: Map<String, String>): File {
    val archive = temporaryFolder.newFile(name)
    ZipOutputStream(archive.outputStream()).use { zip ->
      entries.forEach { (entryName, value) ->
        zip.putNextEntry(ZipEntry(entryName))
        zip.write(value.toByteArray())
        zip.closeEntry()
      }
    }
    return archive
  }
}
