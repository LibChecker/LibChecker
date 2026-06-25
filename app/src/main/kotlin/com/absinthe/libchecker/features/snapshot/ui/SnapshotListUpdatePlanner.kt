package com.absinthe.libchecker.features.snapshot.ui

import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.features.snapshot.ui.adapter.SnapshotAdapter

class SnapshotListUpdatePlanner {

  fun plan(request: Request): Plan {
    val sortedItems = request.sourceItems.asSequence()
      .filter { it.matchesSearchKeyword(request.searchKeyword) }
      .filterNot { request.hideNoComponentChanges && it.isNothingChanged() }
      .sortedByDescending { it.updateTime }
      .toList()

    if (request.highlightRefresh) {
      return Plan(
        items = sortedItems,
        particleRemovalItemIds = emptyList(),
        consumedRemovePackageNames = emptySet()
      )
    }

    val newPackageNames = sortedItems.mapTo(mutableSetOf()) { it.packageName }
    val pendingRemovePackageNames = request.pendingRemovePackageNames
    val deletedReplacementPackageNames = sortedItems.asSequence()
      .filter { it.deleted && it.packageName in pendingRemovePackageNames }
      .mapTo(mutableSetOf()) { it.packageName }
    val consumedRemovePackageNames = mutableSetOf<String>()
    val particleRemovalItemIds = request.currentItems.asSequence()
      .filter {
        val shouldAnimate = it.packageName !in newPackageNames ||
          it.packageName in deletedReplacementPackageNames
        if (shouldAnimate) {
          consumedRemovePackageNames += it.packageName
        }
        shouldAnimate
      }
      .map { SnapshotAdapter.stableItemIdFor(it) }
      .toList()

    return Plan(
      items = sortedItems,
      particleRemovalItemIds = particleRemovalItemIds,
      consumedRemovePackageNames = consumedRemovePackageNames
    )
  }

  private fun SnapshotDiffItem.matchesSearchKeyword(keyword: String): Boolean {
    if (keyword.isEmpty()) {
      return true
    }
    return packageName.contains(keyword, ignoreCase = true) ||
      labelDiff.old.contains(keyword, ignoreCase = true) ||
      labelDiff.new?.contains(keyword, ignoreCase = true) == true
  }

  data class Request(
    val currentItems: List<SnapshotDiffItem>,
    val sourceItems: List<SnapshotDiffItem>,
    val searchKeyword: String,
    val pendingRemovePackageNames: Set<String>,
    val hideNoComponentChanges: Boolean,
    val highlightRefresh: Boolean
  )

  data class Plan(
    val items: List<SnapshotDiffItem>,
    val particleRemovalItemIds: List<Long>,
    val consumedRemovePackageNames: Set<String>
  )
}
