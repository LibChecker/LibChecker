package com.absinthe.libchecker.compat

import java.io.File
import java.io.InputStream
import java.util.Enumeration
import java.util.zip.ZipEntry
import java.util.zip.ZipFile as JavaZipFile

class ZipFileCompat(file: File) : IZipFile {
  private val javaZipFile = JavaZipFile(file)

  constructor(path: String) : this(File(path))

  override fun getZipEntries(): Enumeration<out ZipEntry> {
    return javaZipFile.entries()
  }

  override fun getInputStream(entry: ZipEntry): InputStream {
    return javaZipFile.getInputStream(entry)
  }

  override fun getEntry(name: String): ZipEntry? {
    return javaZipFile.getEntry(name)
  }

  override fun close() {
    javaZipFile.close()
  }
}
