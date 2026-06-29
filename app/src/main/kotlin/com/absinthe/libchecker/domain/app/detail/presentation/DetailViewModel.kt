package com.absinthe.libchecker.domain.app.detail.presentation

import android.content.pm.PackageInfo
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.AppBundleSplitItem
import com.absinthe.libchecker.domain.app.PrepareApkAnalysisPackageUseCase
import com.absinthe.libchecker.domain.app.VersionedFeature
import com.absinthe.libchecker.domain.app.detail.AppDetailAbiLabelData
import com.absinthe.libchecker.domain.app.detail.AppDetailHeaderExtraInfo
import com.absinthe.libchecker.domain.app.detail.action.AppManifestProperty
import com.absinthe.libchecker.domain.app.detail.action.AppPackageShareFile
import com.absinthe.libchecker.domain.app.detail.action.DetailItemDialogRequest
import com.absinthe.libchecker.domain.app.detail.action.DetailItemLongClickActions
import com.absinthe.libchecker.domain.app.detail.feature.AppDetailFeatureItemData
import com.absinthe.libchecker.domain.app.detail.model.LibStringItem
import com.absinthe.libchecker.domain.app.detail.model.LibStringItemChip
import com.absinthe.libchecker.domain.app.detail.navigation.DetailReferenceNavigation
import com.absinthe.libchecker.domain.app.detail.packageinfo.GetAppDetailPackageUseCase
import com.absinthe.libchecker.domain.app.detail.presentation.DetailActionLoader
import com.absinthe.libchecker.domain.app.detail.presentation.DetailFeatureLoader
import com.absinthe.libchecker.domain.app.detail.presentation.DetailFilterController
import com.absinthe.libchecker.domain.app.detail.presentation.DetailPackageLoader
import com.absinthe.libchecker.domain.app.detail.presentation.DetailPackageState
import com.absinthe.libchecker.domain.app.detail.presentation.content.DetailContentLoader
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.utils.apk.ApkPreviewInfo
import java.io.File
import timber.log.Timber

