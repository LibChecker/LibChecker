package com.absinthe.libchecker.domain.app.detail

import android.content.pm.PackageInfo
import com.absinthe.libchecker.domain.app.InstalledAppRepository
import com.absinthe.libchecker.utils.PackageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ShouldShowStaticLibraryTabUseCase(
  private val installedAppRepository: InstalledAppRepository
) {

  suspend operator fun invoke(
    packageInfo: PackageInfo,
    packageName: String
  ): Boolean = withContext(Dispatchers.IO) {
    val sharedLibraryFiles = packageInfo.applicationInfo?.sharedLibraryFiles
    if (sharedLibraryFiles?.isNotEmpty() != true) {
      return@withContext false
    }

    val installedPackageInfo = installedAppRepository.getPackageInfo(packageName)
      ?: return@withContext false
    PackageUtils.getStaticLibs(installedPackageInfo).isNotEmpty()
  }
}
