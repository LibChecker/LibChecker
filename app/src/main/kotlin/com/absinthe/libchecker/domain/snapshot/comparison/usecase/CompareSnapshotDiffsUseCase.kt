package com.absinthe.libchecker.domain.snapshot.comparison.usecase

import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.domain.snapshot.SnapshotRepository
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.domain.snapshot.track.usecase.CompareTrackedSnapshotListsUseCase
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CompareSnapshotDiffsUseCase(
  private val snapshotRepository: SnapshotRepository,
  private val compareTrackedSnapshotLists: CompareTrackedSnapshotListsUseCase,
  private val compareSnapshotWithInstalledApps: CompareSnapshotWithInstalledAppsUseCase
) {
  private val comparisonMutex = Mutex()
  private var cachedTimestamp: Long? = null

  suspend operator fun invoke(
    previousTimestamp: Long,
    currentTimestamp: Long? = null,
    shouldClearDiff: Boolean = false,
    onProgress: (Int) -> Unit
  ): List<SnapshotDiffItem>? = comparisonMutex.withLock {
    // The derived cache has no timestamp column. Keep its baseline together with
    // all reads/writes, including comparisons canceled while changing snapshots.
    if (shouldClearDiff || (currentTimestamp == null && cachedTimestamp != previousTimestamp)) {
      snapshotRepository.deleteAllSnapshotDiffItems()
      cachedTimestamp = previousTimestamp.takeIf { currentTimestamp == null }
    }
    val diffItems = if (currentTimestamp == null) {
      compareSnapshotWithInstalledApps(previousTimestamp, onProgress)
    } else {
      compareTrackedSnapshotLists.byTimestamp(previousTimestamp, currentTimestamp)
    }
    currentCoroutineContext().ensureActive()
    diffItems
  }

  suspend fun compareLists(
    previousItems: List<SnapshotItem>,
    currentItems: List<SnapshotItem>
  ): List<SnapshotDiffItem>? {
    return compareTrackedSnapshotLists(previousItems, currentItems)
  }
}