class DetailViewModel(
  private val detailActionLoader: DetailActionLoader,
  private val detailContentLoader: DetailContentLoader,
  private val detailFilterController: DetailFilterController,
  private val detailFeatureLoader: DetailFeatureLoader,
  private val detailPackageLoader: DetailPackageLoader
) : ViewModel() {
  val contentState = detailContentLoader.contentState
  val featureState = detailFeatureLoader.featureState
  val filterState = detailFilterController.filterState
  private val packageState: DetailPackageState
    get() = detailPackageLoader.packageState

  val isApk: Boolean
    get() = detailPackageLoader.isApk

  val isApkPreview: Boolean
    get() = detailPackageLoader.isApkPreview

  val packageInfo: PackageInfo
    get() = detailPackageLoader.packageInfo

  val apkPreviewInfo: ApkPreviewInfo?
    get() = detailPackageLoader.apkPreviewInfo

  val packageInfoStateFlow = detailPackageLoader.packageInfoStateFlow

  fun packageName(): String = detailPackageLoader.packageName()

  fun initPackageInfo(pi: PackageInfo) {
    detailPackageLoader.initPackageInfo(pi)
  }

  fun startApkMode() {
    detailPackageLoader.startApkMode()
  }

  fun startApkPreviewMode() {
    detailPackageLoader.startApkPreviewMode()
  }

  fun setApkPreviewInfo(apkPreviewInfo: ApkPreviewInfo) {
    detailPackageLoader.setApkPreviewInfo(apkPreviewInfo)
  }

  fun clearApkPreviewInfo() {
    detailPackageLoader.clearApkPreviewInfo()
  }

  fun isPackageInfoAvailable(): Boolean {
    return detailPackageLoader.isPackageInfoAvailable()
  }

  suspend fun loadAppDetailPackage(packageName: String): GetAppDetailPackageUseCase.Result {
    return detailPackageLoader.loadAppDetailPackage(packageName)
  }

  fun buildAppDetailAbiLabelData(
    abi: Int,
    abiSet: Collection<Int>,
    apkAnalyticsMode: Boolean
  ): AppDetailAbiLabelData {
    return detailFeatureLoader.buildAppDetailAbiLabelData(abi, abiSet, apkAnalyticsMode)
  }

  suspend fun buildAppDetailHeaderExtraInfo(
    packageInfo: PackageInfo,
    showAndroidVersion: Boolean
  ): AppDetailHeaderExtraInfo {
    return detailFeatureLoader.buildAppDetailHeaderExtraInfo(packageState, packageInfo, showAndroidVersion)
  }

  fun buildAppDetailHeaderTitleData(
    packageInfo: PackageInfo,
    apkAnalyticsMode: Boolean
  ) = detailFeatureLoader.buildAppDetailHeaderTitleData(packageState, packageInfo, apkAnalyticsMode)

  fun buildAppDetailFeatureItem(
    feature: VersionedFeature,
    currentFeatureCount: Int,
    apkAnalyticsMode: Boolean,
    canShowInstallSource: Boolean,
    canShowAppIcons: Boolean
  ): AppDetailFeatureItemData? {
    return detailFeatureLoader.buildAppDetailFeatureItem(
      feature = feature,
      currentFeatureCount = currentFeatureCount,
      apkAnalyticsMode = apkAnalyticsMode,
      canShowInstallSource = canShowInstallSource,
      canShowAppIcons = canShowAppIcons
    )
  }

  suspend fun getAppBundleItems(packageInfo: PackageInfo): List<AppBundleSplitItem> {
    return detailActionLoader.getAppBundleItems(packageInfo)
  }

  suspend fun getAppInfoActions(packageName: String) = detailActionLoader.getAppInfoActions(packageName)

  suspend fun getAppInfoPrimaryActions(packageName: String?) = detailActionLoader.getAppInfoPrimaryActions(packageName)

  suspend fun getAppLaunchAction(packageName: String?) = detailActionLoader.getAppLaunchAction(packageName)

  suspend fun getAlternativeLaunchItems(packageName: String) = detailActionLoader.getAlternativeLaunchItems(packageName)

  suspend fun getAppInstallSourceDetails(packageName: String) = detailActionLoader.getAppInstallSourceDetails(packageName)

  suspend fun getXposedModuleInfo(packageName: String) = detailActionLoader.getXposedModuleInfo(packageName)

  suspend fun extractNativeLibrary(item: LibStringItem) = detailActionLoader.extractNativeLibrary(packageState, item)

  suspend fun prepareAppPackageShareAction(cacheDir: File, packageName: String) = detailActionLoader.prepareAppPackageShareAction(cacheDir, packageName)

  suspend fun exportAppPackageShareFile(
    shareFile: AppPackageShareFile,
    destinationUri: Uri
  ) = detailActionLoader.exportAppPackageShareFile(shareFile, destinationUri)

  suspend fun getApkPreviewInfo(url: String): Result<ApkPreviewInfo> {
    return detailPackageLoader.getApkPreviewInfo(url)
  }

  suspend fun getAppManifestProperties(
    packageInfo: PackageInfo?,
    properties: Map<String, String>?
  ): List<AppManifestProperty> {
    return detailActionLoader.getAppManifestProperties(packageInfo, properties)
  }

  suspend fun prepareApkAnalysisPackage(
    cacheDir: File,
    uri: Uri
  ): PrepareApkAnalysisPackageUseCase.Result {
    return detailPackageLoader.prepareApkAnalysisPackage(cacheDir, uri)
  }

  suspend fun getElfDetail(packageName: String, elfPath: String) = detailActionLoader.getElfDetail(packageName, elfPath)

  suspend fun isInstalledAppComparisonAvailable(packageName: String): Boolean {
    return detailPackageLoader.isInstalledAppComparisonAvailable(packageName)
  }

  suspend fun loadStaticLibraryTabItems(packageName: String): List<LibStringItemChip> {
    return detailContentLoader.loadStaticLibraryTabItems(packageState, packageName)
  }

  suspend fun emitStaticLibItems(items: List<LibStringItemChip>) {
    detailContentLoader.emitStaticLibItems(items)
  }

  suspend fun loadInstalledAppComparisonPackage(packageName: String): PackageInfo? {
    return detailPackageLoader.loadInstalledAppComparisonPackage(packageName)
  }

  suspend fun buildPackageComparisonSnapshotItem(
    basePackage: PackageInfo,
    analysisPackage: PackageInfo
  ): SnapshotDiffItem {
    return detailPackageLoader.buildPackageComparisonSnapshotItem(basePackage, analysisPackage)
  }

  fun reset() {
    Timber.d("reset")
    detailContentLoader.reset()
    detailFilterController.reset()
    detailFeatureLoader.reset()
  }

  fun initSoAnalysisData() {
    detailContentLoader.initSoAnalysisData(viewModelScope, featureState, packageState)
  }

  fun loadSoAnalysisData(tab: String) {
    detailContentLoader.loadSoAnalysisData(viewModelScope, packageState, tab)
  }

  fun initStaticData() {
    detailContentLoader.initStaticData(viewModelScope, packageState)
  }

  fun initMetaDataData() {
    detailContentLoader.initMetaDataData(viewModelScope, packageState)
  }

  fun initPermissionData() {
    detailContentLoader.initPermissionData(viewModelScope, featureState, packageState)
  }

  fun initDexData() {
    detailContentLoader.initDexData(viewModelScope, packageState)
  }

  fun cancelInitDexDataJob() {
    detailContentLoader.cancelInitDexDataJob()
  }

  fun initSignatures() {
    detailContentLoader.initSignatures(viewModelScope, packageState)
  }

  fun initComponentsData() {
    detailContentLoader.initComponentsData(viewModelScope, packageState)
  }

  fun initComponentsDataInPreview() = detailContentLoader.initComponentsDataInPreview(viewModelScope, packageState)

  suspend fun getLibraryDetailDialogHeader(
    libName: String,
    @LibType type: Int,
    isValidLib: Boolean
  ) = detailActionLoader.getLibraryDetailDialogHeader(libName, type, isValidLib)

  suspend fun getLibraryDetailDialogData(
    libName: String,
    @LibType type: Int,
    regexName: String?,
    isValidLib: Boolean
  ) = detailActionLoader.getLibraryDetailDialogData(libName, type, regexName, isValidLib)

  suspend fun getOverlayDetail(item: LCItem) = detailActionLoader.getOverlayDetail(item)

  suspend fun getPermissionDetail(permissionName: String) = detailActionLoader.getPermissionDetail(permissionName)

  fun initAbilities(packageName: String) {
    detailContentLoader.initAbilities(viewModelScope, packageName)
  }

  fun emitFeature(feature: VersionedFeature) {
    detailFeatureLoader.emitFeature(viewModelScope, feature)
  }

  fun setFeatureLoading(loading: Boolean) {
    detailFeatureLoader.setLoading(viewModelScope, loading)
  }

  suspend fun getRelatedAppDisplayData(packageName: String) = detailActionLoader.getRelatedAppDisplayData(packageName)

  fun buildSignatureDetailItems(detail: String) = detailActionLoader.buildSignatureDetailItems(detail)

  fun buildDetailItemDialogRequest(
    item: LibStringItemChip,
    @LibType detailType: Int
  ): DetailItemDialogRequest {
    return detailActionLoader.buildDetailItemDialogRequest(item, detailType)
  }

  fun buildDetailItemLongClickActions(
    item: LibStringItemChip,
    packageName: String,
    @LibType detailType: Int,
    canReference: Boolean
  ): DetailItemLongClickActions {
    return detailActionLoader.buildDetailItemLongClickActions(
      item = item,
      packageName = packageName,
      detailType = detailType,
      canReference = canReference,
      isApk = isApk,
      isApkPreview = isApkPreview
    )
  }

  fun buildDetailReferenceNavigation(
    packageName: String?,
    refName: String?,
    @LibType refType: Int,
    visibleTypes: List<Int>
  ): DetailReferenceNavigation? {
    return detailActionLoader.buildDetailReferenceNavigation(
      packageName = packageName,
      refName = refName,
      refType = refType,
      visibleTypes = visibleTypes
    )
  }

  fun initFeatures(packageInfo: PackageInfo, features: Int) {
    detailFeatureLoader.initFeatures(viewModelScope, packageState, packageInfo, features)
  }

  fun initAbiInfo(packageInfo: PackageInfo, apkAnalyticsMode: Boolean) {
    detailFeatureLoader.initAbiInfo(viewModelScope, packageInfo, apkAnalyticsMode)
  }

  fun initAbiInfo(apkPreviewInfo: ApkPreviewInfo) {
    detailFeatureLoader.initAbiInfo(viewModelScope, apkPreviewInfo)
  }

  fun filterDetailItems(
    items: List<LibStringItemChip>,
    searchWords: String?,
    process: String?
  ): List<LibStringItemChip> {
    return detailFilterController.filterDetailItems(items, searchWords, process)
  }

  fun filterPermissionDetailItems(
    items: List<LibStringItemChip>,
    searchWords: String?,
    process: String?
  ): List<LibStringItemChip> {
    return detailFilterController.filterPermissionDetailItems(items, searchWords, process)
  }

  fun sortDetailItems(items: List<LibStringItemChip>, @LibType type: Int): List<LibStringItemChip> {
    return detailFilterController.sortDetailItems(items, type)
  }

  fun buildProcessFilterData(
    @LibType type: Int,
    permissionNotGrantedLabel: String,
    permissionNotGrantedColor: Int
  ) = detailFilterController.buildProcessFilterData(
    type = type,
    componentProcessesMap = contentState.processesMap,
    permissionItems = contentState.permissionsItems.value,
    permissionNotGrantedLabel = permissionNotGrantedLabel,
    permissionNotGrantedColor = permissionNotGrantedColor
  )

  fun isComponentDetailType(@LibType type: Int): Boolean {
    return detailFilterController.isComponentDetailType(type)
  }

  fun hasNonGrantedPermissions(@LibType type: Int): Boolean {
    return detailFilterController.hasNonGrantedPermissions(type, contentState.permissionsItems.value)
  }
}
