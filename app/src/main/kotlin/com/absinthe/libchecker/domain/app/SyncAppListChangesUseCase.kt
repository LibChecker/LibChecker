package com.absinthe.libchecker.domain.app

import android.content.pm.PackageInfo
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.utils.extensions.getVersionCode
import com.absinthe.libraries.utils.manager.TimeRecorder
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import timber.log.Timber

class SyncAppListChangesUseCase(
  private val installedAppRepository: InstalledAppRepository,
  private val appListRepository: AppListRepository,
  private val appListItemFactory: AppListItemFactory
) {

  suspend operator fun invoke(
    request: Request,
    currentItems: List<LCItem>
  ): Result {
    Timber.d("Request change: START")
    val timeRecorder = TimeRecorder()
    timeRecorder.start()

    return try {
      val isSynced = when (request) {
        is Request.ApplyPackageChanges -> syncChangedPackages(request.changes)
        Request.RefreshAll -> refreshAll(currentItems)
      }
      if (isSynced) Result.Synced else Result.Canceled
    } finally {
      timeRecorder.end()
      Timber.d("Request change: END, $timeRecorder")
    }
  }

  private suspend fun syncChangedPackages(changes: List<PackageChangeState>): Boolean {
    for (currentState in changes.latestByPackage()) {
      if (!currentCoroutineContext().isActive) {
        return false
      }

      when (currentState) {
        is PackageChangeState.Added -> {
          val packageInfo = installedAppRepository.getPackageInfo(currentState.packageName) ?: continue
          runCatching {
            appListRepository.insertItem(appListItemFactory.create(packageInfo, true))
          }.onFailure { e ->
            Timber.e(e, "requestChange: ${currentState.packageName}")
          }
        }

        is PackageChangeState.Removed -> {
          appListRepository.deleteItemByPackageName(currentState.packageName)
        }

        is PackageChangeState.Replaced -> {
          val packageInfo = installedAppRepository.getPackageInfo(currentState.packageName) ?: continue
          runCatching {
            appListRepository.updateItem(appListItemFactory.create(packageInfo, true))
          }.onFailure { e ->
            Timber.e(e, "requestChange: ${currentState.packageName}")
          }
        }
      }
    }
    return true
  }

  private suspend fun refreshAll(currentItems: List<LCItem>): Boolean {
    val dbItemMap = currentItems.associateBy { it.packageName }
    var applications = installedAppRepository.getApplicationMap(true)

    /*
     * The application list returned with a probability only contains system applications.
     * When the difference is greater than a certain threshold, we re-request the list.
     */
    if (hasLargeApplicationDiff(applications, dbItemMap)) {
      Timber.w("Request change canceled because of large diff, re-request appMap")
      applications = installedAppRepository.getApplicationMap(true)
    }

    for (packageInfo in applications.values) {
      if (packageInfo.packageName in dbItemMap) continue
      if (!currentCoroutineContext().isActive) return false

      runCatching {
        appListRepository.insertItem(appListItemFactory.create(packageInfo, true))
      }.onFailure { e ->
        Timber.e(e, "requestChange: ${packageInfo.packageName}")
      }
    }

    for (packageName in dbItemMap.keys) {
      if (packageName in applications) continue
      if (!currentCoroutineContext().isActive) return false

      appListRepository.deleteItemByPackageName(packageName)
    }

    for (packageInfo in applications.values) {
      val dbItem = dbItemMap[packageInfo.packageName] ?: continue
      if (!isItemOutdated(packageInfo, dbItem)) continue
      if (!currentCoroutineContext().isActive) return false

      runCatching {
        appListRepository.updateItem(appListItemFactory.create(packageInfo, true))
      }.onFailure { e ->
        Timber.e(e, "requestChange: ${packageInfo.packageName}")
      }
    }

    return true
  }

  private fun hasLargeApplicationDiff(
    applications: Map<String, PackageInfo>,
    dbItemMap: Map<String, LCItem>
  ): Boolean {
    return applications.keys.count { it !in dbItemMap } > LARGE_DIFF_THRESHOLD ||
      dbItemMap.keys.count { it !in applications } > LARGE_DIFF_THRESHOLD
  }

  private fun isItemOutdated(packageInfo: PackageInfo, dbItem: LCItem): Boolean {
    return dbItem.versionCode != packageInfo.getVersionCode() ||
      packageInfo.lastUpdateTime != dbItem.lastUpdatedTime ||
      dbItem.lastUpdatedTime == 0L
  }

  private fun List<PackageChangeState>.latestByPackage(): List<PackageChangeState> {
    if (size < 2) {
      return this
    }
    return asReversed().distinctBy { it.packageName }.asReversed()
  }

  sealed class Request {
    data class ApplyPackageChanges(val changes: List<PackageChangeState>) : Request()
    data object RefreshAll : Request()
  }

  enum class Result {
    Synced,
    Canceled
  }

  private companion object {
    const val LARGE_DIFF_THRESHOLD = 30
  }
}
