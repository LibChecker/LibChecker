package com.absinthe.libchecker.utils.extensions

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageInfoHidden
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import com.absinthe.libchecker.app.SystemServices
import com.absinthe.libchecker.compat.ZipFileCompat
import com.absinthe.libchecker.constant.Constants.ARMV7
import com.absinthe.libchecker.constant.Constants.ARMV8
import com.absinthe.libchecker.constant.Constants.MIPS
import com.absinthe.libchecker.constant.Constants.MIPS64
import com.absinthe.libchecker.constant.Constants.X86
import com.absinthe.libchecker.constant.Constants.X86_64
import com.absinthe.libchecker.database.entity.Features
import com.absinthe.libchecker.features.applist.detail.bean.KotlinToolingMetadata
import com.absinthe.libchecker.features.statistics.bean.LibStringItem
import com.absinthe.libchecker.utils.FileUtils
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.fromJson
import com.absinthe.libchecker.utils.manifest.HiddenPermissionsReader
import com.absinthe.libchecker.utils.manifest.ManifestReader
import dalvik.system.DexFile
import dev.rikka.tools.refine.Refine
import java.io.File
import java.text.DateFormat
import java.util.Properties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.buffer
import okio.source
import rikka.material.app.LocaleDelegate
import timber.log.Timber

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
    applicationInfo!!.targetSdkVersion.toString()
  }.getOrDefault("?")
}

private const val compileSdkVersion = "compileSdkVersion"

/**
 * Get compileSdkVersion of an app
 * @return compileSdkVersion
 */
fun PackageInfo.getCompileSdkVersion(): Int {
  return runCatching {
    if (OsUtils.atLeastS()) {
      applicationInfo!!.compileSdkVersion
    } else {
      val demands = ManifestReader.getManifestProperties(
        File(applicationInfo!!.sourceDir),
        arrayOf(compileSdkVersion)
      )
      demands[compileSdkVersion]?.toString()?.toInt() ?: 0
    }
  }.getOrDefault(0)
}

/**
 * Get compileSdkVersion string of an app
 * @return compileSdkVersion string
 */
