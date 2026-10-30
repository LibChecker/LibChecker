package com.absinthe.libchecker.domain.app.feature

import android.content.pm.PackageInfo
import com.absinthe.libchecker.compat.IZipFile
import com.absinthe.libchecker.compat.ZipFileCompat
import com.absinthe.libchecker.database.entity.Features
import com.absinthe.libchecker.domain.app.buildmetadata.DATA_BINDING_VERSION_ENTRIES
import com.absinthe.libchecker.domain.app.buildmetadata.KotlinUsageDetector
import com.absinthe.libchecker.domain.app.buildmetadata.KotlinUsageDetector.hasKotlinRuntimeEvidenceInClassDex
import com.absinthe.libchecker.utils.dex.DexArchiveScanner
import com.absinthe.libchecker.utils.dex.DexArchiveScanner.findDexClasses
import com.absinthe.libchecker.utils.extensions.isSplitsApk
import com.absinthe.libchecker.utils.extensions.toClassDefType
import java.io.File
import java.io.FilterInputStream
import java.io.InputStream
import java.util.Properties
import java.util.concurrent.CancellationException
import okio.buffer
import okio.source

object PackageFeatureDetector {
  private data class ArchiveFeatureEvidence(
    val hasKotlinEvidence: Boolean,
    val hasComposeEvidence: Boolean
  )

  private fun scanArchiveFeatureEvidence(zipFile: IZipFile, checkCancelled: () -> Unit): ArchiveFeatureEvidence {
    var hasKotlinEvidence = zipFile.getEntry("kotlin-tooling-metadata.json") != null ||
      zipFile.getEntry("kotlin/kotlin.kotlin_builtins") != null ||
      zipFile.getEntry("META-INF/services/kotlinx.coroutines.CoroutineExceptionHandler") != null ||
      zipFile.getEntry("META-INF/services/kotlinx.coroutines.internal.MainDispatcherFactory") != null
    var hasComposeEvidence = false

    for (entry in zipFile.getZipEntries()) {
      checkCancelled()
      if (entry.isDirectory) continue
      val entryName = entry.name
      if (!hasKotlinEvidence && entryName.startsWith("META-INF/") && entryName.endsWith(".kotlin_module")) {
        hasKotlinEvidence = true
      }
      if (!hasComposeEvidence && entryName.endsWith(".version")) {
        val fileName = entryName.substringAfterLast(File.separator)
        if (fileName.startsWith("androidx.compose.ui") || fileName.startsWith("androidx.compose.material")) {
          hasComposeEvidence = true
        }
      }
      if (hasKotlinEvidence && hasComposeEvidence) break
    }
    return ArchiveFeatureEvidence(hasKotlinEvidence, hasComposeEvidence)
  }

  private fun InputStream.cancellable(checkCancelled: () -> Unit): InputStream = object : FilterInputStream(this) {
    override fun read(): Int {
      checkCancelled()
      return super.read()
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
      checkCancelled()
      return `in`.read(buffer, offset, minOf(length, DEFAULT_BUFFER_SIZE))
    }
  }

  private const val AGP_KEYWORD = "androidGradlePluginVersion"

  private const val AGP_KEYWORD2 = "Created-By: Android Gradle "

  private fun readAGPVersion(zipFile: IZipFile, checkCancelled: () -> Unit): String? {
    zipFile.getEntry("META-INF/com/android/build/gradle/app-metadata.properties")?.let { ze ->
      Properties().apply { zipFile.getInputStream(ze).cancellable(checkCancelled).use { load(it) } }
        .getProperty(AGP_KEYWORD)?.let { return it }
    }
    zipFile.getEntry("META-INF/MANIFEST.MF")?.let { ze ->
      zipFile.getInputStream(ze).cancellable(checkCancelled).source().buffer().use {
        while (true) {
          checkCancelled()
          val line = it.readUtf8Line() ?: break
          if (line.startsWith(AGP_KEYWORD2)) return line.removePrefix(AGP_KEYWORD2)
        }
      }
    }
    DATA_BINDING_VERSION_ENTRIES.forEach { name ->
      zipFile.getEntry(name)?.let { ze ->
        zipFile.getInputStream(ze).cancellable(checkCancelled).source().buffer().use { source ->
          return source.readUtf8Line().takeIf { !it.isNullOrBlank() }
        }
      }
    }
    return null
  }

