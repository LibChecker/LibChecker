package com.absinthe.libchecker.data.app

import android.content.pm.PackageInfo
import com.absinthe.libchecker.domain.app.InstalledAppRepository
import com.absinthe.libchecker.domain.app.PackageChangeState
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.isPreinstalled
import kotlinx.coroutines.flow.SharedFlow

object LocalInstalledAppRepository : InstalledAppRepository {

  override val packageChanges: SharedFlow<PackageChangeState> = LocalAppDataSource.packageChangeFlow

  override fun getApplicationList(forceUpdate: Boolean): List<PackageInfo> {
    return LocalAppDataSource.getApplicationList(forceUpdate)
  }

  override fun getApplicationMap(forceUpdate: Boolean): Map<String, PackageInfo> {
    return LocalAppDataSource.getApplicationMap(forceUpdate)
  }

  override fun getApplicationCount(forceUpdate: Boolean): Int {
    return LocalAppDataSource.getApplicationCount(forceUpdate)
  }

  override fun getPackageInfo(packageName: String, flags: Int): PackageInfo? {
    return runCatching { PackageUtils.getPackageInfo(packageName, flags) }.getOrNull()
  }

  override fun isPackageInstalled(packageName: String): Boolean {
    return PackageUtils.isAppInstalled(packageName)
  }

  override fun isPackagePreinstalled(packageName: String): Boolean {
    return getPackageInfo(packageName)?.isPreinstalled() == true
  }
}