fun PackageInfo.getCompileSdkVersionString(): String {
  val version = getCompileSdkVersion()
  return if (version == 0) {
    "?"
  } else {
    version.toString()
  }
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
  val hidden by unsafeLazy {
    val sourceDir = applicationInfo?.sourceDir ?: return@unsafeLazy emptyMap<String, Int>()
    HiddenPermissionsReader.getHiddenPermissions(File(sourceDir)).filter { (_, v) ->
      OsUtils.higherThan(v as Int)
    }
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
    s to ((flags?.get(index) ?: 0) and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0)
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
  return !applicationInfo?.splitSourceDirs.isNullOrEmpty()
}

/**
 * Check if an app uses Kotlin language
 * @return true if it uses Kotlin language
 */
fun PackageInfo.isKotlinUsed(): Boolean {
  return runCatching {
    val file = File(applicationInfo!!.sourceDir)

    ZipFileCompat(file).use {
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
    ZipFileCompat(File(applicationInfo!!.sourceDir)).use { zipFile ->
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
  val sourceDir = applicationInfo?.sourceDir ?: return 0
  var size: Long = FileUtils.getFileSize(sourceDir)

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
  val metaData = applicationInfo?.metaData ?: return false
  return metaData.getBoolean("xposedmodule") || metaData.containsKey("xposedminversion")
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
 * Check if an app is Overlay
 * @return True if is Overlay
 */
fun PackageInfo.isOverlay(): Boolean {
  return try {
    Refine.unsafeCast<PackageInfoHidden>(this).isOverlayPackage
  } catch (t: Throwable) {
    if (applicationInfo?.sourceDir == null) return false
    val demands =
      ManifestReader.getManifestProperties(File(applicationInfo!!.sourceDir), arrayOf("overlay"))
    return demands["overlay"] as? Boolean ?: false
  }
}

/**
 * Get features of an app
 * @return Features
 */
fun PackageInfo.getFeatures(): Int {
  var features = 0
  val sourceDir = applicationInfo?.sourceDir ?: return 0
  val resultList = PackageUtils.findDexClasses(
    File(sourceDir),
    listOf(
      "androidx.compose.*".toClassDefType(),
      "rx.schedulers.*".toClassDefType(),
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
    ZipFileCompat(applicationInfo!!.sourceDir).use { zip ->
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
  val file = File(applicationInfo?.sourceDir ?: return false)
  val usedInMetaInf = runCatching {
    ZipFileCompat(file).use {
      it.getZipEntries().asSequence().any { entry ->
        entry.isDirectory.not() &&
          entry.name.startsWith("androidx.compose") &&
          entry.name.endsWith(".version")
      }
    }
  }.getOrDefault(false)
  if (usedInMetaInf) {
    return true
  }
  if (foundList != null) {
    return foundList.contains("androidx.compose.*".toClassDefType())
  }
  return PackageUtils.findDexClasses(
    file,
    listOf("androidx.compose.*".toClassDefType())
  ).isNotEmpty()
}

/**
 * Get Jetpack Compose version of an app
 * @return Jetpack Compose version or null if not found
 */
fun PackageInfo.getJetpackComposeVersion(): String? {
  runCatching {
    ZipFileCompat(File(applicationInfo!!.sourceDir)).use { zipFile ->
      arrayOf(
        "META-INF/androidx.compose.runtime_runtime.version",
        "META-INF/androidx.compose.ui_ui.version",
        "META-INF/androidx.compose.ui_ui-tooling-preview.version",
        "META-INF/androidx.compose.foundation_foundation.version",
        "META-INF/androidx.compose.animation_animation.version"
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
  val file = File(applicationInfo?.sourceDir ?: return false)
  val usedInMetaInf = runCatching {
    ZipFileCompat(file).use {
      it.getEntry("META-INF/rxjava.properties") != null
    }
  }.getOrDefault(false)
  if (usedInMetaInf) {
    return true
  }
  if (foundList != null) {
    return foundList.contains("rx.schedulers.*".toClassDefType()) ||
      foundList.contains("io.reactivex.*".toClassDefType()) ||
      foundList.contains("io.reactivex.rxjava3.*".toClassDefType())
  }
  return PackageUtils.findDexClasses(
    file,
    listOf(
      "rx.schedulers.*".toClassDefType(),
      "io.reactivex.*".toClassDefType(),
      "io.reactivex.rxjava3.*".toClassDefType()
    ),
    hasAny = true
  ).isNotEmpty()
}

private const val REACTIVEX_KEYWORD = "Implementation-Version"

suspend fun PackageInfo.getRxJavaVersion(): String? = withContext(Dispatchers.IO) {
  runCatching {
    ZipFileCompat(File(applicationInfo!!.sourceDir)).use { zipFile ->
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
      File(applicationInfo!!.sourceDir),
      listOf(
        "rx.schedulers.*".toClassDefType(),
        "io.reactivex.*".toClassDefType(),
        "io.reactivex.rxjava3.*".toClassDefType()
      )
    )
    if (resultList.contains("io.reactivex.rxjava3.*".toClassDefType())) {
      return@withContext RX_MAJOR_THREE
    }
    if (resultList.contains("io.reactivex.*".toClassDefType())) {
      return@withContext RX_MAJOR_TWO
    }
    if (resultList.contains("rx.schedulers.*".toClassDefType())) {
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
  val file = File(applicationInfo?.sourceDir ?: return false)
  val usedInMetaInf = runCatching {
    ZipFileCompat(file).use {
      it.getEntry("META-INF/rxkotlin.properties") != null
    }
  }.getOrDefault(false)
  if (usedInMetaInf) {
    return true
  }
  if (foundList != null) {
    return foundList.contains("io.reactivex.rxjava3.kotlin.*".toClassDefType()) ||
      foundList.contains("io.reactivex.rxkotlin".toClassDefType()) ||
      foundList.contains("rx.lang.kotlin".toClassDefType())
  }
  return PackageUtils.findDexClasses(
    file,
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
    val file = File(applicationInfo!!.sourceDir)
    ZipFileCompat(file).use { zipFile ->
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
      file,
      listOf(
        "io.reactivex.rxjava3.kotlin.*".toClassDefType(),
        "io.reactivex.rxkotlin".toClassDefType(),
        "rx.lang.kotlin".toClassDefType()
      )
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
  if (foundList != null) {
    return foundList.contains("io.reactivex.rxjava3.android.*".toClassDefType()) ||
      foundList.contains("io.reactivex.android.*".toClassDefType()) ||
      foundList.contains("rx.android.*".toClassDefType())
  }
  return PackageUtils.findDexClasses(
    File(applicationInfo?.sourceDir ?: return false),
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
    File(applicationInfo?.sourceDir ?: return@withContext null),
    listOf(
      "io.reactivex.rxjava3.android.*".toClassDefType(),
      "io.reactivex.android.*".toClassDefType(),
      "rx.android.*".toClassDefType()
    )
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
 * @param context Context
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
    if (signingInfo!!.hasMultipleSigners()) {
      signingInfo!!.apkContentsSigners
    } else {
      signingInfo!!.signingCertificateHistory
    }
  } else {
    @Suppress("DEPRECATION")
    signatures
  }.orEmpty().asSequence().map {
    PackageUtils.describeSignature(localedContext, dateFormat, it)
  }
}

fun PackageInfo.getAppName(): String? =
  applicationInfo?.loadLabel(SystemServices.packageManager)?.toString()

val PREINSTALLED_TIMESTAMP by lazy {
  // default is 2009-01-01 08:00:00 GMT+8
  runCatching {
    SystemServices.packageManager.getPackageInfo("android", 0).lastUpdateTime
  }.getOrDefault(1230768000000)
}

fun PackageInfo.isPreinstalled(): Boolean {
  return lastUpdateTime <= PREINSTALLED_TIMESTAMP
}

// Keep in sync with `ABI_TO_INSTRUCTION_SET_MAP` in
// libcore/libart/src/main/java/dalvik/system/VMRuntime.java.
private val ABI_TO_INSTRUCTION_SET_MAP = mapOf(
  "armeabi" to "arm",
  "armeabi-v7a" to "arm",
  "x86" to "x86",
  "x86_64" to "x86_64",
  "arm64-v8a" to "arm64",
  "arm64-v8a-hwasan" to "arm64",
  "riscv64" to "riscv64"
)

val INSTRUCTION_SET_MAP_TO_ABI_VALUE = mapOf(
  "arm64" to ARMV8,
  "arm" to ARMV7,
  "x86_64" to X86_64,
  "x86" to X86,
  "mips64" to MIPS64,
  "mips" to MIPS
)

@Suppress("UNCHECKED_CAST")
@SuppressLint("SoonBlockedPrivateApi")
fun PackageInfo.getDexoptInfo(): Pair<String, String>? {
  val sourceDir = applicationInfo?.sourceDir ?: return null
  val ret: Array<String>? = runCatching {
    val method = DexFile::class.java.getDeclaredMethod(
      "getDexFileOptimizationStatus",
      String::class.java,
      String::class.java
    )
    method.isAccessible = true
    method.invoke(
      null,
      sourceDir,
      ABI_TO_INSTRUCTION_SET_MAP[Build.SUPPORTED_ABIS[0]]
    ) as Array<String>
  }.getOrNull()

  Timber.d("getDexoptInfo: ${ret?.contentToString()}")
  return ret?.takeIf { it.size == 2 }?.let { it[0] to it[1] }
}
