package com.absinthe.libchecker.domain.app.detail.content

import android.content.pm.ComponentInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.domain.app.detail.model.StatefulComponent
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
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

    return AppDetailComponents(
      services = packageInfo.getComponents(
        isApk = isApk,
        type = SERVICE
      ),
      activities = packageInfo.getComponents(
        isApk = isApk,
        type = ACTIVITY
      ),
      receivers = packageInfo.getComponents(
        isApk = isApk,
        type = RECEIVER
      ),
      providers = packageInfo.getComponents(
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

  private fun PackageInfo.getComponents(
    isApk: Boolean,
    type: Int
  ): List<StatefulComponent> {
    val components = componentInfoList(type)
    val source = if (components?.isNotEmpty() == true || isApk) {
      components
    } else {
      // Do not combine component flags here: huge apps can exceed Binder's
      // transaction limit when a single PackageInfo carries every component.
      installedAppRepository.getPackageInfo(packageName, componentFlag(type))
        ?.componentInfoList(type)
    }

    return PackageUtils.getComponentList(packageName, source, true)
  }

  private fun PackageInfo.componentInfoList(type: Int): Array<out ComponentInfo>? {
    return when (type) {
      SERVICE -> services
      ACTIVITY -> activities
      RECEIVER -> receivers
      PROVIDER -> providers
      else -> null
    }
  }

  private fun componentFlag(type: Int): Int {
    return when (type) {
      SERVICE -> PackageManager.GET_SERVICES
      ACTIVITY -> PackageManager.GET_ACTIVITIES
      RECEIVER -> PackageManager.GET_RECEIVERS
      PROVIDER -> PackageManager.GET_PROVIDERS
      else -> 0
    }
  }
}

data class AppDetailComponents(
  val services: List<StatefulComponent>,
  val activities: List<StatefulComponent>,
  val receivers: List<StatefulComponent>,
  val providers: List<StatefulComponent>,
  val intentFiltersByClassName: Map<String, List<ParsedIntentFilter>> = emptyMap()
)
