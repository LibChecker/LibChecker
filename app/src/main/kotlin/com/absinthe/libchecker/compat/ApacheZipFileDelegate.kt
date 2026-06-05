package com.absinthe.libchecker.compat

import androidx.annotation.Keep
import java.io.File
import java.io.InputStream
import java.util.Enumeration
import java.util.zip.ZipEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipFile

@Keep
internal class ApacheZipFileDelegate(file: File) : IZipFile {
  private val zipFile = ZipFile.Builder().setFile(file).get()

  override fun getZipEntries(): Enumeration<out ZipEntry> {
    return zipFile.entries
  }

  override fun getInputStream(entry: ZipEntry): InputStream {
    val archiveEntry = entry as? ZipArchiveEntry
      ?: zipFile.getEntry(entry.name)
      ?: throw IllegalArgumentException("Entry ${entry.name} is not part of this ZIP file")
    return zipFile.getInputStream(archiveEntry)
  }

  override fun getEntry(name: String): ZipEntry? {
    return zipFile.getEntry(name)
  }

  override fun close() {
    zipFile.close()
  }
}
