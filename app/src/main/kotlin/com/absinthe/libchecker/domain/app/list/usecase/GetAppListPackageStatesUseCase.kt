package com.absinthe.libchecker.domain.app.list.usecase

import android.content.pm.PackageInfo
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.InstalledAppRepository
import com.absinthe.libchecker.domain.app.list.TRACE_APP_LIST_GET_APPLICATION_MAP
import com.absinthe.libchecker.domain.app.list.TRACE_APP_LIST_RESOLVE_PACKAGE_STATES
import com.absinthe.libchecker.domain.app.list.model.InstalledPackageState
import com.absinthe.libchecker.domain.app.list.traceAppListSection
import com.absinthe.libchecker.utils.FreezeUtils

class GetAppListPackageStatesUseCase(
  private val installedAppRepository: InstalledAppRepository
) {

  fun createSnapshot(): PackageStateSnapshot {
    return PackageStateSnapshot(
      applicationMap = traceAppListSection(TRACE_APP_LIST_GET_APPLICATION_MAP) {
        installedAppRepository.getApplicationMap()
      }
    )
  }

  suspend operator fun invoke(
    items: List<LCItem>,
    snapshot: PackageStateSnapshot? = null
  ): Map<String, InstalledPackageState> {
    if (items.isEmpty()) {
      return emptyMap()
    }
    val applicationMap = snapshot?.applicationMap ?: createSnapshot().applicationMap
    return traceAppListSection(TRACE_APP_LIST_RESOLVE_PACKAGE_STATES) {
      items.associate { item ->
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

  class PackageStateSnapshot internal constructor(
    internal val applicationMap: Map<String, PackageInfo>
  )
}
