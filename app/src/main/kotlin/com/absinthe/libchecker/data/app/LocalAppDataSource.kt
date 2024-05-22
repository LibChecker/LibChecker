package com.absinthe.libchecker.data.app

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.absinthe.libchecker.app.SystemServices
import com.absinthe.libchecker.compat.PackageManagerCompat
import com.absinthe.libchecker.utils.OsUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber

object LocalAppDataSource : AppDataSource {

  var apexPackageSet: Set<String> = emptySet()
    private set

  override fun getApplicationList(ioDispatcher: CoroutineDispatcher): Flow<List<PackageInfo>> =
    flow {
      val list = getApplicationList()
      val listByShell = getAppListByShell()

      if (listByShell.size > list.size) {
        Timber.d("listByShell.size > list.size")
        emit(
          listByShell.mapNotNull { packageName ->
            runCatching {
              PackageManagerCompat.getPackageInfo(
                packageName,
                PackageManager.GET_META_DATA or PackageManager.GET_PERMISSIONS
              )
            }.getOrNull()
          }
        )
      } else {
        emit(list)
      }
    }.flowOn(ioDispatcher)

  override fun getApplicationList(): List<PackageInfo> {
    Timber.d("getApplicationList start")
    val list =
      PackageManagerCompat.getInstalledPackages(0)
    Timber.d("getApplicationList end, apps count: ${list.size}")

    loadApexPackageSet()
    return list
  }

  override fun getApplicationMap(ioDispatcher: CoroutineDispatcher): Flow<Map<String, PackageInfo>> =
    flow {
      getApplicationList(ioDispatcher).collect { list ->
        val map = list.asSequence()
          .filter { it.applicationInfo.sourceDir != null || it.applicationInfo.publicSourceDir != null }
          .map { it.packageName to it }
          .toMap()
        emit(map)
      }
    }.flowOn(ioDispatcher)

  override fun getApplicationMap(): Map<String, PackageInfo> {
    return getApplicationList().asSequence()
      .filter { it.applicationInfo.sourceDir != null || it.applicationInfo.publicSourceDir != null }
      .map { it.packageName to it }
      .toMap()
  }

  private val runtime by lazy { Runtime.getRuntime() }

  private fun getAppListByShell(): List<String> {
    try {
      val process = runtime.exec("pm list packages")
      val packages = process.inputStream.reader().useLines { lines ->
        lines.mapNotNull { line ->
          if (line.startsWith("package:")) {
            line.removePrefix("package:").trim().takeIf { it.isNotEmpty() }
          } else {
            null
          }
        }.toList()
      }

      return packages
    } catch (t: Throwable) {
      Timber.w(t)
      return emptyList()
    }
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
