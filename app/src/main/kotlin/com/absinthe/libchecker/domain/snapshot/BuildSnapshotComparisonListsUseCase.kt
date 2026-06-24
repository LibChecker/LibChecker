package com.absinthe.libchecker.domain.snapshot

import com.absinthe.libchecker.database.entity.SnapshotItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BuildSnapshotComparisonListsUseCase(
  private val repository: SnapshotRepository
) {

  suspend operator fun invoke(
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

data class SnapshotComparisonLists(
  val left: List<SnapshotItem>,
  val right: List<SnapshotItem>
)
