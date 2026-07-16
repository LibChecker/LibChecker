package com.absinthe.libchecker.data.app

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import androidx.lifecycle.LifecycleOwner
import com.absinthe.libchecker.domain.app.list.model.InstalledPackageState
import com.absinthe.libchecker.domain.app.model.AppInstallSource
import com.absinthe.libchecker.domain.app.model.PackageChangeState
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import com.absinthe.libchecker.utils.FreezeUtils
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.isPreinstalled
import kotlinx.coroutines.flow.SharedFlow
import timber.log.Timber

class LocalInstalledAppRepository(
  private val appDataSource: AppDataSource,
  private val packageChangeObserver: LocalPackageChangeObserver
) : InstalledAppRepository {

  override val packageChanges: SharedFlow<PackageChangeState> = packageChangeObserver.packageChanges

  override fun getApplicationList(forceUpdate: Boolean): List<PackageInfo> {
    return appDataSource.getApplicationList(forceUpdate)
  }

  override fun getApplicationMap(forceUpdate: Boolean): Map<String, PackageInfo> {
    return appDataSource.getApplicationMap(forceUpdate)
  }

  override fun getApplicationCount(forceUpdate: Boolean): Int {
    return appDataSource.getApplicationCount(forceUpdate)
  }

  override fun getRandomApplicationInfo(forceUpdate: Boolean): ApplicationInfo? {
    return appDataSource.getRandomApplicationInfo(forceUpdate)
  }

  override fun getApexPackageNames(): Set<String> {
    return appDataSource.getApexPackageNames()
  }

  override fun startPackageChangeMonitoring(owner: LifecycleOwner) {
    packageChangeObserver.start(owner, appDataSource::updateApplications)
  }

  override fun stopPackageChangeMonitoring(owner: LifecycleOwner) {
    packageChangeObserver.stop(owner)
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
