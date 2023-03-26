package com.absinthe.libchecker.utils.extensions

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageInfoHidden
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.core.content.pm.PackageInfoCompat
import com.absinthe.libchecker.database.entity.Features
import com.absinthe.libchecker.model.KotlinToolingMetadata
import com.absinthe.libchecker.model.LibStringItem
import com.absinthe.libchecker.utils.FileUtils
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.fromJson
import com.absinthe.libchecker.utils.manifest.HiddenPermissionsReader
import com.absinthe.libchecker.utils.manifest.ManifestReader
import dev.rikka.tools.refine.Refine
import java.io.File
import java.text.DateFormat
import java.util.Properties
import java.util.zip.ZipFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.buffer
import okio.source
import rikka.material.app.LocaleDelegate

/**
 * Get version code of an app
 * @return version code as Long Integer
 */
fun PackageInfo.getVersionCode(): Long {
  return PackageInfoCompat.getLongVersionCode(this)
}

/**
 * Get version string of an app ( 1.0.0(1) )
 * @return version code as String
 */
fun PackageInfo.getVersionString(): String {
  return try {
    "${versionName ?: "<unknown>"} (${getVersionCode()})"
  } catch (e: PackageManager.NameNotFoundException) {
    "Unknown"
  }
}

/**
 * Get target api string of an app ( API 30 )
 * @return version code as String
 */
fun PackageInfo.getTargetApiString(): String {
  return runCatching {
    applicationInfo.targetSdkVersion.toString()
  }.getOrDefault("?")
}

private const val compileSdkVersion = "compileSdkVersion"

/**
 * Get compileSdkVersion of an app
 * @return compileSdkVersion
 */
fun PackageInfo.getCompileSdkVersion(): String {
  return runCatching {
    val version = if (OsUtils.atLeastS()) {
      applicationInfo.compileSdkVersion
    } else {
      val demands = ManifestReader.getManifestProperties(
        File(applicationInfo.sourceDir),
        arrayOf(compileSdkVersion)
      )
      demands[compileSdkVersion]?.toString()?.toInt() ?: 0
    }

    if (version == 0) {
      "?"
    } else {
      version.toString()
    }
  }.getOrDefault("?")
}

/**
 * Get permissions list of an app
 * @return Permissions list
 */
fun PackageInfo.getPermissionsList(): List<String> {
  return requestedPermissions?.toList() ?: emptyList()
}

/**
 * Get stateful permissions list of an app
 * @return Stateful permissions list
 */
fun PackageInfo.getStatefulPermissionsList(): List<Pair<String, Boolean>> {
  val flags = requestedPermissionsFlags
  val hidden =
    HiddenPermissionsReader.getHiddenPermissions(File(applicationInfo.sourceDir)).filter { (_, v) ->
      OsUtils.higherThan(v as Int)
    }

  if (flags?.size != requestedPermissions?.size) {
    return requestedPermissions?.map { it to true }?.toMutableList()?.apply {
      if (hidden.isNotEmpty()) {
        hidden.forEach { (p, v) ->
          add("$p (maxSdkVersion: $v)" to false)
        }
      }
    } ?: emptyList()
  }

  return requestedPermissions?.mapIndexed { index, s ->
    s to (flags[index] and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0)
  }?.toMutableList()?.apply {
    if (hidden.isNotEmpty()) {
      hidden.forEach { (p, v) ->
        add("$p (maxSdkVersion: $v)" to false)
      }
    }
  } ?: emptyList()
}

/**
 * Check if an app uses split apks
 * @return true if it uses split apks
 */
fun PackageInfo.isSplitsApk(): Boolean {
  return !applicationInfo.splitSourceDirs.isNullOrEmpty()
}

/**
 * Check if an app uses Kotlin language
 * @return true if it uses Kotlin language
 */
