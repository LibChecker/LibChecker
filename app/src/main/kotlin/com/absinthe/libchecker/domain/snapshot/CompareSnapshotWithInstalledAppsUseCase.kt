package com.absinthe.libchecker.domain.snapshot

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.absinthe.libchecker.database.entity.SnapshotDiffStoringItem
import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.domain.app.InstalledAppRepository
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.utils.extensions.getPackageSize
import com.absinthe.libchecker.utils.extensions.getVersionCode
import com.absinthe.libchecker.utils.fromJson
import com.absinthe.libchecker.utils.toJson
import java.io.IOException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import timber.log.Timber

class CompareSnapshotWithInstalledAppsUseCase(
  private val packageManager: PackageManager,
  private val snapshotRepository: SnapshotRepository,
  private val installedAppRepository: InstalledAppRepository,
  private val snapshotItemFactory: SnapshotItemFactory,
  private val compareSnapshotItems: CompareSnapshotItemsUseCase
) {

  suspend operator fun invoke(
    timestamp: Long,
    onProgress: (Int) -> Unit
  ): List<SnapshotDiffItem>? {
    val previousMap = snapshotRepository.getSnapshots(timestamp).associateBy { it.packageName }

    if (previousMap.isEmpty() || timestamp == 0L) {
      return emptyList()
    }

    val currentMap = installedAppRepository.getApplicationMap(true)
    val trackPackageNames = snapshotRepository.getTrackItems()
      .asSequence()
      .map { it.packageName }
      .toSet()
    val progressTotal = currentMap.size.coerceAtLeast(1)
    val diffList = mutableListOf<SnapshotDiffItem>()
    var progressCount = 0

    fun updateProgress() {
      onProgress(progressCount * 100 / progressTotal)
    }

    for ((packageName, snapshotItem) in previousMap) {
      if (!currentCoroutineContext().isActive) {
        return null
      }
      if (packageName !in currentMap) {
        diffList.add(compareSnapshotItems(snapshotItem, null, trackPackageNames)!!)
      }
    }

    for ((packageName, packageInfo) in currentMap) {
      if (!currentCoroutineContext().isActive) {
        return null
      }
      if (packageName in previousMap) {
        continue
      }

      try {
        val newInfo = snapshotItemFactory.create(packageManager, packageInfo)
        diffList.add(compareSnapshotItems(null, newInfo, trackPackageNames)!!)
      } catch (e: Exception) {
        Timber.e(e)
      } finally {
        progressCount++
        updateProgress()
      }
    }

    for ((_, snapshotItem) in previousMap) {
      if (!currentCoroutineContext().isActive) {
        return null
      }
      val presentItem = currentMap[snapshotItem.packageName] ?: continue
      try {
        addChangedSnapshotDiff(
          packageManager = packageManager,
          snapshotItem = snapshotItem,
          presentItem = presentItem,
          trackPackageNames = trackPackageNames,
          diffList = diffList
        )
      } catch (e: Exception) {
        Timber.e(e)
      } finally {
        progressCount++
        updateProgress()
      }
    }

    return diffList
  }

  private suspend fun addChangedSnapshotDiff(
    packageManager: PackageManager,
    snapshotItem: SnapshotItem,
    presentItem: PackageInfo,
    trackPackageNames: Set<String>,
    diffList: MutableList<SnapshotDiffItem>
  ) {
    val snapshotDiffStoringItem = snapshotRepository.getSnapshotDiff(snapshotItem.packageName)

    if (snapshotDiffStoringItem?.lastUpdatedTime != presentItem.lastUpdateTime) {
      createDiffItem(packageManager, snapshotItem, presentItem, trackPackageNames)?.let { item ->
        diffList.add(item)
        snapshotRepository.insertSnapshotDiff(
          SnapshotDiffStoringItem(
            packageName = presentItem.packageName,
            lastUpdatedTime = presentItem.lastUpdateTime,
            diffContent = item.toJson().orEmpty()
          )
        )
      }
      return
    }

    try {
      snapshotDiffStoringItem.diffContent.fromJson<SnapshotDiffItem>()?.let { item ->
        diffList.add(item)
      }
    } catch (e: IOException) {
      Timber.e(e, "diffContent parsing failed")

      createDiffItem(packageManager, snapshotItem, presentItem, trackPackageNames)?.let { item ->
        diffList.add(item)
        snapshotRepository.insertSnapshotDiff(
          SnapshotDiffStoringItem(
            packageName = presentItem.packageName,
            lastUpdatedTime = presentItem.lastUpdateTime,
            diffContent = item.toJson().orEmpty()
          )
        )
      }
    }
  }

  private fun createDiffItem(
    packageManager: PackageManager,
    snapshotItem: SnapshotItem,
    packageInfo: PackageInfo,
    trackPackageNames: Set<String>
  ): SnapshotDiffItem? {
    if (packageInfo.getVersionCode() == snapshotItem.versionCode &&
      packageInfo.lastUpdateTime == snapshotItem.lastUpdatedTime &&
      packageInfo.getPackageSize(true) == snapshotItem.packageSize &&
      snapshotItem.packageName !in trackPackageNames
    ) {
      return null
    }
    return compareSnapshotItems(
      snapshotItem,
      snapshotItemFactory.create(packageManager, packageInfo),
      trackPackageNames
    )
  }
}
