package com.absinthe.libchecker.domain.app.list.usecase

import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.InstalledAppRepository
import com.absinthe.libchecker.domain.app.list.model.InstalledPackageState
import com.absinthe.libchecker.utils.FreezeUtils

class GetAppListPackageStatesUseCase(
  private val installedAppRepository: InstalledAppRepository
) {

  suspend operator fun invoke(items: List<LCItem>): Map<String, InstalledPackageState> {
    if (items.isEmpty()) {
      return emptyMap()
    }
    val applicationMap = installedAppRepository.getApplicationMap()
    return items.associate { item ->
      item.packageName to if (item.packageName == Constants.EXAMPLE_PACKAGE) {
        InstalledPackageState(packageInfo = null, isFrozen = false)
      } else {
        applicationMap[item.packageName]?.let { packageInfo ->
          InstalledPackageState(
            packageInfo = packageInfo,
            isFrozen = packageInfo.applicationInfo?.let(FreezeUtils::isAppFrozen) ?: true
          )
        } ?: installedAppRepository.getPackageState(item.packageName)
      }
    }
  }
}
