package com.absinthe.libchecker.domain.snapshot.comparison.usecase

import com.absinthe.libchecker.domain.app.detail.model.LibStringItem
import com.absinthe.libchecker.domain.snapshot.model.ADDED
import com.absinthe.libchecker.domain.snapshot.model.CHANGED
import com.absinthe.libchecker.domain.snapshot.model.MOVED
import com.absinthe.libchecker.domain.snapshot.model.REMOVED

// Emit differences directly so counting does not allocate display rows or formatted text.
internal inline fun visitNamedSnapshotDiff(
  oldItems: List<LibStringItem>,
  newItems: List<LibStringItem>,
  changed: (LibStringItem, LibStringItem) -> Boolean,
  emit: (Int, LibStringItem?, LibStringItem?) -> Unit
) {
  val index = SnapshotNameIndex(oldItems)
  val added = mutableListOf<LibStringItem>()
  for (item in newItems) {
    val old = index.match(item.name)
    if (old == null) {
      added.add(item)
    } else if (changed(old, item)) {
      emit(CHANGED, old, item)
    }
  }
  index.remainingItems().forEach { emit(REMOVED, it, null) }
  added.forEach { emit(ADDED, null, it) }
}

internal inline fun visitComponentSnapshotDiff(
  oldItems: Set<String>,
  newItems: Set<String>,
  emit: (Int, String?, String?) -> Unit
) {
  val removed = (oldItems - newItems).toMutableSet()
  val added = (newItems - oldItems).toMutableSet()
  val movedOld = mutableSetOf<String>()
  val movedNew = mutableSetOf<String>()
  // Preserve first-short-name matching, including multiple new names matching one old name.
  val firstRemoved = removed.reversed().associateBy { it.substringAfterLast('.') }
  for (name in added) {
    firstRemoved[name.substringAfterLast('.')]?.let { old ->
      emit(MOVED, old, name)
      movedOld.add(old)
      movedNew.add(name)
    }
  }
  removed.removeAll(movedOld)
  added.removeAll(movedNew)
  removed.forEach { emit(REMOVED, it, null) }
  added.forEach { emit(ADDED, null, it) }
}

internal inline fun visitSetSnapshotDiff(
  oldItems: Set<String>,
  newItems: Set<String>,
  emit: (Int, String?, String?) -> Unit
) {
  for (item in oldItems) {
    if (item !in newItems) {
      emit(REMOVED, item, null)
    }
  }
  for (item in newItems) {
    if (item !in oldItems) {
      emit(ADDED, null, item)
    }
  }
}

internal inline fun <T : Any> visitKeyedSnapshotDiff(
  oldItems: Map<String, T>,
  newItems: Map<String, T>,
  emit: (Int, T?, T?) -> Unit
) {
  for ((name, item) in newItems) {
    val old = oldItems[name]
    if (old == null) {
      emit(ADDED, null, item)
    } else if (old != item) {
      emit(CHANGED, old, item)
    }
  }
  for ((name, item) in oldItems) {
    if (name !in newItems) emit(REMOVED, item, null)
  }
}
