package com.absinthe.libchecker.domain.app.detail

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.AdaptiveIconDrawable
import com.absinthe.libchecker.app.SystemServices
import com.absinthe.libchecker.compat.PackageManagerCompat
import com.absinthe.libchecker.compat.ZipFileCompat
import com.absinthe.libchecker.database.entity.Features
import com.absinthe.libchecker.domain.app.AppListRepository
import com.absinthe.libchecker.domain.app.InstalledAppRepository
import com.absinthe.libchecker.domain.app.KotlinToolingMetadata
import com.absinthe.libchecker.domain.app.VersionedFeature
import com.absinthe.libchecker.domain.app.detail.model.AppIconItem
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.getFeatures
import com.absinthe.libchecker.utils.extensions.getRxAndroidVersion
import com.absinthe.libchecker.utils.extensions.getRxJavaVersion
import com.absinthe.libchecker.utils.extensions.getRxKotlinVersion
import com.absinthe.libchecker.utils.extensions.isPWA
import com.absinthe.libchecker.utils.extensions.isPageSizeCompat
import com.absinthe.libchecker.utils.extensions.isPlayAppSigning
import com.absinthe.libchecker.utils.extensions.isUseKMP
import com.absinthe.libchecker.utils.extensions.toClassDefType
import com.absinthe.libchecker.utils.fromJson
import java.io.File
import java.io.InputStreamReader
import java.util.Properties
import kotlinx.coroutines.CancellationException

