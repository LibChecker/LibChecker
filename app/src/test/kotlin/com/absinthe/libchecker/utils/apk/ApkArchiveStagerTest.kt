package com.absinthe.libchecker.utils.apk

import com.absinthe.libchecker.compat.ZipFileCompat
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ApkArchiveStagerTest {

  @get:Rule
  val temporaryFolder = TemporaryFolder()

  @Test
  fun `different source archives use isolated staging directories`() {
    val cache = temporaryFolder.newFolder("cache")
    val first = createArchive("应用😀.apks", mapOf("base.apk" to "one"))
    val second = createArchive("second.apks", mapOf("base.apk" to "two"))

    val firstFile = stage(first, cache).single()
    val secondFile = stage(second, cache).single()

    val legacyKey = MessageDigest.getInstance("SHA-256")
      .digest("${first.canonicalPath}\u0000${first.length()}\u0000${first.lastModified()}".toByteArray())
      .take(16)
      .joinToString("") { "%02x".format(it) }
    assertEquals(legacyKey, firstFile.parentFile!!.name)
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

  @Test
  fun `rejects declared entry and total limits and cleans failed staging`() {
    val archive = createArchive("limits.apks", mapOf("base.apk" to "data"))
    val cache = temporaryFolder.newFolder("limits-cache")
    ZipFileCompat(archive).use { zip ->
      for (sizes in listOf(listOf(4L * 1024 * 1024 * 1024 + 1), List(3) { 4L * 1024 * 1024 * 1024 })) {
        val entries = sizes.mapIndexed { index, size ->
          ZipEntry("base.apk").apply { this.size = size } to "$index.apk"
        }
        assertThrows(IllegalArgumentException::class.java) { ApkArchiveStager.stage(archive, cache, zip, entries) }
        assertTrue(cache.listFiles().orEmpty().isEmpty())
      }
      val mismatched = ZipEntry("base.apk").apply { size = 5 }
      assertThrows(IllegalStateException::class.java) {
        ApkArchiveStager.stage(archive, cache, zip, listOf(mismatched to "base.apk"))
      }
      assertFalse(cache.walkTopDown().any { it.name.endsWith(".tmp") || it.name == "base.apk" })
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
