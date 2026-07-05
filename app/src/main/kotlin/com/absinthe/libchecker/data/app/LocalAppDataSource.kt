package com.absinthe.libchecker.data.app

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.absinthe.libchecker.app.SystemServices
import com.absinthe.libchecker.compat.PackageManagerCompat
import com.absinthe.libchecker.domain.app.model.PackageChangeState
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.isArchivedPackage
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import timber.log.Timber

class LocalAppDataSource : AppDataSource {

  private val applicationsLock = ReentrantReadWriteLock()
  private val applicationMap: MutableMap<String, PackageInfo> = linkedMapOf()
  private var applicationListSnapshot: List<PackageInfo> = emptyList()
  private var applicationMapSnapshot: Map<String, PackageInfo> = emptyMap()
  private var applicationsLoaded: Boolean = false
  private val apexPackageSet: Set<String> by lazy { loadApexPackageSet() }

  override fun getApplicationList(forceUpdate: Boolean): List<PackageInfo> {
    ensureApplicationsLoaded(forceUpdate)
    return applicationsLock.read {
      applicationListSnapshot
    }
  }

  override fun getApplicationMap(forceUpdate: Boolean): Map<String, PackageInfo> {
    ensureApplicationsLoaded(forceUpdate)
    return applicationsLock.read {
      applicationMapSnapshot
    }
  }

  override fun getApplicationCount(forceUpdate: Boolean): Int {
    ensureApplicationsLoaded(forceUpdate)
    return applicationsLock.read {
      applicationMap.size
    }
  }

  override fun getRandomApplicationInfo(forceUpdate: Boolean): ApplicationInfo? {
    ensureApplicationsLoaded(forceUpdate)
    return applicationsLock.read {
      applicationListSnapshot.randomOrNull()?.applicationInfo
    }
  }

  override fun getApexPackageNames(): Set<String> {
    return apexPackageSet
  }

  private fun ensureApplicationsLoaded(forceUpdate: Boolean = false) {
    if (!forceUpdate && applicationsLock.read { applicationsLoaded }) {
      return
    }
    applicationsLock.write {
      if (forceUpdate || !applicationsLoaded) {
        refreshApplicationsLocked()
      }
    }
  }

  private fun refreshApplicationsLocked() {
    applicationMap.clear()
    loadApplications()
      .asSequence()
      .filter { it.isVisiblePackageInfo() }
      .forEach { applicationMap[it.packageName] = it }
    updateSnapshotsLocked()
    applicationsLoaded = true
  }

  private fun loadApplications(): List<PackageInfo> {
    Timber.d("loadApplications start")
    val list = PackageManagerCompat.getInstalledPackages(getInstalledPackageFlags())
    Timber.d("loadApplications end, apps count: ${list.size}")
    return list
  }

  private fun getInstalledPackageFlags(): Long {
    return if (OsUtils.atLeastV()) PackageManager.MATCH_ARCHIVED_PACKAGES else 0L
  }

  private fun loadPackageInfo(packageName: String): PackageInfo? {
    return runCatching { PackageUtils.getPackageInfo(packageName) }.getOrNull()
  }

  private fun PackageInfo.isVisiblePackageInfo(): Boolean {
    return applicationInfo?.sourceDir != null || applicationInfo?.publicSourceDir != null || isArchivedPackage()
  }

  /**
   * Load apex package set
   * PackageInfo#isApex is always false
   * use this method to workaround
   */
  private fun loadApexPackageSet(): Set<String> {
    if (OsUtils.atLeastQ()) {
      Timber.d("loadApexPackageSet start")
      return runCatching {
        SystemServices.packageManager.getInstalledModules(PackageManager.MATCH_ALL)
          .map { it.packageName.orEmpty() }
          .toSet()
      }.onFailure {
        Timber.e(it)
      }.onSuccess {
        Timber.d("loadApexPackageSet end, apex count: ${it.size}")
      }.getOrDefault(emptySet())
    }
    return emptySet()
  }

  override fun updateApplications(state: PackageChangeState) {
    applicationsLock.write {
      if (!applicationsLoaded) {
        refreshApplicationsLocked()
      }
      val packageName = state.packageName
      when (state) {
        is PackageChangeState.Added -> {
          val packageInfo = loadPackageInfo(packageName)
          if (packageInfo?.isVisiblePackageInfo() == true) {
            applicationMap[packageName] = packageInfo
          } else {
            applicationMap.remove(packageName)
          }
        }

        is PackageChangeState.Removed -> {
          applicationMap.remove(packageName)
        }

        is PackageChangeState.Replaced -> {
          val packageInfo = loadPackageInfo(packageName)
          if (packageInfo?.isVisiblePackageInfo() == true) {
            applicationMap[packageName] = packageInfo
          } else {
            applicationMap.remove(packageName)
          }
        }
      }
      updateSnapshotsLocked()
    }
  }

  private fun updateSnapshotsLocked() {
    applicationListSnapshot = applicationMap.values.toList()
    applicationMapSnapshot = applicationMap.toMap()
  }
}
