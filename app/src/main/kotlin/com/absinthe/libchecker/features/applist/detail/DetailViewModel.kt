package com.absinthe.libchecker.features.applist.detail

import android.content.pm.PackageInfo
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.AppBundleSplitItem
import com.absinthe.libchecker.domain.app.AppDetailAbiLabelData
import com.absinthe.libchecker.domain.app.AppDetailHeaderExtraInfo
import com.absinthe.libchecker.domain.app.AppDetailSettingsRepository
import com.absinthe.libchecker.domain.app.AppManifestProperty
import com.absinthe.libchecker.domain.app.AppPackageShareFile
import com.absinthe.libchecker.domain.app.BuildAppDetailAbiLabelDataUseCase
import com.absinthe.libchecker.domain.app.BuildAppDetailHeaderExtraInfoUseCase
import com.absinthe.libchecker.domain.app.BuildAppDetailHeaderTitleDataUseCase
import com.absinthe.libchecker.domain.app.BuildRelatedAppDisplayDataUseCase
import com.absinthe.libchecker.domain.app.BuildSignatureDetailItemsUseCase
import com.absinthe.libchecker.domain.app.ExportAppPackageShareFileUseCase
import com.absinthe.libchecker.domain.app.ExtractNativeLibraryUseCase
import com.absinthe.libchecker.domain.app.FilterAppDetailItemsUseCase
import com.absinthe.libchecker.domain.app.GetAlternativeLaunchItemsUseCase
import com.absinthe.libchecker.domain.app.GetApkPreviewInfoUseCase
import com.absinthe.libchecker.domain.app.GetAppBundleItemsUseCase
import com.absinthe.libchecker.domain.app.GetAppDetailAbiUseCase
import com.absinthe.libchecker.domain.app.GetAppDetailFeaturesUseCase
import com.absinthe.libchecker.domain.app.GetAppDetailPackageUseCase
import com.absinthe.libchecker.domain.app.GetAppInfoActionsUseCase
import com.absinthe.libchecker.domain.app.GetAppInstallSourceDetailsUseCase
import com.absinthe.libchecker.domain.app.GetAppLaunchActionUseCase
import com.absinthe.libchecker.domain.app.GetAppManifestPropertiesUseCase
import com.absinthe.libchecker.domain.app.GetElfDetailUseCase
import com.absinthe.libchecker.domain.app.GetInstalledAppComparisonPackageUseCase
import com.absinthe.libchecker.domain.app.GetLibraryDetailDialogDataUseCase
import com.absinthe.libchecker.domain.app.GetOverlayDetailUseCase
import com.absinthe.libchecker.domain.app.GetPermissionDetailUseCase
import com.absinthe.libchecker.domain.app.GetRelatedAppListItemUseCase
import com.absinthe.libchecker.domain.app.GetXposedModuleInfoUseCase
import com.absinthe.libchecker.domain.app.HasInstalledStaticLibrariesUseCase
import com.absinthe.libchecker.domain.app.PrepareApkAnalysisPackageUseCase
import com.absinthe.libchecker.domain.app.PrepareAppPackageShareFileUseCase
import com.absinthe.libchecker.domain.app.RelatedAppListItem
import com.absinthe.libchecker.domain.app.SortAppDetailItemsUseCase
import com.absinthe.libchecker.domain.app.VersionedFeature
import com.absinthe.libchecker.domain.snapshot.BuildPackageComparisonSnapshotItemUseCase
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.features.applist.MODE_SORT_BY_LIB
import com.absinthe.libchecker.features.statistics.bean.LibStringItem
import com.absinthe.libchecker.features.statistics.bean.LibStringItemChip
import com.absinthe.libchecker.utils.apk.ApkPreviewInfo
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class DetailViewModel(
  private val getAppDetailPackage: GetAppDetailPackageUseCase,
  private val getAlternativeLaunchItemsUseCase: GetAlternativeLaunchItemsUseCase,
  private val getAppInfoActionsUseCase: GetAppInfoActionsUseCase,
  private val getAppLaunchActionUseCase: GetAppLaunchActionUseCase,
  private val getAppBundleItemsUseCase: GetAppBundleItemsUseCase,
  private val filterAppDetailItemsUseCase: FilterAppDetailItemsUseCase,
  private val getAppDetailAbiUseCase: GetAppDetailAbiUseCase,
  private val getAppInstallSourceDetailsUseCase: GetAppInstallSourceDetailsUseCase,
  private val getAppDetailFeaturesUseCase: GetAppDetailFeaturesUseCase,
  private val buildAppDetailAbiLabelDataUseCase: BuildAppDetailAbiLabelDataUseCase,
  private val buildAppDetailHeaderExtraInfoUseCase: BuildAppDetailHeaderExtraInfoUseCase,
  private val buildAppDetailHeaderTitleDataUseCase: BuildAppDetailHeaderTitleDataUseCase,
  private val extractNativeLibraryUseCase: ExtractNativeLibraryUseCase,
  private val prepareAppPackageShareFileUseCase: PrepareAppPackageShareFileUseCase,
  private val exportAppPackageShareFileUseCase: ExportAppPackageShareFileUseCase,
  private val getApkPreviewInfoUseCase: GetApkPreviewInfoUseCase,
  private val getAppManifestPropertiesUseCase: GetAppManifestPropertiesUseCase,
  private val prepareApkAnalysisPackageUseCase: PrepareApkAnalysisPackageUseCase,
  private val getElfDetailUseCase: GetElfDetailUseCase,
  private val getInstalledAppComparisonPackageUseCase: GetInstalledAppComparisonPackageUseCase,
  private val hasInstalledStaticLibrariesUseCase: HasInstalledStaticLibrariesUseCase,
  private val getLibraryDetailDialogDataUseCase: GetLibraryDetailDialogDataUseCase,
  private val getOverlayDetailUseCase: GetOverlayDetailUseCase,
  private val getPermissionDetailUseCase: GetPermissionDetailUseCase,
  private val getRelatedAppListItemUseCase: GetRelatedAppListItemUseCase,
  private val buildRelatedAppDisplayDataUseCase: BuildRelatedAppDisplayDataUseCase,
  private val buildSignatureDetailItemsUseCase: BuildSignatureDetailItemsUseCase,
  private val getXposedModuleInfoUseCase: GetXposedModuleInfoUseCase,
  private val sortAppDetailItemsUseCase: SortAppDetailItemsUseCase,
  private val appDetailSettingsRepository: AppDetailSettingsRepository,
  private val detailContentLoader: DetailContentLoader,
  private val buildPackageComparisonSnapshotItemUseCase: BuildPackageComparisonSnapshotItemUseCase
) : ViewModel() {
  val contentState = DetailContentState()
  val featureState = DetailFeatureState()
  val filterState = DetailFilterState()
  private val packageState = DetailPackageState()

  val isApk: Boolean
    get() = packageState.isApk

  val isApkPreview: Boolean
    get() = packageState.isApkPreview

  val packageInfo: PackageInfo
    get() = packageState.packageInfo

  val apkPreviewInfo: ApkPreviewInfo?
    get() = packageState.apkPreviewInfo

  val packageInfoStateFlow = packageState.packageInfoStateFlow

  fun packageName(): String = packageState.packageName()

  fun initPackageInfo(pi: PackageInfo) {
    packageState.setPackageInfo(pi)
  }

  fun startApkMode() {
    packageState.startApkMode()
  }

  fun startApkPreviewMode() {
    packageState.startApkPreviewMode()
  }

  fun setApkPreviewInfo(apkPreviewInfo: ApkPreviewInfo) {
    packageState.apkPreviewInfo = apkPreviewInfo
  }

  fun clearApkPreviewInfo() {
    packageState.clearApkPreviewInfo()
  }

  fun isPackageInfoAvailable(): Boolean {
    return packageState.hasPackageInfo()
  }

  suspend fun loadAppDetailPackage(packageName: String): GetAppDetailPackageUseCase.Result {
    return getAppDetailPackage(packageName)
  }

  fun buildAppDetailAbiLabelData(
    abi: Int,
    abiSet: Collection<Int>,
    apkAnalyticsMode: Boolean
  ): AppDetailAbiLabelData {
    return buildAppDetailAbiLabelDataUseCase(
      abi = abi,
      abiSet = abiSet,
      apkAnalyticsMode = apkAnalyticsMode
    )
  }

  suspend fun buildAppDetailHeaderExtraInfo(
    packageInfo: PackageInfo,
    showAndroidVersion: Boolean
  ): AppDetailHeaderExtraInfo {
    return buildAppDetailHeaderExtraInfoUseCase(
      packageInfo = packageInfo,
      apkPreviewInfo = packageState.apkPreviewInfo,
      isApkPreview = packageState.isApkPreview,
      showAndroidVersion = showAndroidVersion
    )
  }

  fun buildAppDetailHeaderTitleData(
    packageInfo: PackageInfo,
    apkAnalyticsMode: Boolean
  ) = buildAppDetailHeaderTitleDataUseCase(
    packageInfo = packageInfo,
    apkPreviewInfo = packageState.apkPreviewInfo,
    apkAnalyticsMode = apkAnalyticsMode
  )

  suspend fun getAppBundleItems(packageInfo: PackageInfo): List<AppBundleSplitItem> {
    return getAppBundleItemsUseCase(packageInfo)
  }

  suspend fun getAppInfoActions(packageName: String) = getAppInfoActionsUseCase(packageName)

  suspend fun getAppLaunchAction(packageName: String?) = getAppLaunchActionUseCase(packageName)

  suspend fun getAlternativeLaunchItems(packageName: String) = getAlternativeLaunchItemsUseCase(packageName)

  suspend fun getAppInstallSourceDetails(packageName: String) = getAppInstallSourceDetailsUseCase(packageName)

  suspend fun getXposedModuleInfo(packageName: String) = getXposedModuleInfoUseCase(packageName)

  suspend fun extractNativeLibrary(item: LibStringItem) = extractNativeLibraryUseCase(
    packageState.packageInfo,
    item,
    packageState.isApkPreview
  )

  suspend fun prepareAppPackageShareFile(cacheDir: File, packageName: String) = prepareAppPackageShareFileUseCase(cacheDir, packageName)

  suspend fun exportAppPackageShareFile(
    shareFile: AppPackageShareFile,
    destinationUri: Uri
  ) = exportAppPackageShareFileUseCase(shareFile, destinationUri)

  suspend fun getApkPreviewInfo(url: String): Result<ApkPreviewInfo> {
    return getApkPreviewInfoUseCase(url)
  }

  suspend fun getAppManifestProperties(
    packageInfo: PackageInfo?,
    properties: Map<String, String>?
  ): List<AppManifestProperty> {
    return getAppManifestPropertiesUseCase(packageInfo, properties)
  }

  suspend fun prepareApkAnalysisPackage(
    cacheDir: File,
    uri: Uri
  ): PrepareApkAnalysisPackageUseCase.Result {
    return prepareApkAnalysisPackageUseCase(cacheDir, uri)
  }

  suspend fun getElfDetail(packageName: String, elfPath: String) = getElfDetailUseCase(packageName, elfPath)

  suspend fun isInstalledAppComparisonAvailable(packageName: String): Boolean {
    return getInstalledAppComparisonPackageUseCase.isAvailable(packageName)
  }

  suspend fun hasInstalledStaticLibraries(packageName: String): Boolean {
    return hasInstalledStaticLibrariesUseCase(packageName)
  }

  suspend fun loadInstalledAppComparisonPackage(packageName: String): PackageInfo? {
    return getInstalledAppComparisonPackageUseCase(packageName)
  }

  suspend fun buildPackageComparisonSnapshotItem(
    basePackage: PackageInfo,
    analysisPackage: PackageInfo
  ): SnapshotDiffItem {
    return buildPackageComparisonSnapshotItemUseCase(basePackage, analysisPackage)
  }

  fun reset() {
    Timber.d("reset")
    detailContentLoader.cancelAll()
    contentState.reset()
    filterState.reset()
  }

  fun initSoAnalysisData() {
    detailContentLoader.initSoAnalysisData(viewModelScope, contentState, featureState, packageState)
  }

  fun loadSoAnalysisData(tab: String) {
    detailContentLoader.loadSoAnalysisData(viewModelScope, contentState, packageState, tab)
  }

  fun initStaticData() {
    detailContentLoader.initStaticData(viewModelScope, contentState, packageState)
  }

  fun initMetaDataData() {
    detailContentLoader.initMetaDataData(viewModelScope, contentState, packageState)
  }

  fun initPermissionData() {
    detailContentLoader.initPermissionData(viewModelScope, contentState, featureState, packageState)
  }

  fun initDexData() {
    detailContentLoader.initDexData(viewModelScope, contentState, packageState)
  }

  fun cancelInitDexDataJob() {
    detailContentLoader.cancelInitDexDataJob()
  }

  fun initSignatures() {
    detailContentLoader.initSignatures(viewModelScope, contentState, packageState)
  }

  fun initComponentsData() {
    detailContentLoader.initComponentsData(viewModelScope, contentState, packageState)
  }

  fun initComponentsDataInPreview() = detailContentLoader.initComponentsDataInPreview(viewModelScope, contentState, packageState)

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

  fun initAbilities(packageName: String) {
    detailContentLoader.initAbilities(viewModelScope, contentState, packageName)
  }

  fun emitFeature(feature: VersionedFeature) = viewModelScope.launch {
    featureState.emitFeature(feature)
  }

  suspend fun getRelatedAppListItem(packageName: String): RelatedAppListItem? {
    return getRelatedAppListItemUseCase(packageName)
  }

  fun buildRelatedAppDisplayData(packageName: String, relatedApp: RelatedAppListItem) = buildRelatedAppDisplayDataUseCase(packageName, relatedApp)

  fun buildSignatureDetailItems(detail: String) = buildSignatureDetailItemsUseCase(detail)

  fun initFeatures(packageInfo: PackageInfo, features: Int) = viewModelScope.launch(Dispatchers.IO) {
    Timber.d("initFeatures: features = $features")

    val detailFeatures = getAppDetailFeaturesUseCase(packageInfo, features, packageState.isApk)
    featureState.emitFeatures(detailFeatures)
  }

  fun initAbiInfo(packageInfo: PackageInfo, apkAnalyticsMode: Boolean) = viewModelScope.launch(Dispatchers.IO) {
    getAppDetailAbiUseCase(packageInfo, apkAnalyticsMode)?.let {
      featureState.emitAbiBundle(it)
    }
  }

  fun initAbiInfo(apkPreviewInfo: ApkPreviewInfo) = viewModelScope.launch(Dispatchers.IO) {
    getAppDetailAbiUseCase(apkPreviewInfo)?.let {
      featureState.emitAbiBundle(it)
    }
  }

  fun filterDetailItems(
    items: List<LibStringItemChip>,
    searchWords: String?,
    process: String?
  ): List<LibStringItemChip> {
    return filterAppDetailItemsUseCase(items, searchWords, process)
  }

  fun filterPermissionDetailItems(
    items: List<LibStringItemChip>,
    searchWords: String?,
    process: String?
  ): List<LibStringItemChip> {
    return filterAppDetailItemsUseCase.filterPermissions(items, searchWords, process)
  }

  fun sortDetailItems(items: List<LibStringItemChip>, @LibType type: Int): List<LibStringItemChip> {
    return sortAppDetailItemsUseCase(items, type, isSortByLibMode())
  }

  private fun isSortByLibMode(): Boolean {
    return appDetailSettingsRepository.sortMode == MODE_SORT_BY_LIB
  }
}
