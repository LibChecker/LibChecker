package com.absinthe.libchecker.domain.app

import android.content.pm.PackageInfo
import com.absinthe.libchecker.features.statistics.bean.LibStringItemChip

class FilterAppDetailItemsUseCase {

  operator fun invoke(
    items: List<LibStringItemChip>,
    searchWords: String?,
    process: String?
  ): List<LibStringItemChip> {
    return items.asSequence()
      .filterBySearchWords(searchWords)
      .filter { process == null || it.item.process == process }
      .toList()
  }

  fun filterPermissions(
    items: List<LibStringItemChip>,
    searchWords: String?,
    process: String?
  ): List<LibStringItemChip> {
    return items.asSequence()
      .filterBySearchWords(searchWords)
      .filter {
        process == null ||
          it.item.process != PackageInfo.REQUESTED_PERMISSION_GRANTED.toString()
      }
      .toList()
  }

  private fun Sequence<LibStringItemChip>.filterBySearchWords(
    searchWords: String?
  ): Sequence<LibStringItemChip> {
    return filter {
      searchWords == null ||
        it.item.name.contains(searchWords, ignoreCase = true) ||
        it.item.source?.contains(searchWords, ignoreCase = true) == true
    }
  }
}
