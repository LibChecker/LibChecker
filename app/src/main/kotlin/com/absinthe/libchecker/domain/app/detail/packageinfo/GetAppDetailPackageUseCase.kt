package com.absinthe.libchecker.domain.app.detail.packageinfo

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.absinthe.libchecker.domain.app.InstalledAppRepository
import com.absinthe.libchecker.domain.app.detail.TRACE_DETAIL_GET_PACKAGE_INFO
import com.absinthe.libchecker.domain.app.detail.TRACE_DETAIL_LOAD_PACKAGE
import com.absinthe.libchecker.domain.app.detail.traceDetailSuspendSection
import com.absinthe.libchecker.utils.extensions.isArchivedPackage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetAppDetailPackageUseCase(
  private val installedAppRepository: InstalledAppRepository
) {

  suspend operator fun invoke(packageName: String): Result = withContext(Dispatchers.IO) {
    traceDetailSuspendSection(TRACE_DETAIL_LOAD_PACKAGE) {
      val packageInfo = traceDetailSuspendSection(TRACE_DETAIL_GET_PACKAGE_INFO) {
        installedAppRepository.getPackageInfo(
          packageName = packageName,
          flags = PackageManager.GET_PERMISSIONS or PackageManager.GET_META_DATA,
          resolveFrozenArchiveInfo = false
        )
      } ?: return@traceDetailSuspendSection Result.NotFound

      if (packageInfo.isArchivedPackage()) {
        Result.Archived
      } else {
        Result.Available(packageInfo)
      }
    }
  }

  sealed interface Result {
    data class Available(val packageInfo: PackageInfo) : Result
    data object Archived : Result
    data object NotFound : Result
  }
}
