package com.absinthe.libchecker.features.applist.detail

import android.content.pm.PackageInfo
import android.net.Uri
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.AppBundleSplitItem
import com.absinthe.libchecker.domain.app.GetAppBundleItemsUseCase
import com.absinthe.libchecker.domain.app.detail.GetRelatedAppDisplayDataUseCase
import com.absinthe.libchecker.domain.app.detail.action.AppManifestProperty
import com.absinthe.libchecker.domain.app.detail.action.AppPackageShareFile
import com.absinthe.libchecker.domain.app.detail.action.BuildDetailItemDialogRequestUseCase
import com.absinthe.libchecker.domain.app.detail.action.BuildDetailItemLongClickActionsUseCase
import com.absinthe.libchecker.domain.app.detail.action.BuildSignatureDetailItemsUseCase
import com.absinthe.libchecker.domain.app.detail.action.DetailItemLongClickActionRequest
import com.absinthe.libchecker.domain.app.detail.action.ExportAppPackageShareFileUseCase
import com.absinthe.libchecker.domain.app.detail.action.ExtractNativeLibraryUseCase
import com.absinthe.libchecker.domain.app.detail.action.GetAlternativeLaunchItemsUseCase
import com.absinthe.libchecker.domain.app.detail.action.GetAppInfoActionsUseCase
import com.absinthe.libchecker.domain.app.detail.action.GetAppInfoPrimaryActionsUseCase
import com.absinthe.libchecker.domain.app.detail.action.GetAppInstallSourceDetailsUseCase
import com.absinthe.libchecker.domain.app.detail.action.GetAppLaunchActionUseCase
import com.absinthe.libchecker.domain.app.detail.action.GetAppManifestPropertiesUseCase
import com.absinthe.libchecker.domain.app.detail.action.GetElfDetailUseCase
import com.absinthe.libchecker.domain.app.detail.action.GetLibraryDetailDialogDataUseCase
import com.absinthe.libchecker.domain.app.detail.action.GetOverlayDetailUseCase
import com.absinthe.libchecker.domain.app.detail.action.GetPermissionDetailUseCase
import com.absinthe.libchecker.domain.app.detail.action.GetXposedModuleInfoUseCase
import com.absinthe.libchecker.domain.app.detail.action.PrepareAppPackageShareActionUseCase
import com.absinthe.libchecker.domain.app.detail.model.LibStringItem
import com.absinthe.libchecker.domain.app.detail.model.LibStringItemChip
import com.absinthe.libchecker.domain.app.detail.navigation.BuildDetailReferenceNavigationUseCase
import com.absinthe.libchecker.domain.app.detail.navigation.DetailReferenceNavigationRequest
import java.io.File

