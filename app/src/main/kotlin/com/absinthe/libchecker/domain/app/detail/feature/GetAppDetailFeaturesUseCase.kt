package com.absinthe.libchecker.domain.app.detail.feature

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.AdaptiveIconDrawable
import com.absinthe.libchecker.app.SystemServices
import com.absinthe.libchecker.compat.PackageManagerCompat
import com.absinthe.libchecker.compat.ZipFileCompat
import com.absinthe.libchecker.database.entity.Features
import com.absinthe.libchecker.domain.app.buildmetadata.COMPOSE_VERSION_ENTRIES
import com.absinthe.libchecker.domain.app.buildmetadata.DATA_BINDING_VERSION_ENTRIES
import com.absinthe.libchecker.domain.app.buildmetadata.KotlinBuildMetadata
import com.absinthe.libchecker.domain.app.buildmetadata.KotlinBuildMetadataDetector
import com.absinthe.libchecker.domain.app.buildmetadata.KotlinVersionInferenceHints
import com.absinthe.libchecker.domain.app.buildmetadata.readFirstPresentLine
import com.absinthe.libchecker.domain.app.detail.model.AppIconItem
import com.absinthe.libchecker.domain.app.model.VersionedFeature
import com.absinthe.libchecker.domain.app.repository.AppListRepository
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.extensions.getFeatures
import com.absinthe.libchecker.utils.extensions.hasXposedModuleMetadata
import com.absinthe.libchecker.utils.extensions.isPWA
import com.absinthe.libchecker.utils.extensions.isPageSizeCompat
import com.absinthe.libchecker.utils.extensions.isPlayAppSigning
import com.absinthe.libchecker.utils.extensions.isUseKMP
import java.io.File
import java.io.InputStreamReader
import java.util.Properties
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

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

    val featureContext = currentCoroutineContext()
    var feat = cachedFeatures
    var metadataEmitted = false
    val scannedMetadata = mutableListOf<VersionedFeature>()
    if (feat == -1) {
      val sourceDir = packageInfo.applicationInfo?.sourceDir
      feat = if (sourceDir != null) {
        try {
          ZipFileCompat(File(sourceDir)).use { zip ->
            packageInfo.getFeatures(zip) { featureContext.ensureActive() }.also { scannedFeatures ->
              emitFeatureApkMetadata(
                packageInfo = packageInfo,
                readKotlin = (scannedFeatures and Features.KOTLIN_USED) > 0,
                readAgp = (scannedFeatures and Features.AGP) > 0,
                readCompose = (scannedFeatures and Features.JETPACK_COMPOSE) > 0,
                readXposedMarker = !packageInfo.hasXposedModuleMetadata(),
                isApk = isApk,
                emitFeature = { scannedMetadata.add(it) },
                openedZip = zip
              )
              metadataEmitted = true
            }
          }
        } catch (e: CancellationException) {
          throw e
        } catch (_: Throwable) {
          packageInfo.getFeatures { featureContext.ensureActive() }
        }
      } else {
        packageInfo.getFeatures { featureContext.ensureActive() }
      }
      appListRepository.updateFeatures(packageInfo.packageName, feat)
    }

    if ((feat and Features.SPLIT_APKS) > 0) {
      emitFeature(VersionedFeature(Features.SPLIT_APKS))
    }
    val hasXposedMetadata = packageInfo.hasXposedModuleMetadata()
    if (hasXposedMetadata) {
      emitFeature(VersionedFeature(Features.XPOSED_MODULE))
    }
    if (metadataEmitted) {
      scannedMetadata.forEach { emitFeature(it) }
    } else {
      emitFeatureApkMetadata(
        packageInfo = packageInfo,
        readKotlin = (feat and Features.KOTLIN_USED) > 0,
        readAgp = (feat and Features.AGP) > 0,
        readCompose = (feat and Features.JETPACK_COMPOSE) > 0,
        readXposedMarker = !hasXposedMetadata,
        isApk = isApk,
        emitFeature = ::emitFeature
      )
    }
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

    if (packageInfo.isUseKMP()) {
      emitFeature(VersionedFeature(Features.KMP))
    }

    return AppDetailFeatures(features, appIcons)
  }

  private suspend fun emitFeatureApkMetadata(
    packageInfo: PackageInfo,
    readKotlin: Boolean,
    readAgp: Boolean,
    readCompose: Boolean,
    readXposedMarker: Boolean,
    isApk: Boolean,
    emitFeature: suspend (VersionedFeature) -> Unit,
    openedZip: ZipFileCompat? = null
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

    suspend fun emitFromZip(zip: ZipFileCompat) {
      if (readKotlin) {
        emitFeature(
          VersionedFeature(
            Features.KOTLIN_USED,
            extras = readKotlinBuildInfo(packageInfo, File(sourceDir), zip, isApk)
          )
        )
      }
      if (readAgp) {
        emitFeature(VersionedFeature(Features.AGP, readAgpVersion(zip)))
      }
      if (readCompose) {
        emitFeature(
          VersionedFeature(
            Features.JETPACK_COMPOSE,
            zip.readFirstPresentLine(COMPOSE_VERSION_ENTRIES)
          )
        )
      }
      if (readXposedMarker && zip.getEntry(XPOSED_MODULE_PROP_ENTRY) != null) {
        emitFeature(VersionedFeature(Features.XPOSED_MODULE))
      }
    }

    val emittedFromApk = try {
      if (openedZip != null) {
        emitFromZip(openedZip)
      } else {
        ZipFileCompat(File(sourceDir)).use { zip ->
          emitFromZip(zip)
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

  private fun readKotlinBuildInfo(
    packageInfo: PackageInfo,
    apk: File,
    zip: ZipFileCompat,
    isApk: Boolean
  ): Map<String, String?> {
    val componentPackageInfo = if (isApk) {
      null
    } else {
      installedAppRepository.getPackageInfo(
        packageName = packageInfo.packageName,
        flags = KOTLIN_INFERENCE_PACKAGE_FLAGS,
        resolveFrozenArchiveInfo = false
      )
    }
    return KotlinBuildMetadataDetector.detect(
      apk = apk,
      zip = zip,
      inferenceHints = KotlinVersionInferenceHints.from(packageInfo, componentPackageInfo)
    ).toKotlinDialogEntries()
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

    return zip.readFirstPresentLine(DATA_BINDING_VERSION_ENTRIES)
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

private const val AGP_METADATA_ENTRY = "META-INF/com/android/build/gradle/app-metadata.properties"
private const val AGP_KEYWORD = "androidGradlePluginVersion"
private const val MANIFEST_MF_ENTRY = "META-INF/MANIFEST.MF"
private const val AGP_MANIFEST_PREFIX = "Created-By: Android Gradle "
private const val XPOSED_MODULE_PROP_ENTRY = "META-INF/xposed/module.prop"

private const val KOTLIN_INFERENCE_PACKAGE_FLAGS = PackageManager.GET_ACTIVITIES or
  PackageManager.GET_SERVICES or
  PackageManager.GET_RECEIVERS or
  PackageManager.GET_PROVIDERS or
  PackageManager.GET_INSTRUMENTATION

internal fun KotlinBuildMetadata.toKotlinDialogEntries(): Map<String, String?> {
  return linkedMapOf<String, String?>("Kotlin" to kotlinVersion).apply {
    gradleVersion?.let { version -> put("Gradle", version) }
    javaVersion?.let { version -> put("Java", version) }
  }
}