fun PackageInfo.isKotlinUsed(): Boolean {
  return runCatching {
    val file = File(applicationInfo.sourceDir)

    ZipFile(file).use {
      it.getEntry("kotlin-tooling-metadata.json") != null ||
        it.getEntry("kotlin/kotlin.kotlin_builtins") != null ||
        it.getEntry("META-INF/services/kotlinx.coroutines.CoroutineExceptionHandler") != null ||
        it.getEntry("META-INF/services/kotlinx.coroutines.internal.MainDispatcherFactory") != null ||
        PackageUtils.isKotlinUsedInClassDex(file)
    }
  }.getOrDefault(false)
}

private const val AGP_KEYWORD = "androidGradlePluginVersion"
private const val AGP_KEYWORD2 = "Created-By: Android Gradle "

/**
 * Get Android Gradle Plugin version of an app
 * @return Android Gradle Plugin version or null if not found
 */
fun PackageInfo.getAGPVersion(): String? {
  runCatching {
    ZipFile(File(applicationInfo.sourceDir)).use { zipFile ->
      zipFile.getEntry("META-INF/com/android/build/gradle/app-metadata.properties")?.let { ze ->
        Properties().apply {
          load(zipFile.getInputStream(ze))
          getProperty(AGP_KEYWORD)?.let {
            return it
          }
        }
      }
      zipFile.getEntry("META-INF/MANIFEST.MF")?.let { ze ->
        zipFile.getInputStream(ze).source().buffer().use {
          while (true) {
            it.readUtf8Line()?.let { line ->
              if (line.startsWith(AGP_KEYWORD2)) {
                return line.removePrefix(AGP_KEYWORD2)
              }
            } ?: break
          }
        }
      }
      arrayOf(
        "META-INF/androidx.databinding_viewbinding.version",
        "META-INF/androidx.databinding_databindingKtx.version",
        "META-INF/androidx.databinding_library.version"
      ).forEach { entry ->
        zipFile.getEntry(entry)?.let { ze ->
          zipFile.getInputStream(ze).source().buffer().use { bs ->
            return bs.readUtf8Line().takeIf { it?.isNotBlank() == true }
          }
        }
      }
    }
  }

  return null
}

/**
 * Get package size of an app
 * @param includeSplits Whether to include split APKs
 * @return Package size
 */
fun PackageInfo.getPackageSize(includeSplits: Boolean): Long {
  var size: Long = FileUtils.getFileSize(applicationInfo.sourceDir)

  if (!includeSplits) {
    return size
  }

  PackageUtils.getSplitsSourceDir(this)?.forEach {
    runCatching {
      size += FileUtils.getFileSize(it)
    }
  }
  return size
}

/**
 * Check if an app is a Xposed module
 * @return True if is a Xposed module
 */
fun PackageInfo.isXposedModule(): Boolean {
  return applicationInfo.metaData?.getBoolean("xposedmodule") == true ||
    applicationInfo.metaData?.containsKey("xposedminversion") == true
}

/**
 * Check if an app contains Play App Signing
 * @return True if contains Play App Signing
 */
fun PackageInfo.isPlayAppSigning(): Boolean {
  return applicationInfo.metaData?.getString("com.android.stamp.type") == "STAMP_TYPE_DISTRIBUTION_APK" &&
    applicationInfo.metaData?.getString("com.android.stamp.source") == "https://play.google.com/store"
}

/**
 * Check if an app is PWA
 * @return True if is PWA
 */
fun PackageInfo.isPWA(): Boolean {
  return applicationInfo.metaData?.keySet()
    ?.any { it.startsWith("org.chromium.webapk.shell_apk") } == true
}

/**
 * Check if an app is Overlay
 * @return True if is Overlay
 */
fun PackageInfo.isOverlay(): Boolean {
  return try {
    Refine.unsafeCast<PackageInfoHidden>(this).isOverlayPackage
  } catch (t: Throwable) {
    if (applicationInfo.sourceDir == null) return false
    val demands =
      ManifestReader.getManifestProperties(File(applicationInfo.sourceDir), arrayOf("overlay"))
    return demands["overlay"] as? Boolean ?: false
  }
}

/**
 * Get features of an app
 * @return Features
 */
