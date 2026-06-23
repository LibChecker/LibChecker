package com.absinthe.libchecker.domain.app

import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageInfo
import android.os.Environment
import android.provider.MediaStore
import com.absinthe.libchecker.compat.ZipFileCompat
import com.absinthe.libchecker.features.statistics.bean.LibStringItem
import com.absinthe.libchecker.utils.OsUtils
import java.io.File
import java.io.InputStream
import timber.log.Timber

class ExtractNativeLibraryUseCase(
  private val context: Context,
  private val applicationId: String
) {

  operator fun invoke(
    packageInfo: PackageInfo,
    item: LibStringItem,
    isApkPreview: Boolean
  ): Result<Unit> {
    Timber.d("Extract ELF: $item")

    return runCatching {
      if (isApkPreview) {
        error("not available in apk preview mode")
      }
      val elfSourcePath = item.source ?: error("elf source is null")
      val sourceFile = getSourceFile(packageInfo, item.process)

      ZipFileCompat(sourceFile).use { zipFile ->
        val entry = zipFile.getEntry(elfSourcePath) ?: error("Failed to find elf entry")
        zipFile.getInputStream(entry).use { inputStream ->
          writeElf(
            packageName = packageInfo.packageName,
            elfSourcePath = elfSourcePath,
            elfFileName = item.name,
            inputStream = inputStream
          )
        }
      }
    }
  }

  private fun getSourceFile(packageInfo: PackageInfo, sourceApkName: String?): File {
    val sourceDir = packageInfo.applicationInfo?.sourceDir ?: error("apk source file is invalid")
    val sourceFile = File(sourceDir).parentFile
      ?.resolve(sourceApkName ?: error("apk source file is invalid"))
      ?: error("apk source file is invalid")

    if (!sourceFile.exists() || !sourceFile.canRead()) {
      error("apk source file is invalid")
    }
    return sourceFile
  }

  private fun writeElf(
    packageName: String,
    elfSourcePath: String,
    elfFileName: String,
    inputStream: InputStream
  ) {
    val dir = buildTargetDir(packageName, elfSourcePath)
    Timber.d("dir = $dir")

    if (OsUtils.atLeastQ()) {
      writeElfWithMediaStore(dir, elfFileName, inputStream)
    } else {
      writeElfToDownloads(dir, elfFileName, inputStream)
    }
  }

  private fun buildTargetDir(packageName: String, elfSourcePath: String): String {
    val sourceParent = elfSourcePath.substringBeforeLast(File.separator, missingDelimiterValue = "")
    return listOf(applicationId, packageName, sourceParent)
      .filter(String::isNotBlank)
      .joinToString(File.separator)
  }

  private fun writeElfWithMediaStore(
    dir: String,
    elfFileName: String,
    inputStream: InputStream
  ) {
    val contentValues = ContentValues().apply {
      put(MediaStore.MediaColumns.DISPLAY_NAME, elfFileName)
      put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
      put(
        MediaStore.MediaColumns.RELATIVE_PATH,
        Environment.DIRECTORY_DOWNLOADS + File.separator + dir
      )
    }

    val uri = context.contentResolver.insert(
      MediaStore.Downloads.EXTERNAL_CONTENT_URI,
      contentValues
    ) ?: error("Failed to create MediaStore entry")

    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
      inputStream.copyTo(outputStream)
    } ?: error("Failed to open output stream")
  }

  private fun writeElfToDownloads(
    dir: String,
    elfFileName: String,
    inputStream: InputStream
  ) {
    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val parentDir = downloadsDir.resolve(dir)
    if (!parentDir.exists() && !parentDir.mkdirs()) {
      error("Failed to create ${parentDir.absolutePath}")
    }

    val targetFile = parentDir.resolve(elfFileName)
    targetFile.createNewFile()
    targetFile.outputStream().use { outputStream ->
      inputStream.copyTo(outputStream)
    }
  }
}
