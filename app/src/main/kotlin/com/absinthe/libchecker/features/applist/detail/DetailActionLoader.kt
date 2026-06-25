package com.absinthe.libchecker.features.applist.detail

import android.content.pm.PackageInfo
import android.net.Uri
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.AppBundleSplitItem
import com.absinthe.libchecker.domain.app.AppManifestProperty
import com.absinthe.libchecker.domain.app.AppPackageShareFile
import com.absinthe.libchecker.domain.app.BuildSignatureDetailItemsUseCase
import com.absinthe.libchecker.domain.app.ExportAppPackageShareFileUseCase
import com.absinthe.libchecker.domain.app.ExtractNativeLibraryUseCase
import com.absinthe.libchecker.domain.app.GetAlternativeLaunchItemsUseCase
import com.absinthe.libchecker.domain.app.GetAppBundleItemsUseCase
import com.absinthe.libchecker.domain.app.GetAppInfoActionsUseCase
import com.absinthe.libchecker.domain.app.GetAppInstallSourceDetailsUseCase
import com.absinthe.libchecker.domain.app.GetAppLaunchActionUseCase
import com.absinthe.libchecker.domain.app.GetAppManifestPropertiesUseCase
import com.absinthe.libchecker.domain.app.GetElfDetailUseCase
import com.absinthe.libchecker.domain.app.GetLibraryDetailDialogDataUseCase
import com.absinthe.libchecker.domain.app.GetOverlayDetailUseCase
import com.absinthe.libchecker.domain.app.GetPermissionDetailUseCase
import com.absinthe.libchecker.domain.app.GetXposedModuleInfoUseCase
import com.absinthe.libchecker.domain.app.PrepareAppPackageShareFileUseCase
import com.absinthe.libchecker.domain.app.detail.GetRelatedAppDisplayDataUseCase
import com.absinthe.libchecker.features.statistics.bean.LibStringItem
import java.io.File

class DetailActionLoader(
  private val getAlternativeLaunchItemsUseCase: GetAlternativeLaunchItemsUseCase,
  private val getAppBundleItemsUseCase: GetAppBundleItemsUseCase,
  private val getAppInfoActionsUseCase: GetAppInfoActionsUseCase,
  private val getAppInstallSourceDetailsUseCase: GetAppInstallSourceDetailsUseCase,
  private val getAppLaunchActionUseCase: GetAppLaunchActionUseCase,
  private val getAppManifestPropertiesUseCase: GetAppManifestPropertiesUseCase,
  private val getElfDetailUseCase: GetElfDetailUseCase,
  private val getLibraryDetailDialogDataUseCase: GetLibraryDetailDialogDataUseCase,
  private val getOverlayDetailUseCase: GetOverlayDetailUseCase,
  private val getPermissionDetailUseCase: GetPermissionDetailUseCase,
  private val getRelatedAppDisplayDataUseCase: GetRelatedAppDisplayDataUseCase,
  private val getXposedModuleInfoUseCase: GetXposedModuleInfoUseCase,
  private val buildSignatureDetailItemsUseCase: BuildSignatureDetailItemsUseCase,
  private val extractNativeLibraryUseCase: ExtractNativeLibraryUseCase,
  private val prepareAppPackageShareFileUseCase: PrepareAppPackageShareFileUseCase,
  private val exportAppPackageShareFileUseCase: ExportAppPackageShareFileUseCase
) {
  suspend fun getAppBundleItems(packageInfo: PackageInfo): List<AppBundleSplitItem> {
    return getAppBundleItemsUseCase(packageInfo)
  }

  suspend fun getAppInfoActions(packageName: String) = getAppInfoActionsUseCase(packageName)

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

  suspend fun prepareAppPackageShareFile(
    cacheDir: File,
    packageName: String
  ) = prepareAppPackageShareFileUseCase(cacheDir, packageName)

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

  fun buildSignatureDetailItems(detail: String) = buildSignatureDetailItemsUseCase(detail)
}
