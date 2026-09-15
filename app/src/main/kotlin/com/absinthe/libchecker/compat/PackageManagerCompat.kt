package com.absinthe.libchecker.compat

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import com.absinthe.libchecker.app.SystemServices
import com.absinthe.libchecker.utils.OsUtils
import kotlinx.coroutines.CancellationException
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

  fun getInstalledPackages(flags: Long): List<PackageInfo> {
    return try {
      if (OsUtils.atLeastT()) {
        SystemServices.packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags))
      } else {
        SystemServices.packageManager.getInstalledPackages(flags.toInt())
      }
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      Timber.w(e, "Batch package lookup failed; reading packages individually")
      val applications = if (OsUtils.atLeastT()) {
        SystemServices.packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(flags))
      } else {
        SystemServices.packageManager.getInstalledApplications(flags.toInt())
      }
      // Abort on any failed lookup: a partial list can delete valid app records.
      applications.map { app ->
        if (OsUtils.atLeastT()) {
          SystemServices.packageManager.getPackageInfo(app.packageName, PackageManager.PackageInfoFlags.of(flags))
        } else {
          SystemServices.packageManager.getPackageInfo(app.packageName, flags.toInt())
        }
      }
    }
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
