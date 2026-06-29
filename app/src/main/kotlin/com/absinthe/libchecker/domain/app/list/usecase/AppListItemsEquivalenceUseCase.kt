package com.absinthe.libchecker.domain.app.list.usecase

import com.absinthe.libchecker.database.entity.LCItem

class AppListItemsEquivalenceUseCase {

  operator fun invoke(old: List<LCItem>, new: List<LCItem>): Boolean {
    if (old.size != new.size) {
      return false
    }
    return old.zip(new).all { (oldItem, newItem) ->
      oldItem.isEquivalentTo(newItem)
    }
  }

  private fun LCItem.isEquivalentTo(other: LCItem): Boolean {
    return packageName == other.packageName &&
      label == other.label &&
      versionName == other.versionName &&
      versionCode == other.versionCode &&
      lastUpdatedTime == other.lastUpdatedTime &&
      isSystem == other.isSystem &&
      abi == other.abi &&
      targetApi == other.targetApi &&
      variant == other.variant
  }
}
