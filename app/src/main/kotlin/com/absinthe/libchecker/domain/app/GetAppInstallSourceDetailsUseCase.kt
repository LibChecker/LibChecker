package com.absinthe.libchecker.domain.app

class GetAppInstallSourceDetailsUseCase(
  private val installedAppRepository: InstalledAppRepository
) {

  operator fun invoke(packageName: String): AppInstallSourceDetails? {
    val packageInfo = installedAppRepository.getPackageInfo(packageName) ?: return null
    return AppInstallSourceDetails(
      packageInfo = packageInfo,
      installSource = installedAppRepository.getInstallSource(packageName),
      showInstalledTime = !installedAppRepository.getPackageState(packageName).isFrozen
    )
  }
}
