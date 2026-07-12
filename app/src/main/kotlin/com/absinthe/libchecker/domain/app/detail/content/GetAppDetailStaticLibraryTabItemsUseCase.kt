package com.absinthe.libchecker.domain.app.detail.content

import android.content.pm.PackageInfo
import com.absinthe.libchecker.domain.app.detail.model.LibStringItemChip
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetAppDetailStaticLibraryTabItemsUseCase(
  private val installedAppRepository: InstalledAppRepository,
  private val getAppDetailStaticLibraryChipsUseCase: GetAppDetailStaticLibraryChipsUseCase
) {

  suspend operator fun invoke(
    packageInfo: PackageInfo,
    packageName: String,
    sortBySizeMode: Boolean
  ): List<LibStringItemChip> = withContext(Dispatchers.IO) {
    val sharedLibraryFiles = packageInfo.applicationInfo?.sharedLibraryFiles
    if (sharedLibraryFiles?.isNotEmpty() != true) {
      return@withContext emptyList()
    }

    val installedPackageInfo = installedAppRepository.getPackageInfo(packageName)
      ?: return@withContext emptyList()
    getAppDetailStaticLibraryChipsUseCase(installedPackageInfo, sortBySizeMode)
  }
}
