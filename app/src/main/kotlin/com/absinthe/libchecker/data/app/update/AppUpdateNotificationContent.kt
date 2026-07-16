package com.absinthe.libchecker.data.app.update

import android.content.Context
import com.absinthe.libchecker.compat.PackageManagerCompat
import com.absinthe.libchecker.utils.extensions.getVersionCode

internal data class AppUpdateNotificationVersionInfo(
  val versionName: String,
  val versionCode: Long
)

internal fun Context.getAppUpdateNotificationVersionInfo(): AppUpdateNotificationVersionInfo? {
  return runCatching {
    val packageInfo = PackageManagerCompat.getPackageInfo(packageName, 0)
    appUpdateNotificationVersionInfo(
      versionName = packageInfo.versionName,
      versionCode = packageInfo.getVersionCode()
    )
  }.getOrNull()
}

internal fun appUpdateNotificationVersionInfo(
  versionName: String?,
  versionCode: Long
): AppUpdateNotificationVersionInfo? {
  val normalizedVersionName = versionName?.trim()?.takeIf(String::isNotEmpty) ?: return null
  if (versionCode < 0) {
    return null
  }
  return AppUpdateNotificationVersionInfo(
    versionName = normalizedVersionName,
    versionCode = versionCode
  )
}

internal fun buildAppUpdateNotificationContent(
  previous: AppUpdateNotificationVersionInfo?,
  current: AppUpdateNotificationVersionInfo?
): String? {
  if (previous == null || current == null) {
    return null
  }
  return "${previous.format()} → ${current.format()}"
}

private fun AppUpdateNotificationVersionInfo.format(): String {
  return "$versionName ($versionCode)"
}
