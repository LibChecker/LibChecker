package com.absinthe.libchecker.data.snapshot

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.domain.snapshot.SnapshotItemFactory
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.getAppName
import com.absinthe.libchecker.utils.extensions.getCompileSdkVersion
import com.absinthe.libchecker.utils.extensions.getPackageSize
import com.absinthe.libchecker.utils.extensions.getPermissionsList
import com.absinthe.libchecker.utils.extensions.getVersionCode
import com.absinthe.libchecker.utils.toJson

class AndroidSnapshotItemFactory : SnapshotItemFactory {

  override fun create(packageManager: PackageManager, packageInfo: PackageInfo): SnapshotItem {
    val flaggedPi = PackageUtils.getPackageInfo(
      packageInfo.packageName,
      PackageManager.GET_SERVICES or PackageManager.GET_RECEIVERS or PackageManager.GET_PROVIDERS or
        PackageManager.GET_PERMISSIONS or PackageManager.GET_META_DATA
    )

    return SnapshotItem(
      id = null,
      packageName = packageInfo.packageName,
      timeStamp = 0,
      installedTime = packageInfo.firstInstallTime,
      lastUpdatedTime = packageInfo.lastUpdateTime,
      label = packageInfo.getAppName(packageManager).toString(),
      versionName = packageInfo.versionName.toString(),
      versionCode = packageInfo.getVersionCode(),
      abi = PackageUtils.getAbi(packageInfo).toShort(),
      targetApi = packageInfo.applicationInfo?.targetSdkVersion?.toShort() ?: 0,
      compileSdk = packageInfo.getCompileSdkVersion().toShort(),
      minSdk = packageInfo.applicationInfo?.minSdkVersion?.toShort() ?: 0,
      nativeLibs = PackageUtils.getNativeDirLibs(packageInfo).toJson().orEmpty(),
      services = PackageUtils.getComponentStringList(
        flaggedPi,
        SERVICE,
        false
      ).toJson().orEmpty(),
      activities = PackageUtils.getComponentStringList(
        packageInfo.packageName,
        ACTIVITY,
        false
      ).toJson().orEmpty(),
      receivers = PackageUtils.getComponentStringList(
        flaggedPi,
        RECEIVER,
        false
      ).toJson().orEmpty(),
      providers = PackageUtils.getComponentStringList(
        flaggedPi,
        PROVIDER,
        false
      ).toJson().orEmpty(),
      permissions = flaggedPi.getPermissionsList().toJson().orEmpty(),
      metadata = PackageUtils.getMetaDataItems(flaggedPi).toJson().orEmpty(),
      packageSize = packageInfo.getPackageSize(true),
      isSystem = (packageInfo.applicationInfo!!.flags and ApplicationInfo.FLAG_SYSTEM) > 0
    )
  }
}
