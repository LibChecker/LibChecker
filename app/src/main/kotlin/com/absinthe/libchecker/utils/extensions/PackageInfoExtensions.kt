package com.absinthe.libchecker.utils.extensions

import android.content.pm.PackageInfo
import android.content.pm.PackageInfoHidden
import android.content.pm.PackageManager
import androidx.core.content.pm.PackageInfoCompat
import com.absinthe.libchecker.app.SystemServices
import com.absinthe.libchecker.utils.FileUtils
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.manifest.HiddenPermissionsReader
import com.absinthe.libchecker.utils.manifest.ManifestReader
import dev.rikka.tools.refine.Refine
import java.io.File

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
  return runCatching {
    "${versionName ?: "<unknown>"} (${getVersionCode()})"
  }.getOrDefault("Unknown")
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
  val permissions = requestedPermissions ?: return emptyList()
  val flags = requestedPermissionsFlags
  val hidden by unsafeLazy {
    val sourceDir = applicationInfo?.sourceDir ?: return@unsafeLazy emptyMap<String, Int>()
    HiddenPermissionsReader.getHiddenPermissions(File(sourceDir)).filter { (_, v) ->
      OsUtils.higherThan(v as Int)
    }
  }

  val hasMatchingFlags = flags != null && flags.size == permissions.size
  return buildList(permissions.size + 4) {
    for (i in permissions.indices) {
      val perm = permissions[i] ?: continue
      val isGranted = if (hasMatchingFlags) {
        (flags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
      } else {
        true
      }
      add(perm to isGranted)
    }
    if (hidden.isNotEmpty()) {
      hidden.forEach { (p, v) ->
        add("$p (maxSdkVersion: $v)" to false)
      }
    }
  }
}

/**
 * Check if an app uses split apks
 * @return true if it uses split apks
 */
fun PackageInfo.isSplitsApk(): Boolean {
  return !applicationInfo?.splitSourceDirs.isNullOrEmpty()
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
 * Check if an app is Overlay
 * @return True if is Overlay
 */
fun PackageInfo.isOverlay(): Boolean {
  return try {
    Refine.unsafeCast<PackageInfoHidden>(this).isOverlayPackage
  } catch (_: Throwable) {
    if (applicationInfo?.sourceDir == null) return false
    val demands =
      ManifestReader.getManifestProperties(File(applicationInfo!!.sourceDir), arrayOf("overlay"))
    return demands["overlay"] as? Boolean == true
  }
}

fun PackageInfo.getAppName(packageManager: PackageManager): String? = applicationInfo?.loadLabel(packageManager)?.toString()

val PREINSTALLED_TIMESTAMP by lazy {
  // default is 2009-01-01 08:00:00 GMT+8
  runCatching {
    SystemServices.packageManager.getPackageInfo("android", 0).lastUpdateTime
  }.getOrDefault(1230768000000)
}

fun PackageInfo.isPreinstalled(): Boolean {
  return lastUpdateTime <= PREINSTALLED_TIMESTAMP
}

fun PackageInfo.isArchivedPackage(): Boolean {
  return OsUtils.atLeastV() && archiveTimeMillis > 0
}
