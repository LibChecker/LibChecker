package com.absinthe.libchecker.domain.snapshot

import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem

class UpdateSnapshotDiffItemsUseCase {

  fun applyChange(
    currentItems: List<SnapshotDiffItem>,
    changedItem: SnapshotDiffItem
  ): Result {
    return Result(
      items = currentItems.replacePackageItem(changedItem),
      pendingRemovePackageNames = if (changedItem.deleted) {
        setOf(changedItem.packageName)
      } else {
        emptySet()
      }
    )
  }

  fun applyRemove(
    currentItems: List<SnapshotDiffItem>,
    packageName: String
  ): Result {
    return Result(
      items = currentItems.removePackage(packageName),
      pendingRemovePackageNames = setOf(packageName)
    )
  }

  private fun List<SnapshotDiffItem>.replacePackageItem(
    changedItem: SnapshotDiffItem
  ): List<SnapshotDiffItem> {
    return removePackage(changedItem.packageName) + changedItem
  }

  private fun List<SnapshotDiffItem>.removePackage(packageName: String): List<SnapshotDiffItem> {
    return filterNot { it.packageName == packageName }
  }

  data class Result(
    val items: List<SnapshotDiffItem>,
    val pendingRemovePackageNames: Set<String>
  )
}
