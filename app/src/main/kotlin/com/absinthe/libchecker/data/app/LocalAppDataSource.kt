package com.absinthe.libchecker.data.app

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.absinthe.libchecker.app.SystemServices
import com.absinthe.libchecker.compat.PackageManagerCompat
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.extensions.isArchivedPackage
import timber.log.Timber

object LocalAppDataSource : AppDataSource {

  var apexPackageSet: Set<String> = emptySet()
    private set

  override fun getApplicationList(): List<PackageInfo> {
    Timber.d("getApplicationList start")
    val flag = if (OsUtils.atLeastV()) PackageManager.MATCH_ARCHIVED_PACKAGES else 0
    val list = PackageManagerCompat.getInstalledPackages(flag)
    Timber.d("getApplicationList end, apps count: ${list.size}")

    loadApexPackageSet()
    return list
  }

  override fun getApplicationMap(): Map<String, PackageInfo> {
    return getApplicationList().asSequence()
      .filter { it.applicationInfo?.sourceDir != null || it.applicationInfo?.publicSourceDir != null || it.isArchivedPackage() }
      .map { it.packageName to it }
      .toMap()
  }

  /**
   * Load apex package set
   * PackageInfo#isApex is always false
   * use this method to workaround
   */
  private fun loadApexPackageSet() {
    Timber.d("getApplicationList get apex start")
    if (OsUtils.atLeastQ()) {
      apexPackageSet = SystemServices.packageManager.getInstalledModules(0)
        .map { it.packageName.orEmpty() }
        .toSet()
    }
    Timber.d("getApplicationList get apex end, apex count: ${apexPackageSet.size}")
  }
}
