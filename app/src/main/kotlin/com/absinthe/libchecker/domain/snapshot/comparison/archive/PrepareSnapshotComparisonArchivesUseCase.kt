package com.absinthe.libchecker.domain.snapshot.comparison.archive

import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.domain.snapshot.comparison.model.SnapshotComparisonInput
import com.absinthe.libchecker.domain.snapshot.comparison.model.SnapshotComparisonInputs
import java.io.File
import kotlinx.coroutines.CancellationException

class PrepareSnapshotComparisonArchivesUseCase(
  private val buildArchiveSnapshotItemUseCase: BuildArchiveSnapshotItemUseCase
) {

  suspend operator fun invoke(request: Request): Result {
    var hasNotEnoughStorageSpace = false
    val leftArchive = buildArchiveOrNull(
      input = request.inputs.left,
      destinationFile = File(request.cacheDir, Constants.TEMP_PACKAGE),
      iconSize = request.iconSize,
      onNotEnoughStorageSpace = { hasNotEnoughStorageSpace = true }
    )
    val rightArchive = buildArchiveOrNull(
      input = request.inputs.right,
      destinationFile = File(request.cacheDir, Constants.TEMP_PACKAGE_2),
      iconSize = request.iconSize,
      onNotEnoughStorageSpace = { hasNotEnoughStorageSpace = true }
    )

    return Result(
      leftArchive = leftArchive,
      rightArchive = rightArchive,
      hasNotEnoughStorageSpace = hasNotEnoughStorageSpace
    )
  }

  fun clearCache(cacheDir: File) {
    File(cacheDir, Constants.TEMP_PACKAGE).delete()
    File(cacheDir, Constants.TEMP_PACKAGE_2).delete()
    File(cacheDir, ARCHIVE_PACKAGE_DIR).deleteRecursively()
  }

  private suspend fun buildArchiveOrNull(
    input: SnapshotComparisonInput,
    destinationFile: File,
    iconSize: Int,
    onNotEnoughStorageSpace: () -> Unit
  ): ArchiveSnapshotItem? {
    if (!input.isArchive || input.uri == null) {
      return null
    }

    return try {
      buildArchiveSnapshotItemUseCase(input.uri, destinationFile, iconSize)
    } catch (e: CancellationException) {
      throw e
    } catch (_: BuildArchiveSnapshotItemUseCase.NotEnoughStorageSpaceException) {
      onNotEnoughStorageSpace()
      null
    } catch (_: Exception) {
      null
    }
  }

  private companion object {
    const val ARCHIVE_PACKAGE_DIR = "apks"
  }

  data class Request(
    val inputs: SnapshotComparisonInputs,
    val cacheDir: File,
    val iconSize: Int
  )

  data class Result(
    val leftArchive: ArchiveSnapshotItem?,
    val rightArchive: ArchiveSnapshotItem?,
    val hasNotEnoughStorageSpace: Boolean
  )
}
