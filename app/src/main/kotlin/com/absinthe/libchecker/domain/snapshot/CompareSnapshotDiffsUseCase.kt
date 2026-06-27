package com.absinthe.libchecker.domain.snapshot

import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.domain.snapshot.track.usecase.CompareTrackedSnapshotListsUseCase

private const val NO_TIMESTAMP = -1L
private const val TOP_APPS_LIMIT = 5

class CompareSnapshotDiffsUseCase(
  private val snapshotRepository: SnapshotRepository,
  private val compareTrackedSnapshotLists: CompareTrackedSnapshotListsUseCase,
  private val compareSnapshotWithInstalledApps: CompareSnapshotWithInstalledAppsUseCase,
  private val updateSnapshotTopApps: UpdateSnapshotTopAppsUseCase
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

    diffItems.updateTopApps(previousTimestamp)
    return diffItems
  }

  suspend fun compareLists(
    previousTimestamp: Long = NO_TIMESTAMP,
    previousItems: List<SnapshotItem>,
    currentItems: List<SnapshotItem>
  ): List<SnapshotDiffItem>? {
    val diffItems = compareTrackedSnapshotLists(previousItems, currentItems) ?: return null
    if (previousTimestamp != NO_TIMESTAMP) {
      diffItems.updateTopApps(previousTimestamp)
    }
    return diffItems
  }

  private suspend fun List<SnapshotDiffItem>.updateTopApps(timestamp: Long) {
    if (isEmpty()) {
      return
    }
    updateSnapshotTopApps(timestamp, topAppsForSnapshotSummary())
  }

  private fun List<SnapshotDiffItem>.topAppsForSnapshotSummary(): List<SnapshotDiffItem> {
    return subList(0, (size - 1).coerceAtMost(TOP_APPS_LIMIT))
  }
}
