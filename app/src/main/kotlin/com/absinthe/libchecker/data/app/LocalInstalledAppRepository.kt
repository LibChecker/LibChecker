package com.absinthe.libchecker.data.app

import android.content.pm.PackageInfo
import com.absinthe.libchecker.domain.app.InstalledAppRepository
import com.absinthe.libchecker.domain.app.PackageChangeState
import com.absinthe.libchecker.utils.PackageUtils
import kotlinx.coroutines.flow.SharedFlow

object LocalInstalledAppRepository : InstalledAppRepository {

  override val packageChanges: SharedFlow<PackageChangeState> = LocalAppDataSource.packageChangeFlow

  override fun getApplicationList(forceUpdate: Boolean): List<PackageInfo> {
    return LocalAppDataSource.getApplicationList(forceUpdate)
  }

  override fun getApplicationMap(forceUpdate: Boolean): Map<String, PackageInfo> {
    return LocalAppDataSource.getApplicationMap(forceUpdate)
  }

  override fun getPackageInfo(packageName: String): PackageInfo? {
    return runCatching { PackageUtils.getPackageInfo(packageName) }.getOrNull()
  }
}
