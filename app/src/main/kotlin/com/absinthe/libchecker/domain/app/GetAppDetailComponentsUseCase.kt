package com.absinthe.libchecker.domain.app

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.features.applist.detail.bean.StatefulComponent
import com.absinthe.libchecker.utils.IntentFilterUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.ParsedIntentFilter
import com.absinthe.libchecker.utils.apk.ApkPreviewInfo

class GetAppDetailComponentsUseCase(
  private val installedAppRepository: InstalledAppRepository
) {

  operator fun invoke(
    packageInfo: PackageInfo,
    isApk: Boolean
  ): AppDetailComponents {
    val parsedIntentFiltersByClassName = packageInfo.applicationInfo?.sourceDir
      ?.let { sourceDir ->
        IntentFilterUtils.parseComponentsFromApk(sourceDir)
          .asSequence()
          .map { item -> item.className to item.intentFilters }
          .toMap()
      }
      .orEmpty()

    val componentPackageInfo = packageInfo.getCompleteComponentPackageInfo(isApk)

    return AppDetailComponents(
      services = packageInfo.getComponents(
        fallbackPackageInfo = componentPackageInfo,
        isApk = isApk,
        type = SERVICE
      ),
      activities = packageInfo.getComponents(
        fallbackPackageInfo = componentPackageInfo,
        isApk = isApk,
        type = ACTIVITY
      ),
      receivers = packageInfo.getComponents(
        fallbackPackageInfo = componentPackageInfo,
        isApk = isApk,
        type = RECEIVER
      ),
      providers = packageInfo.getComponents(
        fallbackPackageInfo = componentPackageInfo,
        isApk = isApk,
        type = PROVIDER
      ),
      intentFiltersByClassName = parsedIntentFiltersByClassName
    )
  }

  operator fun invoke(previewInfo: ApkPreviewInfo): AppDetailComponents {
    return AppDetailComponents(
      services = PackageUtils.getComponentList(previewInfo.packageName, previewInfo.services, true),
      activities = PackageUtils.getComponentList(previewInfo.packageName, previewInfo.activities, true),
      receivers = PackageUtils.getComponentList(previewInfo.packageName, previewInfo.receivers, true),
      providers = PackageUtils.getComponentList(previewInfo.packageName, previewInfo.providers, true)
    )
  }

  private fun PackageInfo.getCompleteComponentPackageInfo(isApk: Boolean): PackageInfo? {
    if (
      isApk ||
      (
        services?.isNotEmpty() == true &&
          activities?.isNotEmpty() == true &&
          receivers?.isNotEmpty() == true &&
          providers?.isNotEmpty() == true
        )
    ) {
      return this
    }

    return installedAppRepository.getPackageInfo(
      packageName,
      PackageManager.GET_SERVICES or
        PackageManager.GET_ACTIVITIES or
        PackageManager.GET_RECEIVERS or
        PackageManager.GET_PROVIDERS
    )
  }

  private fun PackageInfo.getComponents(
    fallbackPackageInfo: PackageInfo?,
    isApk: Boolean,
    type: Int
  ): List<StatefulComponent> {
    val components = when (type) {
      SERVICE -> services
      ACTIVITY -> activities
      RECEIVER -> receivers
      PROVIDER -> providers
      else -> null
    }

    val fallbackComponents = when (type) {
      SERVICE -> fallbackPackageInfo?.services
      ACTIVITY -> fallbackPackageInfo?.activities
      RECEIVER -> fallbackPackageInfo?.receivers
      PROVIDER -> fallbackPackageInfo?.providers
      else -> null
    }

    val source = if (components?.isNotEmpty() == true || isApk) {
      components
    } else {
      fallbackComponents
    }

    return PackageUtils.getComponentList(packageName, source, true)
  }
}

data class AppDetailComponents(
  val services: List<StatefulComponent>,
  val activities: List<StatefulComponent>,
  val receivers: List<StatefulComponent>,
  val providers: List<StatefulComponent>,
  val intentFiltersByClassName: Map<String, List<ParsedIntentFilter>> = emptyMap()
)
