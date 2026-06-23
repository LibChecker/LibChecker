package com.absinthe.libchecker.domain.app

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.SharedFlow

interface InstalledAppRepository {

  val packageChanges: SharedFlow<PackageChangeState>

  fun getApplicationList(forceUpdate: Boolean = false): List<PackageInfo>

  fun getApplicationMap(forceUpdate: Boolean = false): Map<String, PackageInfo>

  fun getApplicationCount(forceUpdate: Boolean = false): Int

  fun getRandomApplicationInfo(forceUpdate: Boolean = false): ApplicationInfo?

  fun getApexPackageNames(): Set<String>

  fun startPackageChangeMonitoring(owner: LifecycleOwner)

  fun stopPackageChangeMonitoring(owner: LifecycleOwner)

  fun getPackageInfo(
    packageName: String,
    flags: Int = 0,
    resolveFrozenArchiveInfo: Boolean = true
  ): PackageInfo?

  fun isPackageInstalled(packageName: String): Boolean

  fun isPackagePreinstalled(packageName: String): Boolean

  fun getInstallSource(packageName: String): AppInstallSource?

  fun getPermissions(packageName: String): List<String>

  fun getPackageState(packageName: String): InstalledPackageState
}
