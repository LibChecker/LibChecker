package com.absinthe.libchecker.domain.snapshot.comparison.archive

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.compat.PackageManagerCompat
import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.apk.APKSParser
import com.absinthe.libchecker.utils.extensions.getCompileSdkVersion
import com.absinthe.libchecker.utils.extensions.getPackageSize
import com.absinthe.libchecker.utils.extensions.getPermissionsList
import com.absinthe.libchecker.utils.extensions.getVersionCode
import com.absinthe.libchecker.utils.toJson
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.zhanghai.android.appiconloader.AppIconLoader
import okio.buffer
import okio.sink
import okio.source

class BuildArchiveSnapshotItemUseCase(
  private val context: Context
) {

  suspend operator fun invoke(
    uri: Uri,
    destinationFile: File,
    iconSize: Int
  ): ArchiveSnapshotItem = withContext(Dispatchers.IO) {
    var packageInfo = context.contentResolver.openInputStream(uri)?.use { inputStream ->
      val fileSize = inputStream.available()
      val freeSize = Environment.getExternalStorageDirectory().freeSpace

      if (freeSize <= fileSize * 1.5) {
        throw NotEnoughStorageSpaceException()
      }

      destinationFile.sink().buffer().use { sink ->
        inputStream.source().buffer().use {
          sink.writeAll(it)
        }
      }

      PackageManagerCompat.getPackageArchiveInfo(destinationFile.path, ARCHIVE_PACKAGE_FLAGS)
        ?: APKSParser(destinationFile, ARCHIVE_PACKAGE_FLAGS).getPackageInfo()
    } ?: throw IllegalStateException("InputStream is null")

    packageInfo = packageInfo.apply {
      applicationInfo?.sourceDir = destinationFile.path
      applicationInfo?.publicSourceDir = destinationFile.path
    }

    val applicationInfo = packageInfo.applicationInfo ?: throw IllegalStateException("ApplicationInfo is null")
    val appIconLoader = AppIconLoader(iconSize, false, context)
    ArchiveSnapshotItem(
      snapshotItem = packageInfo.toSnapshotItem(applicationInfo),
      icon = appIconLoader.loadIcon(applicationInfo)
    )
  }

  private fun android.content.pm.PackageInfo.toSnapshotItem(
    applicationInfo: ApplicationInfo
  ): SnapshotItem {
    return SnapshotItem(
      id = null,
      packageName = packageName,
      timeStamp = -1L,
      label = context.packageManager.getApplicationLabel(applicationInfo).toString(),
      versionName = versionName.toString(),
      versionCode = getVersionCode(),
      installedTime = firstInstallTime,
      lastUpdatedTime = lastUpdateTime,
      isSystem = (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) > 0,
      abi = PackageUtils.getAbi(this).toShort(),
      targetApi = applicationInfo.targetSdkVersion.toShort(),
      nativeLibs = PackageUtils.getNativeDirLibs(this).toJson().orEmpty(),
      services = PackageUtils.getComponentStringList(this, SERVICE, false)
        .toJson().orEmpty(),
      activities = PackageUtils.getComponentStringList(this, ACTIVITY, false)
        .toJson().orEmpty(),
      receivers = PackageUtils.getComponentStringList(this, RECEIVER, false)
        .toJson().orEmpty(),
      providers = PackageUtils.getComponentStringList(this, PROVIDER, false)
        .toJson().orEmpty(),
      permissions = getPermissionsList().toJson().orEmpty(),
      metadata = PackageUtils.getMetaDataItems(this).toJson().orEmpty(),
      packageSize = getPackageSize(true),
      compileSdk = getCompileSdkVersion().toShort(),
      minSdk = applicationInfo.minSdkVersion.toShort()
    )
  }

  class NotEnoughStorageSpaceException : IllegalStateException("Not enough storage space")

  companion object {
    private const val ARCHIVE_PACKAGE_FLAGS =
      PackageManager.GET_SERVICES or
        PackageManager.GET_ACTIVITIES or
        PackageManager.GET_RECEIVERS or
        PackageManager.GET_PROVIDERS or
        PackageManager.GET_PERMISSIONS or
        PackageManager.GET_META_DATA or
        PackageManager.MATCH_DISABLED_COMPONENTS or
        PackageManager.MATCH_UNINSTALLED_PACKAGES
  }
}

data class ArchiveSnapshotItem(
  val snapshotItem: SnapshotItem,
  val icon: Bitmap
)