fun PackageInfo.getFeatures(): Int {
  var features = 0
  val resultList = PackageUtils.findDexClasses(
    File(applicationInfo.sourceDir),
    listOf(
      "androidx.compose.*".toClassDefType(),
      "rx.*".toClassDefType(),
      "io.reactivex.*".toClassDefType(),
      "io.reactivex.rxjava3.*".toClassDefType(),
      "io.reactivex.rxjava3.kotlin.*".toClassDefType(),
      "io.reactivex.rxkotlin".toClassDefType(),
      "rx.lang.kotlin".toClassDefType(),
      "io.reactivex.rxjava3.android.*".toClassDefType(),
      "io.reactivex.android.*".toClassDefType(),
      "rx.android.*".toClassDefType()
    )
  )
  if (isSplitsApk()) {
    features = features or Features.SPLIT_APKS
  }
  if (isKotlinUsed()) {
    features = features or Features.KOTLIN_USED
  }
  if (getAGPVersion()?.isNotBlank() == true) {
    features = features or Features.AGP
  }
  if (isXposedModule()) {
    features = features or Features.XPOSED_MODULE
  }
  if (isPlayAppSigning()) {
    features = features or Features.PLAY_SIGNING
  }
  if (isPWA()) {
    features = features or Features.PWA
  }
  if (isUseJetpackCompose(resultList)) {
    features = features or Features.JETPACK_COMPOSE
  }
  if (isRxJavaUsed(resultList)) {
    features = features or Features.RX_JAVA
  }
  if (isRxKotlinUsed(resultList)) {
    features = features or Features.RX_KOTLIN
  }
  if (isRxAndroidUsed(resultList)) {
    features = features or Features.RX_ANDROID
  }

  return features
}

/**
 * Check if an app is using 32-bit ABI
 * @return True if is using 32-bit ABI
 */
fun ApplicationInfo.isUse32BitAbi(): Boolean {
  runCatching {
    val demands = ManifestReader.getManifestProperties(File(sourceDir), arrayOf("use32bitAbi"))
    return demands["use32bitAbi"] as? Boolean ?: false
  }.getOrNull() ?: return false
}

/**
 * Get Kotlin plugin version of an app
 * @return Kotlin plugin version or null if not found
 */
fun PackageInfo.getKotlinPluginVersion(): String? {
  return runCatching {
    ZipFile(applicationInfo.sourceDir).use { zip ->
      val entry = zip.getEntry("kotlin-tooling-metadata.json") ?: return@runCatching null
      zip.getInputStream(entry).source().buffer().use {
        val json = it.readUtf8().fromJson<KotlinToolingMetadata>()
        return json?.buildPluginVersion.takeIf { json?.buildPlugin == "org.jetbrains.kotlin.gradle.plugin.KotlinAndroidPluginWrapper" }
      }
    }
  }.getOrNull()
}

/**
 * Check if an app is using Jetpack Compose
 * @return True if is using Jetpack Compose
 */
fun PackageInfo.isUseJetpackCompose(foundList: List<String>? = null): Boolean {
  val usedInMetaInf = runCatching {
    val file = File(applicationInfo.sourceDir)

    ZipFile(file).use {
      it.entries().asSequence().any { entry ->
        entry.isDirectory.not() &&
          entry.name.startsWith("androidx.compose") &&
          entry.name.endsWith(".version")
      }
    }
  }.getOrDefault(false)
  if (usedInMetaInf) {
    return true
  }
  if (foundList.isNullOrEmpty().not()) {
    return foundList?.contains("androidx.compose.*".toClassDefType()) == true
  }
  return PackageUtils.findDexClasses(
    File(applicationInfo.sourceDir),
    listOf("androidx.compose.*".toClassDefType())
  ).isNotEmpty()
}

/**
 * Get Jetpack Compose version of an app
 * @return Jetpack Compose version or null if not found
 */
