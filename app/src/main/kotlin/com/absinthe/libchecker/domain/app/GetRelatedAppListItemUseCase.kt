package com.absinthe.libchecker.domain.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetRelatedAppListItemUseCase(
  private val appListRepository: AppListRepository,
  private val installedAppRepository: InstalledAppRepository
) {

  suspend operator fun invoke(packageName: String): RelatedAppListItem? =
    withContext(Dispatchers.IO) {
      val item = appListRepository.getItem(packageName) ?: return@withContext null
      RelatedAppListItem(
        item = item,
        packageInfo = installedAppRepository.getPackageInfo(packageName)
      )
    }
}