class DetailActionLoader(
  private val getAlternativeLaunchItemsUseCase: GetAlternativeLaunchItemsUseCase,
  private val getAppBundleItemsUseCase: GetAppBundleItemsUseCase,
  private val getAppInfoActionsUseCase: GetAppInfoActionsUseCase,
  private val getAppInfoPrimaryActionsUseCase: GetAppInfoPrimaryActionsUseCase,
  private val getAppInstallSourceDetailsUseCase: GetAppInstallSourceDetailsUseCase,
  private val getAppLaunchActionUseCase: GetAppLaunchActionUseCase,
  private val getAppManifestPropertiesUseCase: GetAppManifestPropertiesUseCase,
  private val getElfDetailUseCase: GetElfDetailUseCase,
  private val getLibraryDetailDialogDataUseCase: GetLibraryDetailDialogDataUseCase,
  private val getOverlayDetailUseCase: GetOverlayDetailUseCase,
  private val getPermissionDetailUseCase: GetPermissionDetailUseCase,
  private val getRelatedAppDisplayDataUseCase: GetRelatedAppDisplayDataUseCase,
  private val getXposedModuleInfoUseCase: GetXposedModuleInfoUseCase,
  private val buildDetailItemDialogRequestUseCase: BuildDetailItemDialogRequestUseCase,
  private val buildDetailItemLongClickActionsUseCase: BuildDetailItemLongClickActionsUseCase,
  private val buildSignatureDetailItemsUseCase: BuildSignatureDetailItemsUseCase,
  private val extractNativeLibraryUseCase: ExtractNativeLibraryUseCase,
  private val prepareAppPackageShareActionUseCase: PrepareAppPackageShareActionUseCase,
  private val exportAppPackageShareFileUseCase: ExportAppPackageShareFileUseCase,
  private val buildDetailReferenceNavigationUseCase: BuildDetailReferenceNavigationUseCase
) {
  suspend fun getAppBundleItems(packageInfo: PackageInfo): List<AppBundleSplitItem> {
    return getAppBundleItemsUseCase(packageInfo)
  }

  suspend fun getAppInfoActions(packageName: String) = getAppInfoActionsUseCase(packageName)

  suspend fun getAppInfoPrimaryActions(packageName: String?) = getAppInfoPrimaryActionsUseCase(packageName)

  suspend fun getAppLaunchAction(packageName: String?) = getAppLaunchActionUseCase(packageName)

  suspend fun getAlternativeLaunchItems(packageName: String) = getAlternativeLaunchItemsUseCase(packageName)

  suspend fun getAppInstallSourceDetails(packageName: String) = getAppInstallSourceDetailsUseCase(packageName)

  suspend fun getXposedModuleInfo(packageName: String) = getXposedModuleInfoUseCase(packageName)

  suspend fun extractNativeLibrary(
    packageState: DetailPackageState,
    item: LibStringItem
  ) = extractNativeLibraryUseCase(
    packageState.packageInfo,
    item,
    packageState.isApkPreview
  )

  suspend fun prepareAppPackageShareAction(
    cacheDir: File,
    packageName: String
  ) = prepareAppPackageShareActionUseCase(cacheDir, packageName)

  suspend fun exportAppPackageShareFile(
    shareFile: AppPackageShareFile,
    destinationUri: Uri
  ) = exportAppPackageShareFileUseCase(shareFile, destinationUri)

  suspend fun getAppManifestProperties(
    packageInfo: PackageInfo?,
    properties: Map<String, String>?
  ): List<AppManifestProperty> {
    return getAppManifestPropertiesUseCase(packageInfo, properties)
  }

  suspend fun getElfDetail(packageName: String, elfPath: String) = getElfDetailUseCase(packageName, elfPath)

  suspend fun getLibraryDetailDialogHeader(
    libName: String,
    @LibType type: Int,
    isValidLib: Boolean
  ) = getLibraryDetailDialogDataUseCase.getHeader(
    GetLibraryDetailDialogDataUseCase.HeaderRequest(
      libName = libName,
      type = type,
      isValidLib = isValidLib
    )
  )

  suspend fun getLibraryDetailDialogData(
    libName: String,
    @LibType type: Int,
    regexName: String?,
    isValidLib: Boolean
  ) = getLibraryDetailDialogDataUseCase(
    GetLibraryDetailDialogDataUseCase.Request(
      libName = libName,
      type = type,
      regexName = regexName,
      isValidLib = isValidLib
    )
  )

  suspend fun getOverlayDetail(item: LCItem) = getOverlayDetailUseCase(item)

  suspend fun getPermissionDetail(permissionName: String) = getPermissionDetailUseCase(permissionName)

  suspend fun getRelatedAppDisplayData(packageName: String) = getRelatedAppDisplayDataUseCase(packageName)

  fun buildDetailItemDialogRequest(
    item: LibStringItemChip,
    @LibType detailType: Int
  ) = buildDetailItemDialogRequestUseCase(item, detailType)

  fun buildDetailItemLongClickActions(
    item: LibStringItemChip,
    packageName: String,
    @LibType detailType: Int,
    canReference: Boolean,
    isApk: Boolean,
    isApkPreview: Boolean
  ) = buildDetailItemLongClickActionsUseCase(
    DetailItemLongClickActionRequest(
      item = item,
      packageName = packageName,
      detailType = detailType,
      canReference = canReference,
      isApk = isApk,
      isApkPreview = isApkPreview
    )
  )

  fun buildSignatureDetailItems(detail: String) = buildSignatureDetailItemsUseCase(detail)

  fun buildDetailReferenceNavigation(
    packageName: String?,
    refName: String?,
    @LibType refType: Int,
    visibleTypes: List<Int>
  ) = buildDetailReferenceNavigationUseCase(
    DetailReferenceNavigationRequest(
      packageName = packageName,
      refName = refName,
      refType = refType,
      visibleTypes = visibleTypes
    )
  )
}
