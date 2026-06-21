package com.absinthe.libchecker.domain.app

import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.database.entity.LCItem

class GetAppListPackageStatesUseCase(
  private val installedAppRepository: InstalledAppRepository
) {

  suspend operator fun invoke(items: List<LCItem>): Map<String, InstalledPackageState> {
    return items.associate { item ->
      item.packageName to if (item.packageName == Constants.EXAMPLE_PACKAGE) {
        InstalledPackageState(packageInfo = null, isFrozen = false)
      } else {
        installedAppRepository.getPackageState(item.packageName)
      }
    }
  }
}
