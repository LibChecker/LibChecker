package com.absinthe.libchecker.domain.app

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.AdaptiveIconDrawable
import com.absinthe.libchecker.app.SystemServices
import com.absinthe.libchecker.compat.PackageManagerCompat
import com.absinthe.libchecker.database.entity.Features
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.getAGPVersion
import com.absinthe.libchecker.utils.extensions.getFeatures
import com.absinthe.libchecker.utils.extensions.getJetpackComposeVersion
import com.absinthe.libchecker.utils.extensions.getKotlinPluginInfo
import com.absinthe.libchecker.utils.extensions.getRxAndroidVersion
import com.absinthe.libchecker.utils.extensions.getRxJavaVersion
import com.absinthe.libchecker.utils.extensions.getRxKotlinVersion
import com.absinthe.libchecker.utils.extensions.isPWA
import com.absinthe.libchecker.utils.extensions.isPageSizeCompat
import com.absinthe.libchecker.utils.extensions.isPlayAppSigning
import com.absinthe.libchecker.utils.extensions.isUseKMP
import com.absinthe.libchecker.utils.extensions.isXposedModule
import com.absinthe.libchecker.utils.extensions.toClassDefType
import java.io.File

class GetAppDetailFeaturesUseCase(
  private val appListRepository: AppListRepository,
  private val installedAppRepository: InstalledAppRepository
) {

  suspend operator fun invoke(
    packageInfo: PackageInfo,
    cachedFeatures: Int,
    isApk: Boolean
  ): AppDetailFeatures {
    val features = mutableListOf<VersionedFeature>()
    features.add(VersionedFeature(Features.Ext.APPLICATION_PROP))

    if (OsUtils.atLeastR() && !isApk) {
      val info = installedAppRepository.getInstallSource(packageInfo.packageName)
      if (info?.installingPackageName != null) {
        features.add(VersionedFeature(Features.Ext.APPLICATION_INSTALL_SOURCE, info.initiatingPackageName))
      }
    }

    var feat = cachedFeatures
    if (feat == -1) {
      feat = packageInfo.getFeatures()
      appListRepository.updateFeatures(packageInfo.packageName, feat)
    }

    if ((feat and Features.SPLIT_APKS) > 0) {
      features.add(VersionedFeature(Features.SPLIT_APKS))
    }
    if ((feat and Features.KOTLIN_USED) > 0) {
      val versionInfo = packageInfo.getKotlinPluginInfo()
      features.add(VersionedFeature(Features.KOTLIN_USED, extras = versionInfo))
    }
    if ((feat and Features.AGP) > 0) {
      val version = packageInfo.getAGPVersion()
      features.add(VersionedFeature(Features.AGP, version))
    }
    if ((feat and Features.JETPACK_COMPOSE) > 0) {
      val version = packageInfo.getJetpackComposeVersion()
      features.add(VersionedFeature(Features.JETPACK_COMPOSE, version))
    }
    if (packageInfo.isXposedModule()) {
      features.add(VersionedFeature(Features.XPOSED_MODULE))
    }
    if (packageInfo.isPlayAppSigning()) {
      features.add(VersionedFeature(Features.PLAY_SIGNING))
    }
    if (packageInfo.isPWA()) {
      features.add(VersionedFeature(Features.PWA))
    }

    val appIcons = getAllAppIcons(packageInfo)
    if (appIcons.isNotEmpty()) {
      features.add(VersionedFeature(Features.Ext.APPLICATION_ICONS))
    }

    if (OsUtils.atLeastBaklava() && packageInfo.isPageSizeCompat()) {
      features.add(VersionedFeature(Features.Ext.ELF_PAGE_SIZE_16KB_COMPAT))
    }

    packageInfo.applicationInfo?.sourceDir?.let { sourceDir ->
      val foundList = getFeaturesFoundDexList(feat, sourceDir)
      if ((feat and Features.RX_JAVA) > 0) {
        val version = packageInfo.getRxJavaVersion(foundList)
        features.add(VersionedFeature(Features.RX_JAVA, version))
      }
      if ((feat and Features.RX_KOTLIN) > 0) {
        val version = packageInfo.getRxKotlinVersion(foundList)
        features.add(VersionedFeature(Features.RX_KOTLIN, version))
      }
      if ((feat and Features.RX_ANDROID) > 0) {
        val version = packageInfo.getRxAndroidVersion(foundList)
        features.add(VersionedFeature(Features.RX_ANDROID, version))
      }
      if (packageInfo.isUseKMP(foundList)) {
        features.add(VersionedFeature(Features.KMP))
      }
    }

    return AppDetailFeatures(features, appIcons)
  }

  private fun getFeaturesFoundDexList(feat: Int, sourceDir: String): List<String>? {
    val dexList = mutableListOf<String>()
    if ((feat and Features.RX_JAVA) > 0) {
      dexList.addAll(
        listOf(
          "rx.schedulers.*".toClassDefType(),
          "io.reactivex.*".toClassDefType(),
          "io.reactivex.rxjava3.*".toClassDefType()
        )
      )
    }
    if ((feat and Features.RX_KOTLIN) > 0) {
      dexList.addAll(
        listOf(
          "io.reactivex.rxjava3.kotlin.*".toClassDefType(),
          "io.reactivex.rxkotlin".toClassDefType(),
          "rx.lang.kotlin".toClassDefType()
        )
      )
    }
    if ((feat and Features.RX_ANDROID) > 0) {
      dexList.addAll(
        listOf(
          "io.reactivex.rxjava3.android.*".toClassDefType(),
          "io.reactivex.android.*".toClassDefType(),
          "rx.android.*".toClassDefType()
        )
      )
    }
    if (dexList.isNotEmpty()) {
      dexList.add("org.jetbrains.compose.*".toClassDefType())
    }
    return if (dexList.isNotEmpty()) {
      PackageUtils.findDexClasses(File(sourceDir), dexList)
    } else {
      null
    }
  }

  private fun getAllAppIcons(packageInfo: PackageInfo): List<AppIconItem> {
    if (!OsUtils.atLeastO()) return emptyList()
    val applicationInfo = packageInfo.applicationInfo ?: return emptyList()
    val packageManager = SystemServices.packageManager
    val icons = mutableListOf<AppIconItem>()

    val mainIcon = packageManager.getApplicationIcon(applicationInfo)

    var hasAddedMonochrome = false
    if (OsUtils.atLeastT() && mainIcon is AdaptiveIconDrawable && mainIcon.monochrome != null) {
      icons.add(AppIconItem(mainIcon, true))
      hasAddedMonochrome = true
    }

    if (!hasAddedMonochrome && OsUtils.atLeastT() && applicationInfo.icon != 0) {
      try {
        val resources = packageManager.getResourcesForApplication(applicationInfo)
        val rawIcon = resources.getDrawable(applicationInfo.icon, null)

        if (rawIcon is AdaptiveIconDrawable && rawIcon.monochrome != null) {
          icons.add(AppIconItem(rawIcon, true))
        }
      } catch (_: Exception) {
      }
    }

    val altIconsIntent = Intent(Intent.ACTION_MAIN).apply {
      addCategory(Intent.CATEGORY_LAUNCHER)
      setPackage(packageInfo.packageName)
    }
    val intents = PackageManagerCompat.queryIntentActivities(altIconsIntent, PackageManager.MATCH_DISABLED_COMPONENTS)
    val iconResSet = mutableSetOf(applicationInfo.icon)
    intents
      .asSequence()
      .filter { !iconResSet.contains(it.iconResource) }
      .map {
        iconResSet.add(it.iconResource)
        it.loadIcon(SystemServices.packageManager)
      }
      .forEach { icons.add(AppIconItem(it, false)) }
    return icons
  }
}

data class AppDetailFeatures(
  val features: List<VersionedFeature>,
  val appIcons: List<AppIconItem>
)
