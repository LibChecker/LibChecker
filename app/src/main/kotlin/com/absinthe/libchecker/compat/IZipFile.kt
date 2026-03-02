package com.absinthe.libchecker.compat

import java.io.Closeable
import java.io.InputStream
import java.util.Enumeration
import java.util.zip.ZipEntry

interface IZipFile : Closeable {
  fun getZipEntries(): Enumeration<out ZipEntry>

  fun getInputStream(entry: ZipEntry): InputStream

  fun getEntry(name: String): ZipEntry?
}