  /**
   * Check if an app contains Play App Signing
   * @return True if contains Play App Signing
   */
  fun PackageInfo.isPlayAppSigning(): Boolean {
    val metaData = applicationInfo?.metaData ?: return false
    return metaData.getString("com.android.stamp.type") == "STAMP_TYPE_DISTRIBUTION_APK" &&
      metaData.getString("com.android.stamp.source") == "https://play.google.com/store"
  }

  /**
   * Check if an app is PWA
   * @return True if is PWA
   */
  fun PackageInfo.isPWA(): Boolean {
    return applicationInfo?.metaData?.keySet()
      ?.any { it.startsWith("org.chromium.webapk.shell_apk") } == true
  }

  /**
   * Get features of an app
   * @return Features
   */
  fun PackageInfo.getFeatures(checkCancelled: () -> Unit = {}): Int {
    checkCancelled()
    val sourceDir = applicationInfo?.sourceDir ?: return 0
    return runCatching {
      ZipFileCompat(File(sourceDir)).use { zipFile -> getFeatures(zipFile, checkCancelled) }
    }.getOrElse {
      if (it is java.util.concurrent.CancellationException) throw it
      getNonArchiveFeatures()
    }
  }

  fun PackageInfo.getFeatures(zipFile: IZipFile, checkCancelled: () -> Unit = {}): Int {
    checkCancelled()
    var features = getNonArchiveFeatures()
    if (applicationInfo?.sourceDir == null) return features
    val archiveEvidence = scanArchiveFeatureEvidence(zipFile, checkCancelled)
    val kotlinEvidence = archiveEvidence.hasKotlinEvidence
    val composeEvidence = archiveEvidence.hasComposeEvidence
    val patterns = buildList {
      if (!composeEvidence) add(COMPOSE_CLASS_PATTERN)
      if (!kotlinEvidence) {
        add(KOTLIN_CLASS_PATTERN)
        add(KOTLINX_CLASS_PATTERN)
      }
    }
    checkCancelled()
    val resultList = DexArchiveScanner.findDexClasses(zipFile, patterns, checkCancelled = checkCancelled)
    if (
      kotlinEvidence || resultList.any { it == KOTLIN_CLASS_PATTERN || it == KOTLINX_CLASS_PATTERN } ||
      KotlinUsageDetector.hasKotlinRuntimeEvidenceInClassDex(zipFile, checkCancelled)
    ) {
      features = features or Features.KOTLIN_USED
    }
    checkCancelled()
    if (!readAGPVersion(zipFile, checkCancelled).isNullOrBlank()) features = features or Features.AGP
    if (zipFile.getEntry("META-INF/xposed/module.prop") != null) features = features or Features.XPOSED_MODULE
    if (composeEvidence || resultList.contains(COMPOSE_CLASS_PATTERN)) features = features or Features.JETPACK_COMPOSE
    return features
  }

  private fun PackageInfo.getNonArchiveFeatures(): Int {
    var features = 0
    if (isSplitsApk()) features = features or Features.SPLIT_APKS
    if (hasXposedModuleMetadata()) features = features or Features.XPOSED_MODULE
    if (isPlayAppSigning()) features = features or Features.PLAY_SIGNING
    if (isPWA()) features = features or Features.PWA
    return features
  }

  internal fun PackageInfo.hasXposedModuleMetadata(): Boolean {
    val metadata = applicationInfo?.metaData ?: return false
    return metadata.getBoolean("xposedmodule") || metadata.containsKey("xposedminversion")
  }

  private val COMPOSE_CLASS_PATTERN = "androidx.compose.*".toClassDefType()

  private val KOTLIN_CLASS_PATTERN = "kotlin.*".toClassDefType()

  private val KOTLINX_CLASS_PATTERN = "kotlinx.*".toClassDefType()
}
