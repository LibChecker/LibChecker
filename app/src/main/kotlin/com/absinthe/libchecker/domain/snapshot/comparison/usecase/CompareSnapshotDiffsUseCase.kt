package com.absinthe.libchecker.domain.snapshot.comparison.usecase

import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.domain.snapshot.SnapshotRepository
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.domain.snapshot.track.usecase.CompareTrackedSnapshotListsUseCase

class CompareSnapshotDiffsUseCase(
  private val snapshotRepository: SnapshotRepository,
  private val compareTrackedSnapshotLists: CompareTrackedSnapshotListsUseCase,
  private val compareSnapshotWithInstalledApps: CompareSnapshotWithInstalledAppsUseCase
) {

  suspend operator fun invoke(
    previousTimestamp: Long,
    currentTimestamp: Long? = null,
    shouldClearDiff: Boolean = false,
    onProgress: (Int) -> Unit
  ): List<SnapshotDiffItem>? {
    if (shouldClearDiff) {
      snapshotRepository.deleteAllSnapshotDiffItems()
    }

    val diffItems = if (currentTimestamp == null) {
      compareSnapshotWithInstalledApps(previousTimestamp, onProgress)
    } else {
      compareTrackedSnapshotLists.byTimestamp(previousTimestamp, currentTimestamp)
    } ?: return null

    return diffItems
  }

  suspend fun compareLists(
    previousItems: List<SnapshotItem>,
    currentItems: List<SnapshotItem>
  ): List<SnapshotDiffItem>? {
    return compareTrackedSnapshotLists(previousItems, currentItems)
  }
}
