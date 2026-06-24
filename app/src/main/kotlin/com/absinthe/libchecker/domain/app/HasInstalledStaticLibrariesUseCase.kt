package com.absinthe.libchecker.domain.app

import com.absinthe.libchecker.utils.PackageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HasInstalledStaticLibrariesUseCase(
  private val installedAppRepository: InstalledAppRepository
) {

  suspend operator fun invoke(packageName: String): Boolean = withContext(Dispatchers.IO) {
    val packageInfo = installedAppRepository.getPackageInfo(packageName)
      ?: return@withContext false
    PackageUtils.getStaticLibs(packageInfo).isNotEmpty()
  }
}
