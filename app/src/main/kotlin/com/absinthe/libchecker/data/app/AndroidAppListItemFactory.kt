package com.absinthe.libchecker.data.app

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.list.AppListItemFactory
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.getAppName
import com.absinthe.libchecker.utils.extensions.getFeatures
import com.absinthe.libchecker.utils.extensions.getVersionCode
import com.absinthe.libchecker.utils.extensions.isArchivedPackage
import com.absinthe.libchecker.utils.harmony.ApplicationDelegate
import com.absinthe.libchecker.utils.harmony.HarmonyOsUtil
import ohos.bundle.IBundleManager

class AndroidAppListItemFactory(context: Context) : AppListItemFactory {

  private val appContext = context.applicationContext
  private val bundleManager by lazy { ApplicationDelegate(appContext).iBundleManager }

  override fun create(packageInfo: PackageInfo, delayInitFeatures: Boolean): LCItem {
    val variant = if (
      HarmonyOsUtil.isHarmonyOs() &&
      bundleManager?.getBundleInfo(packageInfo.packageName, IBundleManager.GET_BUNDLE_DEFAULT) != null
    ) {
      Constants.VARIANT_HAP
    } else {
      Constants.VARIANT_APK
    }

    val applicationInfo =
      packageInfo.applicationInfo ?: throw IllegalArgumentException("ApplicationInfo is null")

    return LCItem(
      packageInfo.packageName,
      packageInfo.getAppName(appContext.packageManager).toString(),
      if (packageInfo.isArchivedPackage()) "Archived" else packageInfo.versionName.toString(),
      packageInfo.getVersionCode(),
      packageInfo.firstInstallTime,
      packageInfo.lastUpdateTime,
      (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) > 0,
      PackageUtils.getAbi(packageInfo).toShort(),
      if (delayInitFeatures) -1 else packageInfo.getFeatures(),
      applicationInfo.targetSdkVersion.toShort(),
      variant
    )
  }
}
