package com.absinthe.libchecker.domain.snapshot.comparison

import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.domain.snapshot.ArchiveSnapshotItem
import com.absinthe.libchecker.domain.snapshot.BuildArchiveSnapshotItemUseCase
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
