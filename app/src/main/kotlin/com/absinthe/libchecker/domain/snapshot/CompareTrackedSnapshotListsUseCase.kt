package com.absinthe.libchecker.domain.snapshot

import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem

class CompareTrackedSnapshotListsUseCase(
  private val repository: SnapshotRepository,
  private val compareSnapshotLists: CompareSnapshotListsUseCase
) {

  suspend fun byTimestamp(
    oldTimestamp: Long,
    newTimestamp: Long
  ): List<SnapshotDiffItem>? {
    val oldItems = repository.getSnapshots(oldTimestamp)
    if (oldItems.isEmpty()) {
      return null
    }

    val newItems = repository.getSnapshots(newTimestamp)
    if (newItems.isEmpty()) {
      return null
    }

    return invoke(oldItems, newItems)
  }

  suspend operator fun invoke(
    oldItems: List<SnapshotItem>,
    newItems: List<SnapshotItem>
  ): List<SnapshotDiffItem>? {
    if (oldItems.isEmpty() || newItems.isEmpty()) {
      return null
    }

    val trackPackageNames = repository.getTrackItems()
      .asSequence()
      .map { it.packageName }
      .toSet()

    return compareSnapshotLists(oldItems, newItems, trackPackageNames)
  }
}
