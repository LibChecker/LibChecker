package com.absinthe.libchecker.domain.app

import android.content.Context
import android.content.pm.PackageInfo
import androidx.core.content.FileProvider
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.getAppName
import com.absinthe.libchecker.utils.extensions.getVersionCode
import com.absinthe.libchecker.utils.extensions.isSplitsApk
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class PrepareAppPackageShareFileUseCase(
  private val context: Context,
  private val applicationId: String,
  private val installedAppRepository: InstalledAppRepository
) {

  private val packageManager = context.packageManager
  private val illegalFilenameChars = Regex("""[\\/:*?"<>|]""")

  operator fun invoke(cacheDir: File, packageName: String): AppPackageShareFile {
    val packageInfo = installedAppRepository.getPackageInfo(packageName)
      ?: error("PackageInfo not found for $packageName")
    val applicationInfo = packageInfo.applicationInfo
      ?: error("No ApplicationInfo")
    val sourceDir = applicationInfo.sourceDir
      ?: error("No sourceDir")
    val sourceFile = File(sourceDir)
    val splitFiles = PackageUtils.getSplitsSourceDir(packageInfo)
      ?.map { path -> File(path) }
      ?.filter { it.exists() }
      ?: emptyList()
    val hasSplits = packageInfo.isSplitsApk() && splitFiles.isNotEmpty()
    val targetDir = File(cacheDir, CACHE_DIR_NAME)
    if (!targetDir.exists() && !targetDir.mkdirs()) {
      error("Failed to create ${targetDir.absolutePath}")
    }

    val targetFile = File(targetDir, buildSuggestedPackageFileName(packageInfo, hasSplits))
    val latestSourceTimestamp = (listOf(sourceFile) + splitFiles).maxOf { it.lastModified() }
    val needRebuild = !targetFile.exists() ||
      targetFile.lastModified() < latestSourceTimestamp ||
      (!hasSplits && targetFile.length() != sourceFile.length()) ||
      (hasSplits && targetFile.length() == 0L)

    if (needRebuild) {
      if (targetFile.exists()) {
        targetFile.delete()
      }
      if (hasSplits) {
        buildApksArchive(targetFile, sourceFile, splitFiles)
      } else {
        sourceFile.inputStream().use { input ->
          targetFile.outputStream().use { output ->
            input.copyTo(output)
          }
        }
      }
      targetFile.setLastModified(latestSourceTimestamp)
    }

    return AppPackageShareFile(
      file = targetFile,
      mimeType = inferMimeType(targetFile),
      contentUri = FileProvider.getUriForFile(
        context,
        "$applicationId.fileprovider",
        targetFile
      )
    )
  }

  private fun buildSuggestedPackageFileName(
    packageInfo: PackageInfo,
    hasSplits: Boolean
  ): String {
    val appName = packageInfo.getAppName(packageManager).takeUnless { it.isNullOrBlank() }
      ?: packageInfo.packageName
    val versionName = packageInfo.versionName.orEmpty()
    val versionCode = packageInfo.getVersionCode()
    val raw = "${appName}_${versionName}_$versionCode"
    val sanitized = raw.replace(illegalFilenameChars, "_")
    val extension = if (hasSplits) "apks" else "apk"
    return "$sanitized.$extension"
  }

  private fun buildApksArchive(targetFile: File, baseApk: File, splits: List<File>) {
    val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp")
    ZipOutputStream(BufferedOutputStream(FileOutputStream(tempFile))).use { zos ->
      fun putFile(file: File) {
        val entry = ZipEntry(file.name)
        entry.time = file.lastModified()
        zos.putNextEntry(entry)
        file.inputStream().use { input ->
          input.copyTo(zos)
        }
        zos.closeEntry()
      }
      putFile(baseApk)
      splits.forEach { split ->
        putFile(split)
      }
    }

    if (!tempFile.renameTo(targetFile)) {
      targetFile.delete()
      tempFile.copyTo(targetFile, overwrite = true)
      tempFile.delete()
    }
  }

  private fun inferMimeType(file: File): String {
    return if (file.extension.equals("apks", ignoreCase = true)) {
      "application/octet-stream"
    } else {
      MIMETYPE_APK
    }
  }

  private companion object {
    const val CACHE_DIR_NAME = "shared_apk"
    const val MIMETYPE_APK = "application/vnd.android.package-archive"
  }
}
