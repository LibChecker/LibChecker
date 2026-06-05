package com.absinthe.libchecker.compat

import java.io.File
import java.io.InputStream
import java.lang.reflect.InvocationTargetException
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
    private const val APACHE_ZIP_FILE_DELEGATE_CLASS_NAME = "com.absinthe.libchecker.compat.ApacheZipFileDelegate"
    private const val OPEN_OPTION_CLASS_NAME = "java.nio.file.OpenOption"

    private val isApacheZipFileSupported by lazy(LazyThreadSafetyMode.PUBLICATION) {
      runCatching {
        Class.forName(OPEN_OPTION_CLASS_NAME)
      }.isSuccess
    }

    private fun openZipFile(file: File): IZipFile {
      val javaZipFile = runCatching {
        JavaZipFile(file)
      }.getOrElse { javaZipFailure ->
        if (isApacheZipFileSupported.not()) {
          throw javaZipFailure
        }

        return runCatching {
          openApacheZipFile(file)
        }.getOrElse { apacheZipFailure ->
          val failure = apacheZipFailure.unwrapInvocationTargetException()
          failure.addSuppressed(javaZipFailure)
          throw failure
        }
      }

      return JavaZipFileDelegate(javaZipFile)
    }

    private fun openApacheZipFile(file: File): IZipFile {
      val constructor = Class.forName(APACHE_ZIP_FILE_DELEGATE_CLASS_NAME)
        .asSubclass(IZipFile::class.java)
        .getDeclaredConstructor(File::class.java)
      constructor.isAccessible = true
      return constructor.newInstance(file)
    }

    private fun Throwable.unwrapInvocationTargetException(): Throwable {
      return (this as? InvocationTargetException)?.targetException ?: this
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
