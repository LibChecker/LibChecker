package com.absinthe.libchecker.utils.apk

import android.content.pm.PackageInfo
import com.absinthe.libchecker.LibCheckerApp
import com.absinthe.libchecker.compat.PackageManagerCompat
import com.absinthe.libchecker.compat.ZipFileCompat
import com.absinthe.libchecker.utils.extensions.requireAvailableCacheDir
import com.absinthe.libchecker.utils.extensions.use
import com.absinthe.libchecker.utils.manifest.ManifestReader
import java.io.File
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
      val apkFiles = dumpApks(zipFile)
      val baseApkFile = apkFiles.firstOrNull { it.name == "base.apk" }
        ?: throw Exception("Failed to get base.apk entry")
      return PackageManagerCompat.getPackageArchiveInfo(baseApkFile.path, flags)
        ?.also { pai ->
          pai.applicationInfo?.let { ai ->
            val splitApkFiles = apkFiles
              .filter { file -> file.name.startsWith("split_") && file.extension == "apk" }
            ai.sourceDir = baseApkFile.path
            ai.publicSourceDir = baseApkFile.path
            ai.splitSourceDirs = splitApkFiles.map { file -> file.path }.toTypedArray()
            pai.splitNames = resolveApksSplitNames(splitApkFiles) { splitApk ->
              ManifestReader.getManifestProperties(
                splitApk,
                arrayOf(SPLIT_NAME_ATTRIBUTE)
              )[SPLIT_NAME_ATTRIBUTE]?.toString()
            }
          }
        } ?: throw Exception("Failed to get PackageArchiveInfo")
    }
  }

  private fun dumpApks(zipFile: ZipFileCompat): List<File> {
    Timber.d("Dumping apks")
    val entries = zipFile.getZipEntries()
      .asSequence()
      .filter { it.isDirectory.not() && it.name.endsWith(".apk") }
      .map { it to it.name.substringAfterLast(File.separator) }
      .toList()
    val cacheRoot = File(LibCheckerApp.app.requireAvailableCacheDir(), "apks")
    return ApkArchiveStager.stage(file, cacheRoot, zipFile, entries)
  }

  private companion object {
    const val SPLIT_NAME_ATTRIBUTE = "split"
  }
}

internal fun resolveApksSplitNames(
  splitApkFiles: List<File>,
  readSplitName: (File) -> String?
): Array<String> {
  return splitApkFiles.map { splitApk ->
    readSplitName(splitApk)
      ?.takeIf(String::isNotBlank)
      ?: splitApk.nameWithoutExtension
  }.toTypedArray()
}
