package com.absinthe.libchecker.domain.snapshot.comparison.usecase

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.getAppName
import com.absinthe.libchecker.utils.extensions.getCompileSdkVersion
import com.absinthe.libchecker.utils.extensions.getPackageSize
import com.absinthe.libchecker.utils.extensions.getPermissionsList
import com.absinthe.libchecker.utils.extensions.getVersionCode
import com.absinthe.libchecker.utils.extensions.isArchivedPackage
import com.absinthe.libchecker.utils.toJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BuildPackageComparisonSnapshotItemUseCase(
  private val packageManager: PackageManager
) {

  suspend operator fun invoke(
    basePackage: PackageInfo,
    analysisPackage: PackageInfo
  ): SnapshotDiffItem = withContext(Dispatchers.IO) {
    SnapshotDiffItem(
      packageName = basePackage.packageName,
      updateTime = basePackage.lastUpdateTime,
      labelDiff = SnapshotDiffItem.DiffNode(
        basePackage.getAppName(packageManager).toString(),
        analysisPackage.getAppName(packageManager).toString()
      ),
      versionNameDiff = SnapshotDiffItem.DiffNode(
        basePackage.versionName.orEmpty(),
        analysisPackage.versionName.orEmpty()
      ),
      versionCodeDiff = SnapshotDiffItem.DiffNode(
        basePackage.getVersionCode(),
        analysisPackage.getVersionCode()
      ),
      abiDiff = SnapshotDiffItem.DiffNode(
        PackageUtils.getAbi(basePackage).toShort(),
        PackageUtils.getAbi(analysisPackage).toShort()
      ),
      targetApiDiff = SnapshotDiffItem.DiffNode(
        basePackage.applicationInfo?.targetSdkVersion?.toShort() ?: 0,
        analysisPackage.applicationInfo?.targetSdkVersion?.toShort()
      ),
      compileSdkDiff = SnapshotDiffItem.DiffNode(
        basePackage.getCompileSdkVersion().toShort(),
        analysisPackage.getCompileSdkVersion().toShort()
      ),
      minSdkDiff = SnapshotDiffItem.DiffNode(
        basePackage.applicationInfo?.minSdkVersion?.toShort() ?: 0,
        analysisPackage.applicationInfo?.minSdkVersion?.toShort()
      ),
      nativeLibsDiff = SnapshotDiffItem.DiffNode(
        PackageUtils.getNativeDirLibs(basePackage).toJson().orEmpty(),
        PackageUtils.getNativeDirLibs(analysisPackage).toJson().orEmpty()
      ),
      servicesDiff = SnapshotDiffItem.DiffNode(
        basePackage.getComponentJson(SERVICE),
        analysisPackage.getComponentJson(SERVICE)
      ),
      activitiesDiff = SnapshotDiffItem.DiffNode(
        basePackage.getComponentJson(ACTIVITY),
        analysisPackage.getComponentJson(ACTIVITY)
      ),
      receiversDiff = SnapshotDiffItem.DiffNode(
        basePackage.getComponentJson(RECEIVER),
        analysisPackage.getComponentJson(RECEIVER)
      ),
      providersDiff = SnapshotDiffItem.DiffNode(
        basePackage.getComponentJson(PROVIDER),
        analysisPackage.getComponentJson(PROVIDER)
      ),
      permissionsDiff = SnapshotDiffItem.DiffNode(
        basePackage.getPermissionsList().toJson().orEmpty(),
        analysisPackage.getPermissionsList().toJson().orEmpty()
      ),
      metadataDiff = SnapshotDiffItem.DiffNode(
        PackageUtils.getMetaDataItems(basePackage).toJson().orEmpty(),
        PackageUtils.getMetaDataItems(analysisPackage).toJson().orEmpty()
      ),
      packageSizeDiff = SnapshotDiffItem.DiffNode(
        basePackage.getPackageSize(true),
        analysisPackage.getPackageSize(true)
      ),
      archivedDiff = SnapshotDiffItem.DiffNode(
        basePackage.isArchivedPackage(),
        analysisPackage.isArchivedPackage()
      )
    )
  }

  private fun PackageInfo.getComponentJson(@LibType type: Int): String {
    return PackageUtils.getComponentStringList(this, type, false).toJson().orEmpty()
  }
}
