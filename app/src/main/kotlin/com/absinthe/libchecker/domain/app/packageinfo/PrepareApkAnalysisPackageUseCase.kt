package com.absinthe.libchecker.domain.app.packageinfo

import android.content.ContentResolver
import android.content.pm.PackageInfo
import android.net.Uri
import android.os.Environment
import com.absinthe.libchecker.constant.Constants
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.buffer
import okio.sink
import okio.source
import timber.log.Timber

class PrepareApkAnalysisPackageUseCase(
  private val contentResolver: ContentResolver,
  private val getArchivePackageInfo: GetArchivePackageInfoUseCase
) {

  suspend operator fun invoke(
    cacheDir: File,
    uri: Uri
  ): Result = withContext(Dispatchers.IO) {
    val targetFile = File(cacheDir, Constants.TEMP_PACKAGE)
    if (targetFile.exists()) {
      targetFile.delete()
    }

    val inputStream = runCatching {
      contentResolver.openInputStream(uri)
    }.getOrNull() ?: return@withContext Result.Unreadable

    inputStream.use { input ->
      val fileSize = input.available()
      val freeSize = Environment.getExternalStorageDirectory().freeSpace
      Timber.d("fileSize=$fileSize, freeSize=$freeSize")

      if (freeSize <= fileSize * MIN_FREE_SPACE_RATIO) {
        return@withContext Result.NotEnoughStorage
      }

      targetFile.sink().buffer().use { sink ->
        input.source().buffer().use { source ->
          sink.writeAll(source)
        }
      }
    }

    val packageInfo = getArchivePackageInfo(targetFile)
      ?: return@withContext Result.InvalidPackage(targetFile)
    Result.Available(targetFile, packageInfo)
  }

  sealed interface Result {
    data class Available(val file: File, val packageInfo: PackageInfo) : Result
    data class InvalidPackage(val file: File) : Result
    data object Unreadable : Result
    data object NotEnoughStorage : Result
  }

  private companion object {
    private const val MIN_FREE_SPACE_RATIO = 1.5
  }
}
