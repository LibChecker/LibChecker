package com.absinthe.libchecker.domain.app.detail.presentation

import android.content.pm.PackageInfo
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.detail.abi.AppDetailAbiLabelData
import com.absinthe.libchecker.domain.app.detail.action.AppElfDetail
import com.absinthe.libchecker.domain.app.detail.action.AppLaunchAction
import com.absinthe.libchecker.domain.app.detail.action.AppPackageShareAction
import com.absinthe.libchecker.domain.app.detail.action.AppPackageShareFile
import com.absinthe.libchecker.domain.app.detail.action.DetailItemDialogRequest
import com.absinthe.libchecker.domain.app.detail.action.DetailItemLongClickActions
import com.absinthe.libchecker.domain.app.detail.feature.AppDetailFeatureItemData
import com.absinthe.libchecker.domain.app.detail.header.AppDetailHeaderExtraInfo
import com.absinthe.libchecker.domain.app.detail.model.AppBundleItem
import com.absinthe.libchecker.domain.app.detail.model.AppInstallSourceBottomSheetDisplay
import com.absinthe.libchecker.domain.app.detail.model.AppInstallSourceRequesterAccess
import com.absinthe.libchecker.domain.app.detail.model.AppPropItem
import com.absinthe.libchecker.domain.app.detail.model.LibStringItem
import com.absinthe.libchecker.domain.app.detail.model.LibStringItemChip
import com.absinthe.libchecker.domain.app.detail.navigation.DetailReferenceNavigation
import com.absinthe.libchecker.domain.app.detail.packageinfo.GetAppDetailPackageUseCase
import com.absinthe.libchecker.domain.app.detail.presentation.DetailActionLoader
import com.absinthe.libchecker.domain.app.detail.presentation.DetailFilterController
import com.absinthe.libchecker.domain.app.detail.presentation.DetailPackageLoader
import com.absinthe.libchecker.domain.app.detail.presentation.DetailPackageState
import com.absinthe.libchecker.domain.app.detail.presentation.content.DetailContentLoader
import com.absinthe.libchecker.domain.app.detail.statistics.AnalyzeAppStatisticRulesUseCase
import com.absinthe.libchecker.domain.app.detail.statistics.AppStatisticAnalysisState
import com.absinthe.libchecker.domain.app.model.VersionedFeature
import com.absinthe.libchecker.domain.app.packageinfo.PrepareApkAnalysisPackageUseCase
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.utils.apk.ApkPreviewInfo
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class DetailViewModel(
  private val detailActionLoader: DetailActionLoader,
  private val detailContentLoader: DetailContentLoader,
  private val detailFilterController: DetailFilterController,
  private val detailPresentationLoader: DetailPresentationLoader,
  private val detailPackageLoader: DetailPackageLoader,
  private val analyzeAppStatisticRules: AnalyzeAppStatisticRulesUseCase
) : ViewModel() {
  val contentState = detailContentLoader.contentState
  val featureState = detailPresentationLoader.featureState
  val filterState = detailFilterController.filterState
  private val _packageLoadResults = MutableSharedFlow<PackageLoadResult>()
  val packageLoadResults: SharedFlow<PackageLoadResult> = _packageLoadResults.asSharedFlow()
  private val _apkAnalysisPackageResults = MutableSharedFlow<ApkAnalysisPackageResult>()
  val apkAnalysisPackageResults: SharedFlow<ApkAnalysisPackageResult> = _apkAnalysisPackageResults.asSharedFlow()
  private val _apkPreviewResults = MutableSharedFlow<ApkPreviewResult>()
  val apkPreviewResults: SharedFlow<ApkPreviewResult> = _apkPreviewResults.asSharedFlow()
  private val _appInstallSourceDetailsResults = MutableSharedFlow<AppInstallSourceDetailsResult>()
  val appInstallSourceDetailsResults: SharedFlow<AppInstallSourceDetailsResult> =
    _appInstallSourceDetailsResults.asSharedFlow()
  private val _appPackageShareActionResults = MutableSharedFlow<AppPackageShareActionResult>()
  val appPackageShareActionResults: SharedFlow<AppPackageShareActionResult> =
    _appPackageShareActionResults.asSharedFlow()
  private val _appPackageShareExportResults = MutableSharedFlow<AppPackageShareExportResult>()
  val appPackageShareExportResults: SharedFlow<AppPackageShareExportResult> =
    _appPackageShareExportResults.asSharedFlow()
  private val _nativeLibraryExtractionResults = MutableSharedFlow<NativeLibraryExtractionResult>()
  val nativeLibraryExtractionResults: SharedFlow<NativeLibraryExtractionResult> =
    _nativeLibraryExtractionResults.asSharedFlow()
  private val _elfDetailResults = MutableSharedFlow<ElfDetailResult>()
  val elfDetailResults: SharedFlow<ElfDetailResult> = _elfDetailResults.asSharedFlow()
  private val _appStatisticAnalysisState = MutableStateFlow<AppStatisticAnalysisState>(
    AppStatisticAnalysisState.Idle
  )
  val appStatisticAnalysisState: StateFlow<AppStatisticAnalysisState> =
    _appStatisticAnalysisState.asStateFlow()
  private val _onlineStatisticRulesAvailable = MutableStateFlow(false)
  val onlineStatisticRulesAvailable: StateFlow<Boolean> =
    _onlineStatisticRulesAvailable.asStateFlow()
  private var packageLoadJob: Job? = null
  private var apkAnalysisPackageJob: Job? = null
  private var apkPreviewJob: Job? = null
  private var appInstallSourceDetailsJob: Job? = null
  private var appPackageShareActionJob: Job? = null
  private var appPackageShareExportJob: Job? = null
  private var nativeLibraryExtractionJob: Job? = null
  private var elfDetailJob: Job? = null
  private var appStatisticAnalysisJob: Job? = null
  private var onlineStatisticRulesAvailabilityJob: Job? = null
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

  fun isPackageInfoAvailable(): Boolean {
    return detailPackageLoader.isPackageInfoAvailable()
  }

  fun loadAppDetailPackage(packageName: String) {
    packageLoadJob?.cancel()
    packageLoadJob = viewModelScope.launch {
      _packageLoadResults.emit(
        PackageLoadResult(
          packageName = packageName,
          result = detailPackageLoader.loadAppDetailPackage(packageName)
        )
      )
    }
  }

  data class PackageLoadResult(
    val packageName: String,
    val result: GetAppDetailPackageUseCase.Result
  )

  fun loadApkAnalysisPackage(cacheDir: File, uri: Uri) {
    apkAnalysisPackageJob?.cancel()
    apkPreviewJob?.cancel()
    detailPackageLoader.startApkMode()
    detailPackageLoader.clearApkPreviewInfo()
    apkAnalysisPackageJob = viewModelScope.launch {
      _apkAnalysisPackageResults.emit(
        ApkAnalysisPackageResult(
          result = detailPackageLoader.prepareApkAnalysisPackage(cacheDir, uri)
        )
      )
    }
  }

  data class ApkAnalysisPackageResult(
    val result: PrepareApkAnalysisPackageUseCase.Result
  )

  fun loadApkPreview(url: String) {
    apkPreviewJob?.cancel()
    apkAnalysisPackageJob?.cancel()
    detailPackageLoader.startApkPreviewMode()
    detailPackageLoader.clearApkPreviewInfo()
    apkPreviewJob = viewModelScope.launch {
      val result = detailPackageLoader.getApkPreviewInfo(url)
      result.getOrNull()?.let(detailPackageLoader::setApkPreviewInfo)
      _apkPreviewResults.emit(ApkPreviewResult(url, result))
    }
  }

  data class ApkPreviewResult(
    val url: String,
    val result: Result<ApkPreviewInfo>
  )

  fun loadAppInstallSourceDetails(
    packageName: String,
    requesterAccess: AppInstallSourceRequesterAccess
  ) {
    appInstallSourceDetailsJob?.cancel()
    appInstallSourceDetailsJob = viewModelScope.launch {
      _appInstallSourceDetailsResults.emit(
        AppInstallSourceDetailsResult(
          packageName = packageName,
          display = detailActionLoader.getAppInstallSourceBottomSheetDisplay(
            packageName,
            requesterAccess
          )
        )
      )
    }
  }

  data class AppInstallSourceDetailsResult(
    val packageName: String,
    val display: AppInstallSourceBottomSheetDisplay?
  )

  fun prepareAppPackageShareAction(
    cacheDir: File,
    packageName: String,
    target: AppPackageShareTarget
  ) {
    appPackageShareActionJob?.cancel()
    appPackageShareActionJob = viewModelScope.launch {
      _appPackageShareActionResults.emit(
        AppPackageShareActionResult(
          packageName = packageName,
          target = target,
          result = runCatching {
            detailActionLoader.prepareAppPackageShareAction(cacheDir, packageName)
          }
        )
      )
    }
  }

  enum class AppPackageShareTarget {
    SHARE,
    EXPORT
  }

  data class AppPackageShareActionResult(
    val packageName: String,
    val target: AppPackageShareTarget,
    val result: Result<AppPackageShareAction>
  )

  fun exportAppPackageShareFile(
    shareFile: AppPackageShareFile,
    destinationUri: Uri
  ) {
    appPackageShareExportJob?.cancel()
    appPackageShareExportJob = viewModelScope.launch {
      _appPackageShareExportResults.emit(
        AppPackageShareExportResult(
          shareFile = shareFile,
          destinationUri = destinationUri,
          result = runCatching {
            detailActionLoader.exportAppPackageShareFile(shareFile, destinationUri)
            Unit
          }
        )
      )
    }
  }

  data class AppPackageShareExportResult(
    val shareFile: AppPackageShareFile,
    val destinationUri: Uri,
    val result: Result<Unit>
  )

  fun extractNativeLibrary(item: LibStringItem) {
    nativeLibraryExtractionJob?.cancel()
    nativeLibraryExtractionJob = viewModelScope.launch {
      _nativeLibraryExtractionResults.emit(
        NativeLibraryExtractionResult(
          item = item,
          result = detailActionLoader.extractNativeLibrary(packageState, item)
        )
      )
    }
  }

  data class NativeLibraryExtractionResult(
    val item: LibStringItem,
    val result: Result<Unit>
  )

  fun loadElfDetail(packageName: String, elfPath: String) {
    elfDetailJob?.cancel()
    elfDetailJob = viewModelScope.launch {
      _elfDetailResults.emit(
        ElfDetailResult(
          packageName = packageName,
          elfPath = elfPath,
          result = runCatching {
            detailActionLoader.getElfDetail(packageName, elfPath)
          }
        )
      )
    }
  }

  data class ElfDetailResult(
    val packageName: String,
    val elfPath: String,
    val result: Result<AppElfDetail?>
  )

  fun analyzeOnlineStatistics() {
    if (!isPackageInfoAvailable()) return
    appStatisticAnalysisJob?.cancel()
    _appStatisticAnalysisState.value = AppStatisticAnalysisState.Loading(0)
    val currentPackageInfo = packageInfo
    appStatisticAnalysisJob = viewModelScope.launch(Dispatchers.IO) {
      try {
        val analyses = analyzeAppStatisticRules(currentPackageInfo) { progress ->
          _appStatisticAnalysisState.value = AppStatisticAnalysisState.Loading(
            progress.coerceIn(0, 100)
          )
        }
        _appStatisticAnalysisState.value = if (analyses.isEmpty()) {
          AppStatisticAnalysisState.Empty
        } else {
          AppStatisticAnalysisState.Results(analyses)
        }
      } catch (error: CancellationException) {
        throw error
      } catch (error: Throwable) {
        Timber.e(error, "Unable to analyze online statistic rules")
        _appStatisticAnalysisState.value = AppStatisticAnalysisState.Error
      }
    }
  }

  fun refreshOnlineStatisticRulesAvailability() {
    onlineStatisticRulesAvailabilityJob?.cancel()
    onlineStatisticRulesAvailabilityJob = viewModelScope.launch(Dispatchers.IO) {
      _onlineStatisticRulesAvailable.value = try {
        analyzeAppStatisticRules.hasSelectedRules()
      } catch (error: CancellationException) {
        throw error
      } catch (error: Throwable) {
        Timber.e(error, "Unable to check online statistic rule availability")
        false
      }
    }
  }

  fun cancelOnlineStatisticAnalysis() {
    appStatisticAnalysisJob?.cancel()
    appStatisticAnalysisJob = null
    if (_appStatisticAnalysisState.value is AppStatisticAnalysisState.Loading) {
      _appStatisticAnalysisState.value = AppStatisticAnalysisState.Idle
    }
  }

  fun buildAppDetailAbiLabelData(
    abi: Int,
    abiSet: Collection<Int>,
    apkAnalyticsMode: Boolean
  ): AppDetailAbiLabelData {
    return detailPresentationLoader.buildAppDetailAbiLabelData(abi, abiSet, apkAnalyticsMode)
  }

  suspend fun buildAppDetailHeaderExtraInfo(
    packageInfo: PackageInfo,
    showAndroidVersion: Boolean
  ): AppDetailHeaderExtraInfo {
    return detailPresentationLoader.buildAppDetailHeaderExtraInfo(packageState, packageInfo, showAndroidVersion)
  }

  fun buildAppDetailHeaderTitleData(
    packageInfo: PackageInfo,
    apkAnalyticsMode: Boolean
  ) = detailPresentationLoader.buildAppDetailHeaderTitleData(packageState, packageInfo, apkAnalyticsMode)

  fun buildAppDetailFeatureItem(
    feature: VersionedFeature,
    currentFeatureCount: Int,
    apkAnalyticsMode: Boolean,
    canShowInstallSource: Boolean,
    canShowAppIcons: Boolean
  ): AppDetailFeatureItemData? {
    return detailPresentationLoader.buildAppDetailFeatureItem(
      feature = feature,
      currentFeatureCount = currentFeatureCount,
      apkAnalyticsMode = apkAnalyticsMode,
      canShowInstallSource = canShowInstallSource,
      canShowAppIcons = canShowAppIcons
    )
  }

  suspend fun getAppBundleItems(packageInfo: PackageInfo): List<AppBundleItem> {
    return detailActionLoader.getAppBundleItems(packageInfo)
  }

  suspend fun getAppInfoBottomSheetState(packageName: String?) = detailActionLoader.getAppInfoBottomSheetState(packageName)

  suspend fun getAppInfoPrimaryActions(packageName: String?) = detailActionLoader.getAppInfoPrimaryActions(packageName)

  suspend fun getAppLaunchAction(packageName: String?) = detailActionLoader.getAppLaunchAction(packageName)

  suspend fun getAlternativeLaunchItems(packageName: String) = detailActionLoader.getAlternativeLaunchItems(packageName)

  suspend fun getXposedInfoBottomSheetDisplay(packageName: String) = detailActionLoader.getXposedInfoBottomSheetDisplay(packageName)

  suspend fun getAppManifestProperties(
    packageInfo: PackageInfo?,
    properties: Map<String, String>?
  ): List<AppPropItem> {
    return detailActionLoader.getAppManifestProperties(packageInfo, properties)
  }

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
    appStatisticAnalysisJob?.cancel()
    appStatisticAnalysisJob = null
    _appStatisticAnalysisState.value = AppStatisticAnalysisState.Idle
    detailContentLoader.reset()
    detailFilterController.reset()
    detailPresentationLoader.reset()
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
    isValidLib: Boolean,
    preferredLocale: String
  ) = detailActionLoader.getLibraryDetailDialogData(
    libName,
    type,
    regexName,
    isValidLib,
    preferredLocale
  )

  suspend fun getOverlayDetailBottomSheetResult(item: LCItem) = detailActionLoader.getOverlayDetailBottomSheetResult(item)

  suspend fun getPermissionDetail(permissionName: String) = detailActionLoader.getPermissionDetail(permissionName)

  fun initAbilities(packageName: String) {
    detailContentLoader.initAbilities(viewModelScope, packageName)
  }

  fun emitFeature(feature: VersionedFeature) {
    detailPresentationLoader.emitFeature(viewModelScope, feature)
  }

  fun setFeatureLoading(loading: Boolean) {
    detailPresentationLoader.setLoading(viewModelScope, loading)
  }

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
    detailPresentationLoader.initFeatures(viewModelScope, packageState, packageInfo, features)
  }

  fun initAbiInfo(packageInfo: PackageInfo, apkAnalyticsMode: Boolean) {
    detailPresentationLoader.initAbiInfo(viewModelScope, packageInfo, apkAnalyticsMode)
  }

  fun initAbiInfo(apkPreviewInfo: ApkPreviewInfo) {
    detailPresentationLoader.initAbiInfo(viewModelScope, apkPreviewInfo)
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

  suspend fun filterAndSortDetailItems(
    items: List<LibStringItemChip>,
    searchWords: String?,
    process: String?,
    @LibType type: Int
  ): List<LibStringItemChip> = withContext(Dispatchers.Default) {
    val filteredItems = if (type == PERMISSION) {
      detailFilterController.filterPermissionDetailItems(items, searchWords, process)
    } else {
      detailFilterController.filterDetailItems(items, searchWords, process)
    }
    detailFilterController.sortDetailItems(filteredItems, type)
  }

  suspend fun sortDetailItemsForDisplay(
    items: List<LibStringItemChip>,
    @LibType type: Int
  ): List<LibStringItemChip> = withContext(Dispatchers.Default) {
    detailFilterController.sortDetailItems(items, type)
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
