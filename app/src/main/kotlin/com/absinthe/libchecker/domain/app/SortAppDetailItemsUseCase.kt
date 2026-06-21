package com.absinthe.libchecker.domain.app

import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.features.statistics.bean.LibStringItemChip

class SortAppDetailItemsUseCase {

  operator fun invoke(
    items: List<LibStringItemChip>,
    @LibType type: Int,
    sortByLibraryMode: Boolean
  ): List<LibStringItemChip> {
    return if (sortByLibraryMode) {
      if (type == NATIVE) {
        items.sortedByDescending { it.item.size }
      } else {
        items.sortedByDescending { it.item.name }
      }
    } else {
      items.sortedWith(compareByDescending<LibStringItemChip> { it.rule != null }.thenBy { it.item.name })
    }
  }
}
