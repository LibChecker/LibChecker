package com.absinthe.libchecker.domain.app

class GetRelatedAppListItemUseCase(
  private val appListRepository: AppListRepository,
  private val installedAppRepository: InstalledAppRepository
) {

  suspend operator fun invoke(packageName: String): RelatedAppListItem? {
    val item = appListRepository.getItem(packageName) ?: return null
    return RelatedAppListItem(
      item = item,
      packageInfo = installedAppRepository.getPackageInfo(packageName)
    )
  }
}
