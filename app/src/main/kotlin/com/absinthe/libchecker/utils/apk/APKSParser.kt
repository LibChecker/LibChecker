package com.absinthe.libchecker.utils.apk

import android.content.pm.PackageInfo
import com.absinthe.libchecker.LibCheckerApp
import com.absinthe.libchecker.compat.PackageManagerCompat
import com.absinthe.libchecker.compat.ZipFileCompat
import com.absinthe.libchecker.utils.extensions.use
import java.io.File
import okio.buffer
import okio.sink
import okio.source
import timber.log.Timber

class APKSParser(private val file: File, private val flags: Int = 0) {

  constructor(filePath: String) : this(File(filePath))

  fun getPackageInfo(): PackageInfo? {
    return runCatching { parse() }.onFailure {
      Timber.w(it, "Failed to parse APKs file")
    }.getOrNull()
  }

  private fun parse(): PackageInfo {
    ZipFileCompat(file).use { zipFile ->
      val rootDir = dumpApks(zipFile)
      val baseApkFile = File(rootDir, "base.apk")
      return PackageManagerCompat.getPackageArchiveInfo(baseApkFile.path, flags)
        ?.also { pai ->
          pai.applicationInfo?.let { ai ->
            ai.sourceDir = baseApkFile.path
            ai.publicSourceDir = baseApkFile.path
            ai.splitSourceDirs = rootDir.listFiles()!!
              .filter { file -> file.name.startsWith("split_config.") }
              .map { file -> file.path }
              .toTypedArray()
          }
        } ?: throw Exception("Failed to get PackageArchiveInfo")
    }
  }

  private fun dumpApks(zipFile: ZipFileCompat): File {
    Timber.d("Dumping apks")
    val rootDir = File(LibCheckerApp.app.externalCacheDir, "apks")
    rootDir.mkdir()

    zipFile.getZipEntries()
      .asSequence()
      .filter { it.isDirectory.not() && it.name.endsWith(".apk") }
      .forEach { entry ->
        zipFile.getInputStream(entry).source().buffer().use {
          val file = File(rootDir, entry.name.substringAfterLast(File.separator))
          file.sink().buffer().use { sink ->
            sink.writeAll(it)
          }
        }
      }
    return rootDir
  }
}
