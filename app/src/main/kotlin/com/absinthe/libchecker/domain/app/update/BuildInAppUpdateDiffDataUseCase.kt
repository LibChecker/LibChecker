package com.absinthe.libchecker.domain.app.update

import android.content.pm.PackageManager
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.api.bean.GetAppUpdateInfo
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.getCompileSdkVersion
import com.absinthe.libchecker.utils.extensions.getPackageSize
import com.absinthe.libchecker.utils.extensions.getVersionCode

class BuildInAppUpdateDiffDataUseCase(
  private val packageName: String,
  private val packageManager: PackageManager,
  private val installedAppRepository: InstalledAppRepository
) {

  operator fun invoke(updateInfo: GetAppUpdateInfo?): InAppUpdateDiffData? {
    val packageInfo = installedAppRepository.getPackageInfo(packageName) ?: return null
    val appInfo = packageInfo.applicationInfo ?: return null
    val localVersionName = packageInfo.versionName.toString()
    val localVersionCode = packageInfo.getVersionCode()
    val localCompileSdk = packageInfo.getCompileSdkVersion().toShort()
    val localPackageSize = packageInfo.getPackageSize(includeSplits = false)
    val (displayedRemoteApp, hasUpdate) = resolveInAppUpdateDisplay(
      remoteApp = updateInfo?.appForFlavor(BuildConfig.IS_FOSS),
      localVersionCode = localVersionCode
    )

    return InAppUpdateDiffData(
      hasUpdate = hasUpdate,
      item = SnapshotDiffItem(
        packageName = packageInfo.packageName,
        updateTime = System.currentTimeMillis(),
        labelDiff = SnapshotDiffItem.DiffNode(appInfo.loadLabel(packageManager).toString()),
        versionNameDiff = SnapshotDiffItem.DiffNode(
          localVersionName,
          displayedRemoteApp?.version ?: localVersionName
        ),
        versionCodeDiff = SnapshotDiffItem.DiffNode(
          localVersionCode,
          displayedRemoteApp?.versionCode?.toLong() ?: localVersionCode
        ),
        abiDiff = SnapshotDiffItem.DiffNode(PackageUtils.getAbi(packageInfo).toShort()),
        targetApiDiff = SnapshotDiffItem.DiffNode(
          appInfo.targetSdkVersion.toShort(),
          displayedRemoteApp?.extra?.target?.toShort() ?: appInfo.targetSdkVersion.toShort()
        ),
        compileSdkDiff = SnapshotDiffItem.DiffNode(
          localCompileSdk,
          displayedRemoteApp?.extra?.compile?.toShort() ?: localCompileSdk
        ),
        minSdkDiff = SnapshotDiffItem.DiffNode(
          appInfo.minSdkVersion.toShort(),
          displayedRemoteApp?.extra?.min?.toShort() ?: appInfo.minSdkVersion.toShort()
        ),
        packageSizeDiff = SnapshotDiffItem.DiffNode(
          localPackageSize,
          displayedRemoteApp?.extra?.packageSize?.toLong() ?: localPackageSize
        ),
        nativeLibsDiff = SnapshotDiffItem.DiffNode(""),
        servicesDiff = SnapshotDiffItem.DiffNode(""),
        activitiesDiff = SnapshotDiffItem.DiffNode(""),
        receiversDiff = SnapshotDiffItem.DiffNode(""),
        providersDiff = SnapshotDiffItem.DiffNode(""),
        permissionsDiff = SnapshotDiffItem.DiffNode(""),
        metadataDiff = SnapshotDiffItem.DiffNode("")
      )
    )
  }
}

internal fun resolveInAppUpdateDisplay(
  remoteApp: GetAppUpdateInfo.App?,
  localVersionCode: Long
): Pair<GetAppUpdateInfo.App?, Boolean> {
  return remoteApp to (remoteApp?.versionCode?.toLong()?.let { it > localVersionCode } == true)
}

data class InAppUpdateDiffData(
  val hasUpdate: Boolean,
  val item: SnapshotDiffItem
)
