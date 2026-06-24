package com.absinthe.libchecker.domain.app

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageItemInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.core.net.toUri
import com.absinthe.libchecker.compat.PackageManagerCompat
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetAppInfoActionsUseCase(
  private val ownPackageName: String,
  private val packageManager: PackageManager,
  private val installedAppRepository: InstalledAppRepository,
  private val allowFileUriExposure: AllowFileUriExposureUseCase
) {

  suspend operator fun invoke(packageName: String): List<AppInfoActionItem> = withContext(Dispatchers.IO) {
    listOf(
      getShowAppInfoList(packageName),
      getShowAppSourceList(packageName),
      getShowMarketList(packageName)
    )
      .flatten()
      .distinctBy { it.packageName }
  }

  private fun getShowAppInfoList(packageName: String): List<AppInfoActionItem> {
    return PackageManagerCompat.queryIntentActivities(
      Intent(Intent.ACTION_SHOW_APP_INFO),
      PackageManager.MATCH_DEFAULT_ONLY
    ).filter { it.activityInfo.packageName != ownPackageName }
      .map {
        toActionItem(
          packageItemInfo = it.activityInfo,
          intent = Intent(Intent.ACTION_SHOW_APP_INFO)
            .setComponent(ComponentName(it.activityInfo.packageName, it.activityInfo.name))
            .putExtra(Intent.EXTRA_PACKAGE_NAME, packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
      }
  }

  private fun getShowAppSourceList(packageName: String): List<AppInfoActionItem> {
    val source = installedAppRepository.getPackageInfo(packageName)
      ?.applicationInfo
      ?.sourceDir
      ?: return emptyList()
    allowFileUriExposure()
    val sourcePath = runCatching { File(source) }.getOrNull() ?: return emptyList()

    return PackageManagerCompat.queryIntentActivities(
      Intent(Intent.ACTION_VIEW).also {
        it.setDataAndType(sourcePath.toUri(), MIMETYPE_APK)
      },
      PackageManager.MATCH_DEFAULT_ONLY
    )
      .filter { isFileManager(it.activityInfo.packageName) }
      .map {
        toActionItem(
          packageItemInfo = it.activityInfo,
          intent = Intent(Intent.ACTION_VIEW)
            .setPackage(it.activityInfo.packageName)
            .setDataAndType(sourcePath.toUri(), MIMETYPE_APK)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
      }
  }

  private fun getShowMarketList(packageName: String): List<AppInfoActionItem> {
    return PackageManagerCompat.queryIntentActivities(
      Intent(Intent.ACTION_VIEW).also {
        it.data = "market://details?id=$packageName".toUri()
      },
      PackageManager.MATCH_DEFAULT_ONLY
    ).filter { it.activityInfo.packageName != ownPackageName }
      .map {
        toActionItem(
          packageItemInfo = it.activityInfo,
          intent = Intent(Intent.ACTION_VIEW)
            .setData("market://details?id=$packageName".toUri())
            .setComponent(ComponentName(it.activityInfo.packageName, it.activityInfo.name))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
      }
  }

  private fun isFileManager(packageName: String): Boolean {
    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
      setDataAndType("file:///".toUri(), "*/*")
      setPackage(packageName)
    }
    val canHandleFiles = PackageManagerCompat.queryIntentActivities(
      viewIntent,
      PackageManager.MATCH_DEFAULT_ONLY
    ).any { it.activityInfo.packageName == packageName }

    val permissions = installedAppRepository.getPermissions(packageName)
    val hasStoragePermission = permissions.any {
      it == "android.permission.MANAGE_EXTERNAL_STORAGE" ||
        it == Manifest.permission.READ_EXTERNAL_STORAGE ||
        it == Manifest.permission.WRITE_EXTERNAL_STORAGE
    }

    val getContentIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
      type = "*/*"
      setPackage(packageName)
    }
    val canPickFiles = PackageManagerCompat.queryIntentActivities(
      getContentIntent,
      PackageManager.MATCH_DEFAULT_ONLY
    ).any { it.activityInfo.packageName == packageName }

    val openTreeIntent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
      setPackage(packageName)
    }
    val canManageDirectories = PackageManagerCompat.queryIntentActivities(
      openTreeIntent,
      PackageManager.MATCH_DEFAULT_ONLY
    ).any { it.activityInfo.packageName == packageName }

    return (canHandleFiles || canPickFiles || canManageDirectories) && hasStoragePermission
  }

  private fun toActionItem(packageItemInfo: PackageItemInfo, intent: Intent): AppInfoActionItem {
    return AppInfoActionItem(
      packageName = packageItemInfo.packageName,
      label = packageItemInfo.loadLabel(packageManager),
      icon = getAppIcon(packageItemInfo.packageName),
      intent = intent
    )
  }

  private fun getAppIcon(packageName: String): Drawable? {
    return runCatching {
      installedAppRepository.getPackageInfo(packageName)
        ?.applicationInfo
        ?.loadIcon(packageManager)
    }.getOrNull()
  }

  private companion object {
    const val MIMETYPE_APK = "application/vnd.android.package-archive"
  }
}
