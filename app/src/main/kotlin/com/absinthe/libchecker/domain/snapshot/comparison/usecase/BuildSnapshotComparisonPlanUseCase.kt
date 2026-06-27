package com.absinthe.libchecker.domain.snapshot.comparison.usecase

import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.domain.snapshot.SnapshotRepository
import com.absinthe.libchecker.domain.snapshot.comparison.archive.ArchiveSnapshotItem
import com.absinthe.libchecker.domain.snapshot.comparison.model.SnapshotComparisonLists
import com.absinthe.libchecker.domain.snapshot.comparison.model.SnapshotComparisonPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BuildSnapshotComparisonPlanUseCase(
  private val repository: SnapshotRepository
) {

  suspend operator fun invoke(
    leftTimeStamp: Long,
    leftArchive: ArchiveSnapshotItem?,
    rightTimeStamp: Long,
    rightArchive: ArchiveSnapshotItem?
  ): SnapshotComparisonPlan? {
    return when {
      leftTimeStamp > 0 && rightTimeStamp > 0 -> {
        if (leftTimeStamp == rightTimeStamp) {
          null
        } else {
          SnapshotComparisonPlan.TimestampRange(
            previousTimestamp = leftTimeStamp.coerceAtMost(rightTimeStamp),
            currentTimestamp = leftTimeStamp.coerceAtLeast(rightTimeStamp)
          )
        }
      }

      leftArchive != null && rightArchive != null -> {
        val requiresDifferentPackageConfirmation =
          leftArchive.snapshotItem.packageName != rightArchive.snapshotItem.packageName

        SnapshotComparisonPlan.ArchivePair(
          left = leftArchive,
          right = rightArchive,
          requiresDifferentPackageConfirmation = requiresDifferentPackageConfirmation
        )
      }

      else -> {
        buildSnapshotComparisonLists(
          leftTimeStamp = leftTimeStamp,
          leftPackage = leftArchive?.snapshotItem,
          rightTimeStamp = rightTimeStamp,
          rightPackage = rightArchive?.snapshotItem
        )?.let(SnapshotComparisonPlan::SnapshotLists)
      }
    }
  }

  private suspend fun buildSnapshotComparisonLists(
    leftTimeStamp: Long,
    leftPackage: SnapshotItem?,
    rightTimeStamp: Long,
    rightPackage: SnapshotItem?
  ): SnapshotComparisonLists? = withContext(Dispatchers.IO) {
    val leftSnapshots = when {
      leftPackage != null -> listOf(leftPackage)
      leftTimeStamp > 0 -> getSnapshots(leftTimeStamp, rightPackage?.packageName)
      else -> return@withContext null
    }
    val rightSnapshots = when {
      rightPackage != null -> listOf(rightPackage)
      rightTimeStamp > 0 -> getSnapshots(rightTimeStamp, leftPackage?.packageName)
      else -> return@withContext null
    }
    SnapshotComparisonLists(leftSnapshots, rightSnapshots)
  }

  private suspend fun getSnapshots(
    timestamp: Long,
    packageName: String?
  ): List<SnapshotItem> {
    return packageName?.let {
      repository.getSnapshot(timestamp, it)?.let(::listOf).orEmpty()
    } ?: repository.getSnapshots(timestamp)
  }
}