fun PackageInfo.getJetpackComposeVersion(): String? {
  runCatching {
    ZipFile(File(applicationInfo.sourceDir)).use { zipFile ->
      arrayOf(
        "META-INF/androidx.compose.runtime_runtime.version",
        "META-INF/androidx.compose.ui_ui.version",
        "META-INF/androidx.compose.ui_ui-tooling-preview.version"
      ).forEach { entry ->
        zipFile.getEntry(entry)?.let { ze ->
          zipFile.getInputStream(ze).source().buffer().use { bs ->
            return bs.readUtf8Line().takeIf { it?.isNotBlank() == true }
          }
        }
      }
    }
  }

  return null
}

private const val RX_MAJOR_ONE = "1"
private const val RX_MAJOR_TWO = "2"
private const val RX_MAJOR_THREE = "3"

/**
 * Check if an app uses RxJava framework
 * @return true if it uses RxJava framework
 */
fun PackageInfo.isRxJavaUsed(foundList: List<String>? = null): Boolean {
  val usedInMetaInf = runCatching {
    val file = File(applicationInfo.sourceDir)

    ZipFile(file).use {
      it.getEntry("META-INF/rxjava.properties") != null
    }
  }.getOrDefault(false)
  if (usedInMetaInf) {
    return true
  }
  if (foundList.isNullOrEmpty().not()) {
    return foundList?.contains("rx.*".toClassDefType()) == true ||
      foundList?.contains("io.reactivex.*".toClassDefType()) == true ||
      foundList?.contains("io.reactivex.rxjava3.*".toClassDefType()) == true
  }
  return PackageUtils.findDexClasses(
    File(applicationInfo.sourceDir),
    listOf(
      "rx.*".toClassDefType(),
      "io.reactivex.*".toClassDefType(),
      "io.reactivex.rxjava3.*".toClassDefType()
    ),
    hasAny = true
  ).isNotEmpty()
}

private const val REACTIVEX_KEYWORD = "Implementation-Version"

suspend fun PackageInfo.getRxJavaVersion(): String? = withContext(Dispatchers.IO) {
  runCatching {
    ZipFile(File(applicationInfo.sourceDir)).use { zipFile ->
      zipFile.getEntry("META-INF/rxjava.properties")?.let { ze ->
        Properties().apply {
          load(zipFile.getInputStream(ze))
          getProperty(REACTIVEX_KEYWORD)?.let {
            return@withContext it
          }
        }
      }
    }
    val resultList = PackageUtils.findDexClasses(
      File(applicationInfo.sourceDir),
      listOf(
        "rx.*".toClassDefType(),
        "io.reactivex.*".toClassDefType(),
        "io.reactivex.rxjava3.*".toClassDefType()
      ),
      hasAny = true
    )
    if (resultList.contains("io.reactivex.rxjava3.*".toClassDefType())) {
      return@withContext RX_MAJOR_THREE
    }
    if (resultList.contains("io.reactivex.*".toClassDefType())) {
      return@withContext RX_MAJOR_TWO
    }
    if (resultList.contains("rx.*".toClassDefType())) {
      return@withContext RX_MAJOR_ONE
    }
  }
  return@withContext null
}

/**
 * Check if an app uses RxKotlin framework
 * @return true if it uses RxKotlin framework
 */
fun PackageInfo.isRxKotlinUsed(foundList: List<String>? = null): Boolean {
  val usedInMetaInf = runCatching {
    val file = File(applicationInfo.sourceDir)

    ZipFile(file).use {
      it.getEntry("META-INF/rxkotlin.properties") != null
    }
  }.getOrDefault(false)
  if (usedInMetaInf) {
    return true
  }
  if (foundList.isNullOrEmpty().not()) {
    return foundList?.contains("io.reactivex.rxjava3.kotlin.*".toClassDefType()) == true ||
      foundList?.contains("io.reactivex.rxkotlin".toClassDefType()) == true ||
      foundList?.contains("rx.lang.kotlin".toClassDefType()) == true
  }
  return PackageUtils.findDexClasses(
    File(applicationInfo.sourceDir),
    listOf(
      "io.reactivex.rxjava3.kotlin.*".toClassDefType(),
      "io.reactivex.rxkotlin".toClassDefType(),
      "rx.lang.kotlin".toClassDefType()
    ),
    hasAny = true
  ).isNotEmpty()
}

