package com.absinthe.libchecker.compat

import java.io.File
import java.io.InputStream
import java.util.Enumeration
import java.util.zip.ZipEntry
import java.util.zip.ZipFile as JavaZipFile

class ZipFileCompat(file: File) : IZipFile {
  private val zipFile = openZipFile(file)

  constructor(path: String) : this(File(path))

  override fun getZipEntries(): Enumeration<out ZipEntry> {
    return zipFile.getZipEntries()
  }

  override fun getInputStream(entry: ZipEntry): InputStream {
    return zipFile.getInputStream(entry)
  }

  override fun getEntry(name: String): ZipEntry? {
    return zipFile.getEntry(name)
  }

  override fun close() {
    zipFile.close()
  }

  private companion object {
    private fun openZipFile(file: File): IZipFile {
      val javaZipFile = runCatching {
        JavaZipFile(file)
      }.getOrElse { javaZipFailure ->
        if (file.exists().not() || file.canRead().not()) {
          throw javaZipFailure
        }

        return runCatching {
          ApacheZipFileDelegate(file)
        }.getOrElse { apacheZipFailure ->
          apacheZipFailure.addSuppressed(javaZipFailure)
          throw apacheZipFailure
        }
      }

      return JavaZipFileDelegate(javaZipFile)
    }
  }
}

private class JavaZipFileDelegate(
  private val javaZipFile: JavaZipFile
) : IZipFile {
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
