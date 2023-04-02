package com.absinthe.libchecker.data.app

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.absinthe.libchecker.compat.PackageManagerCompat
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber

object LocalAppDataSource : AppDataSource {

  private var applicationsMap: ConcurrentMap<String, PackageInfo> = ConcurrentHashMap(100)

  override fun getApplicationList(ioDispatcher: CoroutineDispatcher): Flow<List<PackageInfo>> =
    flow {
      Timber.d("getApplicationList start")
      val list =
        PackageManagerCompat.getInstalledPackages(PackageManager.GET_META_DATA or PackageManager.GET_PERMISSIONS)
      Timber.d("getApplicationList end, apps count: ${list.size}")

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

  override fun getCachedApplicationMap(ioDispatcher: CoroutineDispatcher): Flow<Map<String, PackageInfo>> =
    flow {
      if (applicationsMap.isEmpty()) {
        getApplicationList(ioDispatcher).collect { list ->
          applicationsMap.putAll(
            list.asSequence()
              .filter { it.applicationInfo.sourceDir != null || it.applicationInfo.publicSourceDir != null }
              .map { it.packageName to it }
              .toMap()
          )
        }
      }
      emit(applicationsMap)
    }.flowOn(ioDispatcher)

  override fun getCachedApplicationMap(): Map<String, PackageInfo> {
    return applicationsMap
  }

  override fun clearCache() {
    applicationsMap.clear()
  }

  fun removePackage(packageName: String) {
    applicationsMap.remove(packageName)
  }

  fun addPackage(packageInfo: PackageInfo) {
    applicationsMap[packageInfo.packageName] = packageInfo
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
}
