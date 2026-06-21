package com.absinthe.libchecker.domain.snapshot

import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem

class CompareSnapshotListsUseCase(
  private val compareSnapshotItems: CompareSnapshotItemsUseCase
) {

  operator fun invoke(
    oldItems: List<SnapshotItem>,
    newItems: List<SnapshotItem>,
    trackPackageNames: Set<String>
  ): List<SnapshotDiffItem> {
    val oldMap = oldItems.associateBy { it.packageName }
    val newMap = newItems.associateBy { it.packageName }
    return compare(oldMap, newMap, trackPackageNames)
  }

  private fun compare(
    oldMap: Map<String, SnapshotItem>,
    newMap: Map<String, SnapshotItem>,
    trackPackageNames: Set<String>
  ): List<SnapshotDiffItem> {
    if (oldMap.isEmpty() || newMap.isEmpty()) {
      return emptyList()
    }

    val diffList = mutableListOf<SnapshotDiffItem>()

    oldMap.forEach { (packageName, oldItem) ->
      if (packageName !in newMap) {
        compareSnapshotItems(oldItem, null, trackPackageNames)?.let(diffList::add)
      }
    }

    newMap.forEach { (packageName, newItem) ->
      if (packageName !in oldMap) {
        compareSnapshotItems(null, newItem, trackPackageNames)?.let(diffList::add)
      }
    }

    oldMap.forEach { (packageName, oldItem) ->
      val newItem = newMap[packageName] ?: return@forEach
      if (newItem.versionCode != oldItem.versionCode || newItem.lastUpdatedTime != oldItem.lastUpdatedTime) {
        compareSnapshotItems(oldItem, newItem, trackPackageNames)?.let(diffList::add)
      }
    }

    return diffList
  }
}
