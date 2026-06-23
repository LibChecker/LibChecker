package com.absinthe.libchecker.domain.snapshot

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.domain.app.InstalledAppRepository
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.getAppName
import com.absinthe.libchecker.utils.extensions.getCompileSdkVersion
import com.absinthe.libchecker.utils.extensions.getPackageSize
import com.absinthe.libchecker.utils.extensions.getPermissionsList
import com.absinthe.libchecker.utils.extensions.getVersionCode
import com.absinthe.libchecker.utils.toJson

class BuildInstalledSnapshotItemUseCase(
  private val installedAppRepository: InstalledAppRepository
) {

  operator fun invoke(
    packageManager: PackageManager,
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
}
