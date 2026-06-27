package com.absinthe.libchecker.domain.app.detail.packageinfo

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.absinthe.libchecker.domain.app.InstalledAppRepository
import com.absinthe.libchecker.utils.extensions.isArchivedPackage

class GetAppDetailPackageUseCase(
  private val installedAppRepository: InstalledAppRepository
) {

  suspend operator fun invoke(packageName: String): Result {
    val packageInfo = installedAppRepository.getPackageInfo(
      packageName = packageName,
      flags = PackageManager.GET_PERMISSIONS or PackageManager.GET_META_DATA,
      resolveFrozenArchiveInfo = false
    ) ?: return Result.NotFound

    return if (packageInfo.isArchivedPackage()) {
      Result.Archived
    } else {
      Result.Available(packageInfo)
    }
  }

  sealed interface Result {
    data class Available(val packageInfo: PackageInfo) : Result
    data object Archived : Result
    data object NotFound : Result
  }
}
