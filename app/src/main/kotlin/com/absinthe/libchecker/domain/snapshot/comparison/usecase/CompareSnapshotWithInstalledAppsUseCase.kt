package com.absinthe.libchecker.domain.snapshot.comparison.usecase

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.absinthe.libchecker.database.entity.SnapshotDiffStoringItem
import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import com.absinthe.libchecker.domain.snapshot.SnapshotItemFactory
import com.absinthe.libchecker.domain.snapshot.SnapshotRepository
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.utils.extensions.getPackageSize
import com.absinthe.libchecker.utils.extensions.getVersionCode
import com.absinthe.libchecker.utils.extensions.isArchivedPackage
import com.absinthe.libchecker.utils.fromJson
import com.absinthe.libchecker.utils.toJson
import java.io.IOException
import kotlinx.coroutines.CancellationException
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

    val storedDiffMap = try {
      snapshotRepository.getSnapshotDiffs().associateBy { it.packageName }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Timber.w(e, "Failed to read snapshot diff cache")
      emptyMap()
    }
    val pendingDiffStoreItems = mutableListOf<SnapshotDiffStoringItem>()

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
          storedDiffItem = storedDiffMap[snapshotItem.packageName],
          diffList = diffList,
          pendingDiffStoreItems = pendingDiffStoreItems
        )
      } catch (e: Exception) {
        Timber.e(e)
      } finally {
        progressCount++
        updateProgress()
      }
    }

    if (pendingDiffStoreItems.isNotEmpty()) {
      try {
        snapshotRepository.insertSnapshotDiffs(pendingDiffStoreItems)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Timber.w(e, "Failed to write snapshot diff cache")
      }
    }

    return diffList
  }

  private fun addChangedSnapshotDiff(
    packageManager: PackageManager,
    snapshotItem: SnapshotItem,
    presentItem: PackageInfo,
    trackPackageNames: Set<String>,
    storedDiffItem: SnapshotDiffStoringItem?,
    diffList: MutableList<SnapshotDiffItem>,
    pendingDiffStoreItems: MutableList<SnapshotDiffStoringItem>
  ) {
    val presentIsArchived = presentItem.isArchivedPackage()

    if (storedDiffItem?.lastUpdatedTime != presentItem.lastUpdateTime ||
      storedDiffItem.isArchived != presentIsArchived ||
      snapshotItem.isArchived != presentIsArchived
    ) {
      createDiffItem(packageManager, snapshotItem, presentItem, trackPackageNames)?.let { item ->
        diffList.add(item)
        pendingDiffStoreItems.add(
          SnapshotDiffStoringItem(
            packageName = presentItem.packageName,
            lastUpdatedTime = presentItem.lastUpdateTime,
            isArchived = presentIsArchived,
            diffContent = item.toJson().orEmpty()
          )
        )
      }
      return
    }

    try {
      storedDiffItem.diffContent.fromJson<SnapshotDiffItem>()?.let { item ->
        diffList.add(item)
      }
    } catch (e: IOException) {
      Timber.e(e, "diffContent parsing failed")

      createDiffItem(packageManager, snapshotItem, presentItem, trackPackageNames)?.let { item ->
        diffList.add(item)
        pendingDiffStoreItems.add(
          SnapshotDiffStoringItem(
            packageName = presentItem.packageName,
            lastUpdatedTime = presentItem.lastUpdateTime,
            isArchived = presentIsArchived,
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
      packageInfo.isArchivedPackage() == snapshotItem.isArchived &&
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