class GetAppDetailFeaturesUseCase(
  private val appListRepository: AppListRepository,
  private val installedAppRepository: InstalledAppRepository
) {

  suspend operator fun invoke(
    packageInfo: PackageInfo,
    cachedFeatures: Int,
    isApk: Boolean,
    onFeature: suspend (VersionedFeature) -> Unit = {},
    onAppIcons: suspend (List<AppIconItem>) -> Unit = {}
  ): AppDetailFeatures {
    val features = mutableListOf<VersionedFeature>()
    suspend fun emitFeature(feature: VersionedFeature) {
      features.add(feature)
      onFeature(feature)
    }

    emitFeature(VersionedFeature(Features.Ext.APPLICATION_PROP))

    if (OsUtils.atLeastR() && !isApk) {
      val info = installedAppRepository.getInstallSource(packageInfo.packageName)
      if (info?.installingPackageName != null) {
        emitFeature(
          VersionedFeature(
            Features.Ext.APPLICATION_INSTALL_SOURCE,
            info.initiatingPackageName
          )
        )
      }
    }

    var feat = cachedFeatures
    if (feat == -1) {
      feat = packageInfo.getFeatures()
      appListRepository.updateFeatures(packageInfo.packageName, feat)
    }

    if ((feat and Features.SPLIT_APKS) > 0) {
      emitFeature(VersionedFeature(Features.SPLIT_APKS))
    }
    val hasXposedMetadata = packageInfo.hasXposedModuleMetadata()
    if (hasXposedMetadata) {
      emitFeature(VersionedFeature(Features.XPOSED_MODULE))
    }
    emitFeatureApkMetadata(
      packageInfo = packageInfo,
      readKotlin = (feat and Features.KOTLIN_USED) > 0,
      readAgp = (feat and Features.AGP) > 0,
      readCompose = (feat and Features.JETPACK_COMPOSE) > 0,
      readXposedMarker = !hasXposedMetadata,
      emitFeature = ::emitFeature
    )
    if (packageInfo.isPlayAppSigning()) {
      emitFeature(VersionedFeature(Features.PLAY_SIGNING))
    }
    if (packageInfo.isPWA()) {
      emitFeature(VersionedFeature(Features.PWA))
    }

    val appIcons = getAllAppIcons(packageInfo)
    onAppIcons(appIcons)
    if (appIcons.isNotEmpty()) {
      emitFeature(VersionedFeature(Features.Ext.APPLICATION_ICONS))
    }

    if (OsUtils.atLeastBaklava() && packageInfo.isPageSizeCompat()) {
      emitFeature(VersionedFeature(Features.Ext.ELF_PAGE_SIZE_16KB_COMPAT))
    }

    packageInfo.applicationInfo?.sourceDir?.let { sourceDir ->
      val foundList = getFeaturesFoundDexList(feat, sourceDir)
      if ((feat and Features.RX_JAVA) > 0) {
        val version = packageInfo.getRxJavaVersion(foundList)
        emitFeature(VersionedFeature(Features.RX_JAVA, version))
      }
      if ((feat and Features.RX_KOTLIN) > 0) {
        val version = packageInfo.getRxKotlinVersion(foundList)
        emitFeature(VersionedFeature(Features.RX_KOTLIN, version))
      }
      if ((feat and Features.RX_ANDROID) > 0) {
        val version = packageInfo.getRxAndroidVersion(foundList)
        emitFeature(VersionedFeature(Features.RX_ANDROID, version))
      }
      if (packageInfo.isUseKMP(foundList)) {
        emitFeature(VersionedFeature(Features.KMP))
      }
    }

    return AppDetailFeatures(features, appIcons)
  }

  private suspend fun emitFeatureApkMetadata(
    packageInfo: PackageInfo,
    readKotlin: Boolean,
    readAgp: Boolean,
    readCompose: Boolean,
    readXposedMarker: Boolean,
    emitFeature: suspend (VersionedFeature) -> Unit
  ) {
    if (!readKotlin && !readAgp && !readCompose && !readXposedMarker) {
      return
    }

    suspend fun emitFallbackFeatures() {
      if (readKotlin) {
        emitFeature(VersionedFeature(Features.KOTLIN_USED, extras = DEFAULT_KOTLIN_PLUGIN_INFO))
      }
      if (readAgp) {
        emitFeature(VersionedFeature(Features.AGP))
      }
      if (readCompose) {
        emitFeature(VersionedFeature(Features.JETPACK_COMPOSE))
      }
    }

    val sourceDir = packageInfo.applicationInfo?.sourceDir
    if (sourceDir == null) {
      emitFallbackFeatures()
      return
    }

    val emittedFromApk = try {
      ZipFileCompat(File(sourceDir)).use { zip ->
        if (readKotlin) {
          emitFeature(VersionedFeature(Features.KOTLIN_USED, extras = readKotlinPluginInfo(zip)))
        }
        if (readAgp) {
          emitFeature(VersionedFeature(Features.AGP, readAgpVersion(zip)))
        }
        if (readCompose) {
          emitFeature(
            VersionedFeature(
              Features.JETPACK_COMPOSE,
              readFirstPresentLine(zip, COMPOSE_VERSION_ENTRIES)
            )
          )
        }
        if (readXposedMarker && zip.getEntry(XPOSED_MODULE_PROP_ENTRY) != null) {
          emitFeature(VersionedFeature(Features.XPOSED_MODULE))
        }
      }
      true
    } catch (e: CancellationException) {
      throw e
    } catch (_: Throwable) {
      false
    }

    if (!emittedFromApk) {
      emitFallbackFeatures()
    }
  }

  private fun readKotlinPluginInfo(zip: ZipFileCompat): Map<String, String?> {
    val map = DEFAULT_KOTLIN_PLUGIN_INFO.toMutableMap()
    val entry = zip.getEntry(KOTLIN_TOOLING_METADATA_ENTRY) ?: return map
    return runCatching {
      val json = InputStreamReader(zip.getInputStream(entry), Charsets.UTF_8).use { it.readText() }
      val metadata = json.fromJson<KotlinToolingMetadata>() ?: return@runCatching map
      val kotlinAndroidTarget =
        metadata.projectTargets?.find { target -> target.target == KOTLIN_ANDROID_TARGET }

      map["Kotlin"] =
        metadata.buildPluginVersion.takeIf { metadata.buildPlugin == KOTLIN_ANDROID_PLUGIN || kotlinAndroidTarget != null }
      if (metadata.buildSystem == GRADLE_BUILD_SYSTEM && metadata.buildSystemVersion.isNotEmpty()) {
        map["Gradle"] = metadata.buildSystemVersion
      }

      val sourceCompatibility = kotlinAndroidTarget?.extras?.android?.sourceCompatibility
      if (kotlinAndroidTarget != null && sourceCompatibility?.all { it.isDigit() } == true) {
        map["Java"] = sourceCompatibility
      }
      map
    }.getOrDefault(DEFAULT_KOTLIN_PLUGIN_INFO)
  }

  private fun readAgpVersion(zip: ZipFileCompat): String? {
    zip.getEntry(AGP_METADATA_ENTRY)?.let { entry ->
      runCatching {
        Properties().apply {
          load(zip.getInputStream(entry))
        }.getProperty(AGP_KEYWORD)?.takeIf { it.isNotBlank() }
      }.getOrNull()?.let { return it }
    }

    zip.getEntry(MANIFEST_MF_ENTRY)?.let { entry ->
      runCatching {
        InputStreamReader(zip.getInputStream(entry), Charsets.UTF_8).buffered().useLines { lines ->
          lines.firstOrNull { it.startsWith(AGP_MANIFEST_PREFIX) }
            ?.removePrefix(AGP_MANIFEST_PREFIX)
            ?.takeIf { version -> version.isNotBlank() }
        }
      }.getOrNull()?.let { return it }
    }

    return readFirstPresentLine(zip, DATA_BINDING_VERSION_ENTRIES)
  }

  private fun readFirstPresentLine(zip: ZipFileCompat, entries: Array<String>): String? {
    entries.forEach { name ->
      zip.getEntry(name)?.let { entry ->
        runCatching {
          InputStreamReader(zip.getInputStream(entry), Charsets.UTF_8).buffered().use { it.readLine() }
            ?.takeIf { line -> line.isNotBlank() }
        }.getOrNull()?.let { return it }
      }
    }
    return null
  }

  private fun PackageInfo.hasXposedModuleMetadata(): Boolean {
    val metaData = applicationInfo?.metaData ?: return false
    return metaData.getBoolean("xposedmodule") || metaData.containsKey("xposedminversion")
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

private val DEFAULT_KOTLIN_PLUGIN_INFO: Map<String, String?> = mapOf("Kotlin" to null)

private const val KOTLIN_TOOLING_METADATA_ENTRY = "kotlin-tooling-metadata.json"
private const val KOTLIN_ANDROID_TARGET = "org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget"
private const val KOTLIN_ANDROID_PLUGIN = "org.jetbrains.kotlin.gradle.plugin.KotlinAndroidPluginWrapper"
private const val GRADLE_BUILD_SYSTEM = "Gradle"
private const val AGP_METADATA_ENTRY = "META-INF/com/android/build/gradle/app-metadata.properties"
private const val AGP_KEYWORD = "androidGradlePluginVersion"
private const val MANIFEST_MF_ENTRY = "META-INF/MANIFEST.MF"
private const val AGP_MANIFEST_PREFIX = "Created-By: Android Gradle "
private const val XPOSED_MODULE_PROP_ENTRY = "META-INF/xposed/module.prop"

private val DATA_BINDING_VERSION_ENTRIES = arrayOf(
  "META-INF/androidx.databinding_viewbinding.version",
  "META-INF/androidx.databinding_databindingKtx.version",
  "META-INF/androidx.databinding_library.version"
)

private val COMPOSE_VERSION_ENTRIES = arrayOf(
  "META-INF/androidx.compose.runtime_runtime.version",
  "META-INF/androidx.compose.ui_ui.version",
  "META-INF/androidx.compose.ui_ui-tooling-preview.version",
  "META-INF/androidx.compose.foundation_foundation.version",
  "META-INF/androidx.compose.animation_animation.version"
)
