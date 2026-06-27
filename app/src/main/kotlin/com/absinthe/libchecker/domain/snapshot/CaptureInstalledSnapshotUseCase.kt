package com.absinthe.libchecker.domain.snapshot

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.utils.extensions.getPackageSize
import com.absinthe.libchecker.utils.extensions.getVersionCode
import com.absinthe.libchecker.utils.toJson

class CaptureInstalledSnapshotUseCase(
  private val packageManager: PackageManager,
  private val snapshotRepository: SnapshotRepository,
  private val buildInstalledSnapshotItem: BuildInstalledSnapshotItemUseCase,
  private val snapshotSelectionUseCase: SnapshotSelectionUseCase,
  private val snapshotSettingsRepository: SnapshotSettingsRepository
) {

  suspend operator fun invoke(
    request: Request,
    onProgress: suspend (Progress) -> Unit
  ): Result {
    val timestamp = request.timestamp
    snapshotRepository.deleteAllSnapshotDiffItems()

    val total = request.appList.size
    val snapshotItems = mutableListOf<SnapshotItem>()
    val currentSnapshotTimestamp = snapshotSelectionUseCase.getCurrentTimestamp()
    var count = 0
    var lastProgress = 0

    for (packageInfo in request.appList) {
      if (packageInfo.applicationInfo == null) {
        continue
      }

      val previousSnapshotItem = snapshotRepository.getSnapshot(currentSnapshotTimestamp, packageInfo.packageName)
      val snapshotItem = previousSnapshotItem
        ?.takeIf { it.isReusableFor(packageInfo, request.shouldSaveFullSnapshot) }
        ?.copy()
        ?.also {
          it.id = null
          it.timeStamp = timestamp
        }
        ?: buildInstalledSnapshotItem(packageManager, packageInfo, timestamp)
      snapshotItem?.let(snapshotItems::add)

      count++
      val currentProgress = if (total == 0) 0 else count * 100 / total
      if (currentProgress > lastProgress) {
        lastProgress = currentProgress
        onProgress(Progress(count, total, currentProgress))
      }

      if (snapshotItems.size >= BATCH_SIZE) {
        snapshotRepository.insertSnapshots(snapshotItems)
        snapshotItems.clear()
      }
    }

    snapshotRepository.insertSnapshots(snapshotItems)
    snapshotRepository.insertTimeStamp(TimeStampItem(timestamp, null, request.systemProps.toJson()))

    if (request.dropPrevious) {
      snapshotRepository.deleteSnapshotsAndTimeStamp(currentSnapshotTimestamp)
    }

    val autoRemoveThreshold = snapshotSettingsRepository.autoRemoveThreshold
    if (autoRemoveThreshold > 0) {
      snapshotRepository.retainLatestSnapshots(autoRemoveThreshold)
    }

    snapshotSelectionUseCase.setCurrentTimestamp(timestamp)
    return Result(
      timestamp = timestamp,
      processedCount = count,
      total = total
    )
  }

  private fun SnapshotItem.isReusableFor(
    packageInfo: PackageInfo,
    shouldSaveFullSnapshot: Boolean
  ): Boolean {
    return versionCode == packageInfo.getVersionCode() &&
      lastUpdatedTime == packageInfo.lastUpdateTime &&
      packageSize == packageInfo.getPackageSize(true) &&
      !shouldSaveFullSnapshot
  }

  data class Request(
    val appList: List<PackageInfo>,
    val dropPrevious: Boolean,
    val shouldSaveFullSnapshot: Boolean,
    val systemProps: Map<String, String>,
    val timestamp: Long = System.currentTimeMillis()
  )

  data class Progress(
    val count: Int,
    val total: Int,
    val percent: Int
  )

  data class Result(
    val timestamp: Long,
    val processedCount: Int,
    val total: Int
  )

  private companion object {
    const val BATCH_SIZE = 50
  }
}
