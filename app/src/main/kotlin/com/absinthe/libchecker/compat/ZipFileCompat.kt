package com.absinthe.libchecker.compat

import java.io.File
import java.io.InputStream
import java.util.Enumeration
import java.util.zip.ZipEntry
import java.util.zip.ZipFile as JavaZipFile
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipFile

class ZipFileCompat(file: File) : IZipFile {
  private var javaZipFile: JavaZipFile? = null
  private var zipFile: ZipFile? = null

  init {
    runCatching {
      javaZipFile = JavaZipFile(file)
    }.onFailure {
      zipFile = ZipFile.Builder().setFile(file).get()
    }
  }

  constructor(path: String) : this(File(path))

  override fun getZipEntries(): Enumeration<out ZipEntry> {
    return javaZipFile?.entries()
      ?: zipFile?.entries
      ?: throw Exception("Failed to get ZipEntries")
  }

  override fun getInputStream(entry: ZipEntry): InputStream {
    return javaZipFile?.getInputStream(entry)
      ?: run {
        return if (entry is ZipArchiveEntry) {
          zipFile?.getInputStream(entry) ?: throw Exception("Failed to get InputStream")
        } else {
          throw Exception("Failed to get InputStream")
        }
      }
  }

  override fun getEntry(name: String): ZipEntry? {
    return javaZipFile?.getEntry(name)
      ?: zipFile?.getEntry(name)
  }

  override fun close() {
    javaZipFile?.close()
    zipFile?.close()
  }
}
