package com.absinthe.libchecker.domain.snapshot.comparison.usecase

import com.absinthe.libchecker.domain.app.detail.model.LibStringItem

/** Preserves legacy first-name matching, even when either archive contains duplicate names. */
internal class SnapshotNameIndex(private val oldItems: List<LibStringItem>) {
  private val firstByName = oldItems.asReversed().associateBy { it.name }
  private val matchedCounts = mutableMapOf<String, Int>()

  fun match(name: String): LibStringItem? {
    val first = firstByName[name] ?: return null
    matchedCounts[name] = (matchedCounts[name] ?: 0) + 1
    return first
  }

  fun remainingItems(): List<LibStringItem> {
    val remainingMatches = matchedCounts.toMutableMap()
    return oldItems.filter { item ->
      val count = remainingMatches[item.name] ?: 0
      if (count > 0) {
        remainingMatches[item.name] = count - 1
        false
      } else {
        true
      }
    }
  }
}
