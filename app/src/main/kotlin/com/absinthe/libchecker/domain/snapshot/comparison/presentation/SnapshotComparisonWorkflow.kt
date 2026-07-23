package com.absinthe.libchecker.domain.snapshot.comparison.presentation

import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.domain.snapshot.SnapshotRepository
import com.absinthe.libchecker.domain.snapshot.comparison.archive.ArchiveSnapshotItem
import com.absinthe.libchecker.domain.snapshot.comparison.archive.BuildArchiveSnapshotItemUseCase
import com.absinthe.libchecker.domain.snapshot.comparison.model.SnapshotComparisonInput
import com.absinthe.libchecker.domain.snapshot.comparison.model.SnapshotComparisonInputs
import com.absinthe.libchecker.domain.snapshot.comparison.model.SnapshotComparisonLists
import com.absinthe.libchecker.domain.snapshot.comparison.model.SnapshotComparisonPlan
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SnapshotComparisonWorkflow(
  private val repository: SnapshotRepository,
  private val buildArchiveSnapshotItem: BuildArchiveSnapshotItemUseCase
) {
  suspend fun prepareArchives(
    inputs: SnapshotComparisonInputs,
    cacheDir: File,
    iconSize: Int
  ): ArchivePreparation {
    var hasNotEnoughStorageSpace = false
    val leftArchive = buildArchiveOrNull(
      input = inputs.left,
      destinationFile = File(cacheDir, Constants.TEMP_PACKAGE),
      iconSize = iconSize,
      onNotEnoughStorageSpace = { hasNotEnoughStorageSpace = true }
    )
    val rightArchive = buildArchiveOrNull(
      input = inputs.right,
      destinationFile = File(cacheDir, Constants.TEMP_PACKAGE_2),
      iconSize = iconSize,
      onNotEnoughStorageSpace = { hasNotEnoughStorageSpace = true }
    )
    return ArchivePreparation(leftArchive, rightArchive, hasNotEnoughStorageSpace)
  }

  fun clearArchiveCache(cacheDir: File) {
    File(cacheDir, Constants.TEMP_PACKAGE).delete()
    File(cacheDir, Constants.TEMP_PACKAGE_2).delete()
    File(cacheDir, ARCHIVE_PACKAGE_DIR).deleteRecursively()
  }

  suspend fun buildPlan(
    inputs: SnapshotComparisonInputs,
    leftArchive: ArchiveSnapshotItem?,
    rightArchive: ArchiveSnapshotItem?
  ): SnapshotComparisonPlan? {
    val leftTimestamp = inputs.left.timestamp
    val rightTimestamp = inputs.right.timestamp
    return when {
      leftTimestamp > 0 && rightTimestamp > 0 -> {
        if (leftTimestamp == rightTimestamp) {
          null
        } else {
          SnapshotComparisonPlan.TimestampRange(
            previousTimestamp = leftTimestamp.coerceAtMost(rightTimestamp),
            currentTimestamp = leftTimestamp.coerceAtLeast(rightTimestamp)
          )
        }
      }

      leftArchive != null && rightArchive != null -> {
        SnapshotComparisonPlan.ArchivePair(
          left = leftArchive,
          right = rightArchive,
          requiresDifferentPackageConfirmation =
          leftArchive.snapshotItem.packageName != rightArchive.snapshotItem.packageName
        )
      }

      else -> buildComparisonLists(
        leftTimestamp = leftTimestamp,
        leftPackage = leftArchive?.snapshotItem,
        rightTimestamp = rightTimestamp,
        rightPackage = rightArchive?.snapshotItem
      )?.let(SnapshotComparisonPlan::SnapshotLists)
    }
  }

  fun buildPairDiff(left: SnapshotItem, right: SnapshotItem): SnapshotDiffItem {
    return SnapshotDiffItem(
      packageName = "${left.packageName}/${right.packageName}",
      updateTime = -1,
      labelDiff = SnapshotDiffItem.DiffNode(left.label, right.label),
      versionNameDiff = SnapshotDiffItem.DiffNode(left.versionName, right.versionName),
      versionCodeDiff = SnapshotDiffItem.DiffNode(left.versionCode, right.versionCode),
      abiDiff = SnapshotDiffItem.DiffNode(left.abi, right.abi),
      targetApiDiff = SnapshotDiffItem.DiffNode(left.targetApi, right.targetApi),
      compileSdkDiff = SnapshotDiffItem.DiffNode(left.compileSdk, right.compileSdk),
      minSdkDiff = SnapshotDiffItem.DiffNode(left.minSdk, right.minSdk),
      nativeLibsDiff = SnapshotDiffItem.DiffNode(left.nativeLibs, right.nativeLibs),
      servicesDiff = SnapshotDiffItem.DiffNode(left.services, right.services),
      activitiesDiff = SnapshotDiffItem.DiffNode(left.activities, right.activities),
      receiversDiff = SnapshotDiffItem.DiffNode(left.receivers, right.receivers),
      providersDiff = SnapshotDiffItem.DiffNode(left.providers, right.providers),
      permissionsDiff = SnapshotDiffItem.DiffNode(left.permissions, right.permissions),
      metadataDiff = SnapshotDiffItem.DiffNode(left.metadata, right.metadata),
      packageSizeDiff = SnapshotDiffItem.DiffNode(left.packageSize, right.packageSize),
      isTrackItem = false,
      archivedDiff = SnapshotDiffItem.DiffNode(left.isArchived, right.isArchived)
    )
  }

  private suspend fun buildArchiveOrNull(
    input: SnapshotComparisonInput,
    destinationFile: File,
    iconSize: Int,
    onNotEnoughStorageSpace: () -> Unit
  ): ArchiveSnapshotItem? {
    if (!input.isArchive || input.uri == null) return null
    return try {
      buildArchiveSnapshotItem(input.uri, destinationFile, iconSize)
    } catch (e: CancellationException) {
      throw e
    } catch (_: BuildArchiveSnapshotItemUseCase.NotEnoughStorageSpaceException) {
      onNotEnoughStorageSpace()
      null
    } catch (_: Exception) {
      null
    }
  }

  private suspend fun buildComparisonLists(
    leftTimestamp: Long,
    leftPackage: SnapshotItem?,
    rightTimestamp: Long,
    rightPackage: SnapshotItem?
  ): SnapshotComparisonLists? = withContext(Dispatchers.IO) {
    val leftSnapshots = when {
      leftPackage != null -> listOf(leftPackage)
      leftTimestamp > 0 -> getSnapshots(leftTimestamp, rightPackage?.packageName)
      else -> return@withContext null
    }
    val rightSnapshots = when {
      rightPackage != null -> listOf(rightPackage)
      rightTimestamp > 0 -> getSnapshots(rightTimestamp, leftPackage?.packageName)
      else -> return@withContext null
    }
    SnapshotComparisonLists(leftSnapshots, rightSnapshots)
  }

  private suspend fun getSnapshots(timestamp: Long, packageName: String?): List<SnapshotItem> {
    return packageName?.let {
      repository.getSnapshot(timestamp, it)?.let(::listOf).orEmpty()
    } ?: repository.getSnapshots(timestamp)
  }

  data class ArchivePreparation(
    val leftArchive: ArchiveSnapshotItem?,
    val rightArchive: ArchiveSnapshotItem?,
    val hasNotEnoughStorageSpace: Boolean
  )

  private companion object {
    const val ARCHIVE_PACKAGE_DIR = "apks"
  }
}
