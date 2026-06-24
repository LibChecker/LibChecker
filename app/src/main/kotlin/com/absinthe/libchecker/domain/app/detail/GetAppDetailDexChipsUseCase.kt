package com.absinthe.libchecker.domain.app.detail

import android.content.pm.PackageInfo
import com.absinthe.libchecker.annotation.DEX
import com.absinthe.libchecker.database.RulesRepository
import com.absinthe.libchecker.features.statistics.bean.LibStringItemChip
import com.absinthe.libchecker.utils.PackageUtils
import timber.log.Timber

class GetAppDetailDexChipsUseCase {

  suspend operator fun invoke(
    packageInfo: PackageInfo,
    sortBySizeMode: Boolean
  ): List<LibStringItemChip> {
    Timber.d("getDexChipList")
    val items = runCatching {
      PackageUtils.getDexList(packageInfo)
    }.onFailure {
      Timber.e(it)
    }.getOrDefault(emptyList())
    if (items.isEmpty()) {
      return emptyList()
    }

    return items.map {
      LibStringItemChip(it, RulesRepository.getRule(it.name, DEX, true))
    }.sortedWith(
      if (sortBySizeMode) {
        compareByDescending { it.item.name }
      } else {
        compareByDescending { it.rule != null }
      }
    )
  }
}
