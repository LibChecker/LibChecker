package com.absinthe.libchecker.domain.snapshot.list.capture

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.domain.app.InstalledAppRepository
import com.absinthe.libchecker.domain.snapshot.SnapshotRepository
import com.absinthe.libchecker.domain.snapshot.SnapshotSelectionUseCase
import com.absinthe.libchecker.domain.snapshot.SnapshotSettingsRepository
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.getAppName
import com.absinthe.libchecker.utils.extensions.getCompileSdkVersion
import com.absinthe.libchecker.utils.extensions.getPackageSize
import com.absinthe.libchecker.utils.extensions.getPermissionsList
import com.absinthe.libchecker.utils.extensions.getVersionCode
import com.absinthe.libchecker.utils.toJson

class CaptureInstalledSnapshotUseCase(
  private val packageManager: PackageManager,
  private val snapshotRepository: SnapshotRepository,
  private val installedAppRepository: InstalledAppRepository,
  private val snapshotSelectionUseCase: SnapshotSelectionUseCase,
  private val snapshotSettingsRepository: SnapshotSettingsRepository,
  private val snapshotCaptureStateRepository: SnapshotCaptureStateRepository
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
    val shouldSaveFullSnapshot = snapshotCaptureStateRepository.shouldSaveFullSnapshot()
    var count = 0
    var lastProgress = 0

    for (packageInfo in request.appList) {
      if (packageInfo.applicationInfo == null) {
        continue
      }

      val previousSnapshotItem = snapshotRepository.getSnapshot(currentSnapshotTimestamp, packageInfo.packageName)
      val snapshotItem = previousSnapshotItem
        ?.takeIf { it.isReusableFor(packageInfo, shouldSaveFullSnapshot) }
        ?.copy()
        ?.also {
          it.id = null
          it.timeStamp = timestamp
        }
        ?: buildInstalledSnapshotItem(packageInfo, timestamp)
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

    if (shouldSaveFullSnapshot) {
      snapshotCaptureStateRepository.markFullSnapshotSaved()
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

  private fun buildInstalledSnapshotItem(
    packageInfo: PackageInfo,
    timestamp: Long
  ): SnapshotItem? {
    val applicationInfo = packageInfo.applicationInfo ?: return null
    val activitiesPi = installedAppRepository.getPackageInfo(
      packageInfo.packageName,
      PackageManager.GET_ACTIVITIES
    ) ?: return null
    val othersPi = installedAppRepository.getPackageInfo(
      packageInfo.packageName,
      PackageManager.GET_SERVICES or PackageManager.GET_RECEIVERS or
        PackageManager.GET_PROVIDERS or PackageManager.GET_PERMISSIONS or
        PackageManager.GET_META_DATA
    ) ?: return null
    val abi = PackageUtils.getAbi(packageInfo)
    if (abi == Constants.ERROR) {
      return null
    }

    return SnapshotItem(
      id = null,
      packageName = packageInfo.packageName,
      timeStamp = timestamp,
      label = packageInfo.getAppName(packageManager).toString(),
      versionName = packageInfo.versionName.toString(),
      versionCode = packageInfo.getVersionCode(),
      installedTime = packageInfo.firstInstallTime,
      lastUpdatedTime = packageInfo.lastUpdateTime,
      isSystem = (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) > 0,
      abi = abi.toShort(),
      targetApi = applicationInfo.targetSdkVersion.toShort(),
      nativeLibs = PackageUtils.getNativeDirLibs(packageInfo).toJson().orEmpty(),
      services = PackageUtils.getComponentStringList(othersPi, SERVICE, false)
        .toJson().orEmpty(),
      activities = PackageUtils.getComponentStringList(activitiesPi, ACTIVITY, false)
        .toJson().orEmpty(),
      receivers = PackageUtils.getComponentStringList(othersPi, RECEIVER, false)
        .toJson().orEmpty(),
      providers = PackageUtils.getComponentStringList(othersPi, PROVIDER, false)
        .toJson().orEmpty(),
      permissions = othersPi.getPermissionsList().toJson().orEmpty(),
      metadata = PackageUtils.getMetaDataItems(othersPi).toJson().orEmpty(),
      packageSize = packageInfo.getPackageSize(true),
      compileSdk = packageInfo.getCompileSdkVersion().toShort(),
      minSdk = applicationInfo.minSdkVersion.toShort()
    )
  }

  data class Request(
    val appList: List<PackageInfo>,
    val dropPrevious: Boolean,
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
