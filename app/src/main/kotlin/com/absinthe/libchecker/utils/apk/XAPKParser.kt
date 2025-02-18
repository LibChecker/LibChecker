package com.absinthe.libchecker.utils.apk

import android.content.pm.PackageInfo
import com.absinthe.libchecker.LibCheckerApp
import com.absinthe.libchecker.compat.PackageManagerCompat
import com.absinthe.libchecker.compat.ZipFileCompat
import com.absinthe.libchecker.utils.extensions.use
import com.absinthe.libchecker.utils.fromJson
import java.io.File
import okio.buffer
import okio.sink
import okio.source
import timber.log.Timber

class XAPKParser(private val file: File, private val flags: Int = 0) {

  constructor(filePath: String) : this(File(filePath))

  fun getPackageInfo(): PackageInfo? {
    return runCatching { parse() }.onFailure {
      Timber.w(it, "Failed to parse XAPK file")
    }.getOrNull()
  }

  private fun parse(): PackageInfo {
    ZipFileCompat(file).use { zipFile ->
      val entry =
        zipFile.getEntry("manifest.json") ?: throw Exception("Failed to get manifest.json entry")
      val manifestIS = zipFile.getInputStream(entry)
      manifestIS.source().buffer().use {
        val json =
          it.readUtf8().fromJson<XAPKManifest>() ?: throw Exception("Failed to parse manifest.json")
        val rootDir = dumpApks(zipFile, json)
        val baseApkFile = File(rootDir, json.package_name + ".apk")
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
  }

  private fun dumpApks(zipFile: ZipFileCompat, json: XAPKManifest): File {
    Timber.d("Dumping apks")
    val rootDir = File(LibCheckerApp.app.externalCacheDir, json.package_name)
    rootDir.mkdir()

    json.split_apks.forEach { apkConfig ->
      val entry =
        zipFile.getEntry(apkConfig.file) ?: throw Exception("Failed to get split entry")
      zipFile.getInputStream(entry).source().buffer().use {
        val file = File(rootDir, apkConfig.file.replace("config.", "split_config."))
        file.sink().buffer().use { sink ->
          sink.writeAll(it)
        }
      }
    }
    return rootDir
  }
}