suspend fun PackageInfo.getRxKotlinVersion(): String? = withContext(Dispatchers.IO) {
  runCatching {
    ZipFile(File(applicationInfo.sourceDir)).use { zipFile ->
      zipFile.getEntry("META-INF/rxkotlin.properties")?.let { ze ->
        Properties().apply {
          load(zipFile.getInputStream(ze))
          getProperty(REACTIVEX_KEYWORD)?.let {
            return@withContext it
          }
        }
      }
    }
    val resultList = PackageUtils.findDexClasses(
      File(applicationInfo.sourceDir),
      listOf(
        "io.reactivex.rxjava3.kotlin.*".toClassDefType(),
        "io.reactivex.rxkotlin".toClassDefType(),
        "rx.lang.kotlin".toClassDefType()
      ),
      hasAny = true
    )
    if (resultList.contains("io.reactivex.rxjava3.kotlin.*".toClassDefType())) {
      return@withContext RX_MAJOR_THREE
    }
    if (resultList.contains("io.reactivex.rxkotlin".toClassDefType())) {
      return@withContext RX_MAJOR_TWO
    }
    if (resultList.contains("rx.lang.kotlin".toClassDefType())) {
      return@withContext RX_MAJOR_ONE
    }
  }
  return@withContext null
}

/**
 * Check if an app uses RxAndroid framework
 * @return true if it uses RxAndroid framework
 */
fun PackageInfo.isRxAndroidUsed(foundList: List<String>? = null): Boolean {
  if (foundList.isNullOrEmpty().not()) {
    return foundList?.contains("io.reactivex.rxjava3.android.*".toClassDefType()) == true ||
      foundList?.contains("io.reactivex.android.*".toClassDefType()) == true ||
      foundList?.contains("rx.android.*".toClassDefType()) == true
  }
  return PackageUtils.findDexClasses(
    File(applicationInfo.sourceDir),
    listOf(
      "io.reactivex.rxjava3.android.*".toClassDefType(),
      "io.reactivex.android.*".toClassDefType(),
      "rx.android.*".toClassDefType()
    ),
    hasAny = true
  ).isNotEmpty()
}

suspend fun PackageInfo.getRxAndroidVersion(): String? = withContext(Dispatchers.IO) {
  val resultList = PackageUtils.findDexClasses(
    File(applicationInfo.sourceDir),
    listOf(
      "io.reactivex.rxjava3.android.*".toClassDefType(),
      "io.reactivex.android.*".toClassDefType(),
      "rx.android.*".toClassDefType()
    ),
    hasAny = true
  )
  if (resultList.contains("io.reactivex.rxjava3.android.*".toClassDefType())) {
    return@withContext RX_MAJOR_THREE
  }
  if (resultList.contains("io.reactivex.android.*".toClassDefType())) {
    return@withContext RX_MAJOR_TWO
  }
  if (resultList.contains("rx.android.*".toClassDefType())) {
    return@withContext RX_MAJOR_ONE
  }
  return@withContext null
}

/**
 * Get signatures of an app
 * @param packageInfo PackageInfo
 * @return List of LibStringItem
 */
fun PackageInfo.getSignatures(context: Context): Sequence<LibStringItem> {
  val localedContext = context.createConfigurationContext(
    Configuration(context.resources.configuration).apply {
      setLocale(LocaleDelegate.defaultLocale)
    }
  )
  val dateFormat = DateFormat.getDateTimeInstance(
    DateFormat.LONG,
    DateFormat.LONG,
    LocaleDelegate.defaultLocale
  )
  return if (OsUtils.atLeastP() && signingInfo != null) {
    if (signingInfo.hasMultipleSigners()) {
      signingInfo.apkContentsSigners
    } else {
      signingInfo.signingCertificateHistory
    }
  } else {
    @Suppress("DEPRECATION")
    signatures
  }.orEmpty().asSequence().map {
    PackageUtils.describeSignature(localedContext, dateFormat, it)
  }
}
