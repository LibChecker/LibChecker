package com.absinthe.libchecker.data.app

import android.content.pm.PackageInfo
import com.absinthe.libchecker.domain.app.AppInstallSource
import com.absinthe.libchecker.domain.app.InstalledAppRepository
import com.absinthe.libchecker.domain.app.InstalledPackageState
import com.absinthe.libchecker.domain.app.PackageChangeState
import com.absinthe.libchecker.utils.FreezeUtils
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.isPreinstalled
import kotlinx.coroutines.flow.SharedFlow
import timber.log.Timber

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

  override fun getApexPackageNames(): Set<String> {
    return LocalAppDataSource.apexPackageSet
  }

  override fun getPackageInfo(
    packageName: String,
    flags: Int,
    resolveFrozenArchiveInfo: Boolean
  ): PackageInfo? {
    return runCatching {
      PackageUtils.getPackageInfo(packageName, flags, resolveFrozenArchiveInfo)
    }.getOrNull()
  }

  override fun isPackageInstalled(packageName: String): Boolean {
    return PackageUtils.isAppInstalled(packageName)
  }

  override fun isPackagePreinstalled(packageName: String): Boolean {
    return getPackageInfo(packageName)?.isPreinstalled() == true
  }

  override fun getInstallSource(packageName: String): AppInstallSource? {
    if (!OsUtils.atLeastR()) {
      return null
    }
    return runCatching {
      PackageUtils.getInstallSourceInfo(packageName)?.let {
        AppInstallSource(
          initiatingPackageName = it.initiatingPackageName,
          originatingPackageName = it.originatingPackageName,
          installingPackageName = it.installingPackageName
        )
      }
    }.onFailure {
      Timber.e(it)
    }.getOrNull()
  }

  override fun getPermissions(packageName: String): List<String> {
    return PackageUtils.getPermissionsList(packageName)
  }

  override fun getPackageState(packageName: String): InstalledPackageState {
    val packageInfo = getPackageInfo(packageName)
    return InstalledPackageState(
      packageInfo = packageInfo,
      isFrozen = packageInfo?.applicationInfo?.let { FreezeUtils.isAppFrozen(it) } ?: true
    )
  }
}
