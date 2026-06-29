package com.absinthe.libchecker.domain.app.detail.content

import android.content.pm.PackageInfo
import com.absinthe.libchecker.annotation.STATIC
import com.absinthe.libchecker.database.RulesRepository
import com.absinthe.libchecker.domain.app.detail.model.LibStringItemChip
import com.absinthe.libchecker.utils.PackageUtils

class GetAppDetailStaticLibraryChipsUseCase {

  suspend operator fun invoke(
    packageInfo: PackageInfo,
    sortBySizeMode: Boolean
  ): List<LibStringItemChip> {
    val items = runCatching { PackageUtils.getStaticLibs(packageInfo) }.getOrDefault(emptyList())
    if (items.isEmpty()) {
      return emptyList()
    }

    return items.map {
      LibStringItemChip(it, RulesRepository.getRule(it.name, STATIC, false))
    }.sortedWith(
      if (sortBySizeMode) {
        compareByDescending { it.item.name }
      } else {
        compareByDescending { it.rule != null }
      }
    )
  }
}
