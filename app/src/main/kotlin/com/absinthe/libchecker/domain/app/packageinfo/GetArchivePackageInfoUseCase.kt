package com.absinthe.libchecker.domain.app.packageinfo

import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageInfoHidden
import android.content.pm.PackageManager
import android.content.pm.ProviderInfo
import android.content.pm.ServiceInfo
import android.os.Build
import com.absinthe.libchecker.compat.PackageManagerCompat
import com.absinthe.libchecker.utils.apk.APKSParser
import com.absinthe.libchecker.utils.apk.XAPKParser
import com.absinthe.libchecker.utils.manifest.FullManifestReader
import dev.rikka.tools.refine.Refine
import java.io.File
import timber.log.Timber

class GetArchivePackageInfoUseCase {

  operator fun invoke(file: File): PackageInfo? {
    return getPackageArchiveInfo(file)
      ?: getOverlayPackageInfo(file)
      ?: getApksPackageInfo(file)
      ?: getXapkPackageInfo(file)
  }

  private fun getPackageArchiveInfo(file: File): PackageInfo? {
    return PackageManagerCompat.getPackageArchiveInfo(file.path, PACKAGE_INFO_FLAGS)
      ?.also { packageInfo ->
        packageInfo.applicationInfo?.let { applicationInfo ->
          applicationInfo.sourceDir = file.path
          applicationInfo.publicSourceDir = file.path
        }
      }
      ?: run {
        Timber.w("Failed to get PackageArchiveInfo")
        null
      }
  }

  @Suppress("DEPRECATION")
  private fun getOverlayPackageInfo(file: File): PackageInfo? {
    val manifestReader = runCatching {
      FullManifestReader(file, null)
    }.onFailure {
      Timber.w(it, "Failed to read archive manifest")
    }.getOrNull() ?: return null
    val properties = manifestReader.properties
    if (properties["overlay"] as? Boolean != true) {
      return null
    }
    val packageName = properties["package"].asString() ?: return null
    val overlayTarget = properties["targetPackage"].asString()

    return PackageInfo().apply {
      this.packageName = packageName
      versionName = properties["versionName"].asString()
      versionCode = properties["versionCode"].asLong()?.toInt() ?: 0
      sharedUserId = properties["sharedUserId"].asString()
      applicationInfo = buildApplicationInfo(file, packageName, properties)
      requestedPermissions = manifestReader.permissionList.toTypedArray()
      requestedPermissionsFlags = IntArray(manifestReader.permissionList.size)
      services = manifestReader.services.toServiceInfoArray(packageName)
      activities = manifestReader.activities.toActivityInfoArray(packageName)
      receivers = manifestReader.receivers.toActivityInfoArray(packageName)
      providers = manifestReader.providers.toProviderInfoArray(packageName)
      runCatching {
        Refine.unsafeCast<PackageInfoHidden>(this).overlayTarget = overlayTarget
      }.onFailure {
        Timber.w(it, "Failed to mark archive as overlay")
      }
      Timber.w("Using manifest parsed overlay PackageInfo for $packageName")
    }
  }

  private fun getApksPackageInfo(file: File): PackageInfo? {
    val packageInfo = APKSParser(file, PACKAGE_INFO_FLAGS).getPackageInfo()
    if (packageInfo == null) {
      Timber.w("Not APKS file")
    }
    return packageInfo
  }

  private fun getXapkPackageInfo(file: File): PackageInfo? {
    return XAPKParser(file, PACKAGE_INFO_FLAGS).getPackageInfo()
      ?: run {
        Timber.w("Not XAPK file")
        null
      }
  }

  private companion object {
    private val PACKAGE_INFO_FLAGS = PackageManager.GET_SERVICES or
      PackageManager.GET_ACTIVITIES or
      PackageManager.GET_RECEIVERS or
      PackageManager.GET_PROVIDERS or
      PackageManager.GET_PERMISSIONS or
      PackageManager.GET_META_DATA or
      PackageManager.MATCH_DISABLED_COMPONENTS or
      PackageManager.MATCH_UNINSTALLED_PACKAGES
  }
}

private fun buildApplicationInfo(
  file: File,
  packageName: String,
  properties: Map<String, Any?>
): ApplicationInfo {
  return ApplicationInfo().apply {
    this.packageName = packageName
    name = properties["name"].asString()?.toPackageClassName(packageName)
    className = name
    sourceDir = file.path
    publicSourceDir = file.path
    minSdkVersion = properties["minSdkVersion"].asInt() ?: 0
    targetSdkVersion = properties["targetSdkVersion"].asInt() ?: 0
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      compileSdkVersion = properties["compileSdkVersion"].asInt() ?: 0
    }
    properties["label"].asInt()?.let { labelRes = it }
    properties["label"].asString()?.let { nonLocalizedLabel = it }
    properties["icon"].asInt()?.let { icon = it }
    properties["theme"].asInt()?.let { theme = it }
  }
}

private fun List<String>.toActivityInfoArray(packageName: String): Array<ActivityInfo> {
  return map { componentName ->
    ActivityInfo().apply {
      this.packageName = packageName
      name = componentName.toPackageClassName(packageName)
    }
  }.toTypedArray()
}

private fun List<String>.toServiceInfoArray(packageName: String): Array<ServiceInfo> {
  return map { componentName ->
    ServiceInfo().apply {
      this.packageName = packageName
      name = componentName.toPackageClassName(packageName)
    }
  }.toTypedArray()
}

private fun List<String>.toProviderInfoArray(packageName: String): Array<ProviderInfo> {
  return map { componentName ->
    ProviderInfo().apply {
      this.packageName = packageName
      name = componentName.toPackageClassName(packageName)
    }
  }.toTypedArray()
}

private fun String.toPackageClassName(packageName: String): String {
  return when {
    startsWith(".") -> packageName + this
    "." in this -> this
    else -> "$packageName.$this"
  }
}

private fun Any?.asString(): String? {
  return this as? String
}

private fun Any?.asInt(): Int? {
  return when (this) {
    is Int -> this
    is Long -> toInt()
    is String -> toIntOrNull()
    else -> null
  }
}

private fun Any?.asLong(): Long? {
  return when (this) {
    is Int -> toLong()
    is Long -> this
    is String -> toLongOrNull()
    else -> null
  }
}
