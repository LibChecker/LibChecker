package com.absinthe.libchecker.domain.app

import com.absinthe.libchecker.utils.PackageUtils

class HasInstalledStaticLibrariesUseCase(
  private val installedAppRepository: InstalledAppRepository
) {

  operator fun invoke(packageName: String): Boolean {
    val packageInfo = installedAppRepository.getPackageInfo(packageName) ?: return false
    return PackageUtils.getStaticLibs(packageInfo).isNotEmpty()
  }
}
