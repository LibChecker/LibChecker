package com.absinthe.libchecker.compat

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import com.absinthe.libchecker.app.SystemServices
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.ShizukuUtils
import timber.log.Timber

object PackageManagerCompat {
  fun getPackageInfo(packageName: String, flags: Int): PackageInfo {
    return if (OsUtils.atLeastT()) {
      SystemServices.packageManager.getPackageInfo(
        packageName,
        PackageManager.PackageInfoFlags.of(flags.toLong())
      )
    } else {
      SystemServices.packageManager.getPackageInfo(packageName, flags)
    }
  }

  fun getPackageArchiveInfo(archiveFilePath: String, flags: Int): PackageInfo? {
    return runCatching {
      if (OsUtils.atLeastT()) {
        SystemServices.packageManager.getPackageArchiveInfo(
          archiveFilePath,
          PackageManager.PackageInfoFlags.of(flags.toLong())
        )
      } else {
        SystemServices.packageManager.getPackageArchiveInfo(archiveFilePath, flags)
      }
    }.getOrNull()
  }

  fun getApplicationInfo(packageName: String, flags: Int): ApplicationInfo {
    return if (OsUtils.atLeastT()) {
      SystemServices.packageManager.getApplicationInfo(
        packageName,
        PackageManager.ApplicationInfoFlags.of(flags.toLong())
      )
    } else {
      SystemServices.packageManager.getApplicationInfo(packageName, flags)
    }
  }

  fun getInstalledPackages(flags: Int): List<PackageInfo> {
    return if (OsUtils.atLeastT()) {
      SystemServices.packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong()))
    } else {
      SystemServices.packageManager.getInstalledPackages(flags)
    }
  }

  fun getInstalledPackages(flags: Int, userId: Int): List<PackageInfo> {
    if (ShizukuUtils.checkShizukuStatus() != ShizukuUtils.Status.SUCCESS) {
      Timber.w("Shizuku unavailable")
      return listOf()
    }
    return if (OsUtils.atLeastT()) {
      SystemServices.iPackageManager.getInstalledPackages(flags.toLong(), userId)
    } else {
      SystemServices.iPackageManager.getInstalledPackages(flags, userId)
    }.list
  }

  fun queryIntentActivities(intent: Intent, flags: Int): List<ResolveInfo> {
    return if (OsUtils.atLeastT()) {
      SystemServices.packageManager.queryIntentActivities(
        intent,
        PackageManager.ResolveInfoFlags.of(flags.toLong())
      )
    } else {
      SystemServices.packageManager.queryIntentActivities(intent, flags)
    }
  }
}
