package com.absinthe.libchecker.domain.app.packageinfo

import android.content.ContentResolver
import android.content.pm.PackageInfo
import android.net.Uri
import com.absinthe.libchecker.constant.Constants
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okio.buffer
import okio.sink
import okio.source
import timber.log.Timber

class PrepareApkAnalysisPackageUseCase(
  private val contentResolver: ContentResolver,
  private val getArchivePackageInfo: GetArchivePackageInfoUseCase
) {

  suspend operator fun invoke(cacheDir: File, uri: Uri): Result {
    var targetFile: File? = null
    var delivered = false
    try {
      val result = withContext(Dispatchers.IO) {
        currentCoroutineContext().ensureActive()
        // Keep the recognized APK filename, with a directory owned by this request.
        val directory = File(cacheDir, "apk-${UUID.randomUUID()}")
        if (!directory.mkdir()) {
          throw IOException("Unable to create APK cache directory")
        }
        val file = File(directory, Constants.TEMP_PACKAGE)
        targetFile = file
        val input = contentResolver.openInputStream(uri) ?: return@withContext Result.Unreadable
        input.use {
          val fileSize = it.available()
          val freeSize = directory.usableSpace
          if (freeSize <= fileSize * 1.5) {
            return@withContext Result.NotEnoughStorage
          }
          file.sink().buffer().use { sink ->
            sink.writeAll(it.source())
          }
        }
        currentCoroutineContext().ensureActive()
        val packageInfo = getArchivePackageInfo(file)
        if (packageInfo == null) Result.InvalidPackage(file) else Result.Available(file, packageInfo)
      }
      delivered = result.file != null
      return result
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Timber.w(e, "Unable to prepare APK for analysis")
      return Result.Unreadable
    } finally {
      // This also handles cancellation when withContext cannot deliver its result.
      if (!delivered) {
        targetFile?.deleteApkAnalysisCache()
      }
    }
  }

  sealed interface Result {
    val file: File?
      get() = null

    data class Available(override val file: File, val packageInfo: PackageInfo) : Result
    data class InvalidPackage(override val file: File) : Result
    data object Unreadable : Result
    data object NotEnoughStorage : Result
  }
}

internal fun File.deleteApkAnalysisCache() {
  try {
    if (exists() && !delete()) {
      Timber.w("Unable to delete cached APK")
      return
    }
    // Never recursively remove files that this request did not create.
    parentFile?.delete()
  } catch (e: SecurityException) {
    Timber.w(e, "Unable to delete cached APK")
  }
}
