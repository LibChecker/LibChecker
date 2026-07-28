package com.absinthe.libchecker.domain.app.detail.presentation

import android.content.Context
import android.content.pm.PackageInfo
import android.net.Uri
import com.absinthe.libchecker.annotation.ACTION
import com.absinthe.libchecker.annotation.ACTION_IN_RULES
import com.absinthe.libchecker.annotation.ALL
import com.absinthe.libchecker.annotation.ET_NOT_ELF
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.isComponentType
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.detail.action.AppInstallSourceDisplayRequest
import com.absinthe.libchecker.domain.app.detail.action.AppPackageShareFile
import com.absinthe.libchecker.domain.app.detail.action.DetailAppInfoResolver
import com.absinthe.libchecker.domain.app.detail.action.DetailItemDialogRequest
import com.absinthe.libchecker.domain.app.detail.action.DetailItemElfInfoAction
import com.absinthe.libchecker.domain.app.detail.action.DetailItemLongClickActions
import com.absinthe.libchecker.domain.app.detail.action.DetailItemReferenceAction
import com.absinthe.libchecker.domain.app.detail.action.DetailItemResolver
import com.absinthe.libchecker.domain.app.detail.action.ExportAppPackageShareFileUseCase
import com.absinthe.libchecker.domain.app.detail.action.ExtractNativeLibraryUseCase
import com.absinthe.libchecker.domain.app.detail.action.OverlayDetailResult
import com.absinthe.libchecker.domain.app.detail.action.PrepareAppPackageShareActionUseCase
import com.absinthe.libchecker.domain.app.detail.action.buildAppInstallSourceBottomSheetDisplay
import com.absinthe.libchecker.domain.app.detail.action.buildOverlayDetailBottomSheetDisplay
import com.absinthe.libchecker.domain.app.detail.action.buildXposedInfoBottomSheetDisplay
import com.absinthe.libchecker.domain.app.detail.content.buildAppBundleItemDisplayData
import com.absinthe.libchecker.domain.app.detail.content.getAppBundleSplitItems
import com.absinthe.libchecker.domain.app.detail.model.AppBundleItem
import com.absinthe.libchecker.domain.app.detail.model.AppInfoBottomSheetState
import com.absinthe.libchecker.domain.app.detail.model.AppInstallSourceBottomSheetDisplay
import com.absinthe.libchecker.domain.app.detail.model.AppInstallSourceRequesterAccess
import com.absinthe.libchecker.domain.app.detail.model.AppPropItem
import com.absinthe.libchecker.domain.app.detail.model.LibStringItem
import com.absinthe.libchecker.domain.app.detail.model.LibStringItemChip
import com.absinthe.libchecker.domain.app.detail.model.OverlayDetailBottomSheetResult
import com.absinthe.libchecker.domain.app.detail.model.SignatureDetailItem
import com.absinthe.libchecker.domain.app.detail.model.XposedInfoBottomSheetDisplay
import com.absinthe.libchecker.domain.app.detail.navigation.DetailReferenceNavigation
import com.absinthe.libchecker.domain.app.detail.related.RelatedAppDisplayData
import com.absinthe.libchecker.domain.app.detail.related.buildRelatedAppDisplayData
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import java.io.File
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class DetailActionLoader(
  private val context: Context,
  private val installedAppRepository: InstalledAppRepository,
  private val appInfoResolver: DetailAppInfoResolver,
  private val itemResolver: DetailItemResolver,
  private val extractNativeLibraryUseCase: ExtractNativeLibraryUseCase,
  private val prepareAppPackageShareActionUseCase: PrepareAppPackageShareActionUseCase,
  private val exportAppPackageShareFileUseCase: ExportAppPackageShareFileUseCase
) {
  suspend fun getAppBundleItems(packageInfo: PackageInfo): List<AppBundleItem> {
    return buildAppBundleItemDisplayData(context, getAppBundleSplitItems(packageInfo))
  }

  suspend fun getAppInfoBottomSheetState(packageName: String?): AppInfoBottomSheetState.Content {
    return coroutineScope {
      val primaryActions = async {
        appInfoResolver.getAppInfoPrimaryActions(packageName)
      }
      val externalActions = async {
        packageName?.let {
          appInfoResolver.getAppInfoActions(it)
        }.orEmpty()
      }
      AppInfoBottomSheetState.Content(
        packageName = packageName,
        primaryActions = primaryActions.await(),
        externalActions = externalActions.await()
      )
    }
  }

  suspend fun getAppInfoPrimaryActions(packageName: String?) = appInfoResolver.getAppInfoPrimaryActions(packageName)

  suspend fun getAppLaunchAction(packageName: String?) = appInfoResolver.getAppLaunchAction(packageName)

  suspend fun getAlternativeLaunchItems(packageName: String) = appInfoResolver.getAlternativeLaunchItems(packageName)

  suspend fun getAppInstallSourceBottomSheetDisplay(
    packageName: String,
    requesterAccess: AppInstallSourceRequesterAccess
  ): AppInstallSourceBottomSheetDisplay? {
    val details = appInfoResolver.getAppInstallSourceDetails(packageName) ?: return null
    val installSource = details.installSource
    return coroutineScope {
      val originatingApp = async {
        if (requesterAccess == AppInstallSourceRequesterAccess.Available) {
          installSource?.originatingPackageName?.let {
            getRelatedAppDisplayData(it)
          }
        } else {
          null
        }
      }
      val installingApp = async {
        installSource?.installingPackageName?.let {
          getRelatedAppDisplayData(it)
        }
      }
      buildAppInstallSourceBottomSheetDisplay(
        context,
        AppInstallSourceDisplayRequest(
          details = details,
          originatingApp = originatingApp.await(),
          installingApp = installingApp.await(),
          requesterAccess = requesterAccess
        )
      )
    }
  }

  suspend fun getXposedInfoBottomSheetDisplay(packageName: String): XposedInfoBottomSheetDisplay? {
    return appInfoResolver.getXposedModuleInfo(packageName)
      ?.let { buildXposedInfoBottomSheetDisplay(context, it) }
  }

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
  ): List<AppPropItem> {
    return itemResolver.getAppManifestProperties(packageInfo, properties)
  }

  suspend fun getElfDetail(packageName: String, elfPath: String) = itemResolver.getElfDetail(packageName, elfPath)

  suspend fun getLibraryDetailDialogHeader(
    libName: String,
    @LibType type: Int,
    isValidLib: Boolean
  ) = itemResolver.getHeader(
    DetailItemResolver.HeaderRequest(
      libName = libName,
      type = type,
      isValidLib = isValidLib
    )
  )

  suspend fun getLibraryDetailDialogData(
    libName: String,
    @LibType type: Int,
    regexName: String?,
    isValidLib: Boolean,
    preferredLocale: String
  ) = itemResolver(
    DetailItemResolver.Request(
      libName = libName,
      type = type,
      regexName = regexName,
      isValidLib = isValidLib,
      preferredLocale = preferredLocale
    )
  )

  suspend fun getOverlayDetailBottomSheetResult(item: LCItem): OverlayDetailBottomSheetResult {
    return when (val result = appInfoResolver.getOverlayDetail(item)) {
      OverlayDetailResult.NotFound -> OverlayDetailBottomSheetResult.NotFound

      is OverlayDetailResult.Available -> {
        val targetApp = result.data.targetPackageName?.let {
          getRelatedAppDisplayData(it)
        }
        OverlayDetailBottomSheetResult.Available(
          buildOverlayDetailBottomSheetDisplay(result.data, targetApp)
        )
      }
    }
  }

  suspend fun getPermissionDetail(permissionName: String) = itemResolver.getPermissionDetail(permissionName)

  private suspend fun getRelatedAppDisplayData(packageName: String): RelatedAppDisplayData? {
    val relatedApp = appInfoResolver.getRelatedAppListItem(packageName) ?: return null
    return buildRelatedAppDisplayData(context, installedAppRepository, packageName, relatedApp)
  }

  fun buildDetailItemDialogRequest(
    item: LibStringItemChip,
    @LibType detailType: Int
  ): DetailItemDialogRequest {
    if (detailType == PERMISSION) {
      return DetailItemDialogRequest.Permission(item.item.name)
    }

    val rule = item.rule
    return DetailItemDialogRequest.Library(
      name = rule?.libName ?: item.item.name,
      type = if (rule?.libType == ACTION_IN_RULES) ACTION else detailType,
      regexName = rule?.regexName,
      isValidLib = rule != null
    )
  }

  fun buildDetailItemLongClickActions(
    item: LibStringItemChip,
    packageName: String,
    @LibType detailType: Int,
    canReference: Boolean,
    isApk: Boolean,
    isApkPreview: Boolean
  ): DetailItemLongClickActions {
    val componentName = if (detailType == PERMISSION) {
      item.item.name.substringBefore(" ")
    } else {
      item.item.name
    }
    val elfInfo = if (detailType != NATIVE || isApkPreview || item.item.elfInfo.elfType == ET_NOT_ELF) {
      null
    } else {
      DetailItemElfInfoAction(
        packageName = packageName,
        elfPath = item.item.source.orEmpty(),
        ruleIcon = item.rule?.iconRes ?: com.absinthe.lc.rulesbundle.R.drawable.ic_sdk_placeholder
      )
    }
    val reference = if (!canReference || componentName.startsWith(".")) {
      null
    } else {
      DetailItemReferenceAction(
        refName = item.rule?.libName ?: componentName,
        label = item.rule?.label,
        type = if (item.rule?.libType == ACTION_IN_RULES) ACTION else detailType
      )
    }
    return DetailItemLongClickActions(
      packageName = packageName,
      componentName = componentName,
      fullComponentName = if (componentName.startsWith(".")) packageName + componentName else componentName,
      copyText = if (detailType == METADATA) componentName + ": " + item.item.source else componentName,
      elfExtractAvailable = detailType == NATIVE && !isApkPreview,
      elfInfo = elfInfo,
      reference = reference,
      integrationsAvailable = !isApk && !isApkPreview,
      providerPermissionAvailable = detailType == PERMISSION && !isApkPreview
    )
  }

  fun buildSignatureDetailItems(detail: String): List<SignatureDetailItem> {
    return detail.lines().map {
      val values = it.split(":", limit = 2)
      SignatureDetailItem(
        values.getOrNull(0).orEmpty(),
        values.getOrNull(1).orEmpty()
      )
    }
  }

  fun buildDetailReferenceNavigation(
    packageName: String?,
    refName: String?,
    @LibType refType: Int,
    visibleTypes: List<Int>
  ): DetailReferenceNavigation? {
    packageName ?: return null
    refName ?: return null
    if (refType == ALL) {
      return null
    }

    val tabPosition = visibleTypes.indexOf(refType)
    if (tabPosition < 0) {
      return null
    }

    return DetailReferenceNavigation(
      type = refType,
      tabPosition = tabPosition,
      targetName = if (isComponentType(refType)) {
        refName.removePrefix(packageName)
      } else {
        refName
      }
    )
  }
}
