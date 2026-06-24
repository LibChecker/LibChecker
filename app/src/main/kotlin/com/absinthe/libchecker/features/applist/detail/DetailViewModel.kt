package com.absinthe.libchecker.features.applist.detail

import android.content.pm.PackageInfo
import android.net.Uri
import android.util.SparseArray
import androidx.core.util.forEach
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.constant.AbilityType
import com.absinthe.libchecker.database.entity.Features
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.AppBundleSplitItem
import com.absinthe.libchecker.domain.app.AppDetailAbi
import com.absinthe.libchecker.domain.app.AppDetailAbiLabelData
import com.absinthe.libchecker.domain.app.AppDetailHeaderExtraInfo
import com.absinthe.libchecker.domain.app.AppDetailSettingsRepository
import com.absinthe.libchecker.domain.app.AppIconItem
import com.absinthe.libchecker.domain.app.AppListRepository
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
import com.absinthe.libchecker.domain.app.GetAppDetailAbilityChipsUseCase
import com.absinthe.libchecker.domain.app.GetAppDetailComponentChipsUseCase
import com.absinthe.libchecker.domain.app.GetAppDetailDexChipsUseCase
import com.absinthe.libchecker.domain.app.GetAppDetailFeaturesUseCase
import com.absinthe.libchecker.domain.app.GetAppDetailMetadataChipsUseCase
import com.absinthe.libchecker.domain.app.GetAppDetailNativeLibrariesUseCase
import com.absinthe.libchecker.domain.app.GetAppDetailPackageUseCase
import com.absinthe.libchecker.domain.app.GetAppDetailPermissionChipsUseCase
import com.absinthe.libchecker.domain.app.GetAppDetailSignatureChipsUseCase
import com.absinthe.libchecker.domain.app.GetAppDetailStaticLibraryChipsUseCase
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
import com.absinthe.libchecker.features.applist.LocatedCount
import com.absinthe.libchecker.features.applist.MODE_SORT_BY_LIB
import com.absinthe.libchecker.features.applist.MODE_SORT_BY_SIZE
import com.absinthe.libchecker.features.statistics.bean.LibStringItem
import com.absinthe.libchecker.features.statistics.bean.LibStringItemChip
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.apk.ApkPreviewInfo
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

class DetailViewModel(
  private val appListRepository: AppListRepository,
  private val getAppDetailPackage: GetAppDetailPackageUseCase,
  private val getAlternativeLaunchItemsUseCase: GetAlternativeLaunchItemsUseCase,
  private val getAppInfoActionsUseCase: GetAppInfoActionsUseCase,
  private val getAppLaunchActionUseCase: GetAppLaunchActionUseCase,
  private val getAppBundleItemsUseCase: GetAppBundleItemsUseCase,
  private val filterAppDetailItemsUseCase: FilterAppDetailItemsUseCase,
  private val getAppDetailAbiUseCase: GetAppDetailAbiUseCase,
  private val getAppDetailAbilityChipsUseCase: GetAppDetailAbilityChipsUseCase,
  private val getAppInstallSourceDetailsUseCase: GetAppInstallSourceDetailsUseCase,
  private val getAppDetailComponentChipsUseCase: GetAppDetailComponentChipsUseCase,
  private val getAppDetailDexChipsUseCase: GetAppDetailDexChipsUseCase,
  private val getAppDetailFeaturesUseCase: GetAppDetailFeaturesUseCase,
  private val getAppDetailMetadataChipsUseCase: GetAppDetailMetadataChipsUseCase,
  private val getAppDetailNativeLibrariesUseCase: GetAppDetailNativeLibrariesUseCase,
  private val getAppDetailStaticLibraryChipsUseCase: GetAppDetailStaticLibraryChipsUseCase,
  private val buildAppDetailAbiLabelDataUseCase: BuildAppDetailAbiLabelDataUseCase,
  private val buildAppDetailHeaderExtraInfoUseCase: BuildAppDetailHeaderExtraInfoUseCase,
  private val buildAppDetailHeaderTitleDataUseCase: BuildAppDetailHeaderTitleDataUseCase,
  private val extractNativeLibraryUseCase: ExtractNativeLibraryUseCase,
  private val prepareAppPackageShareFileUseCase: PrepareAppPackageShareFileUseCase,
  private val exportAppPackageShareFileUseCase: ExportAppPackageShareFileUseCase,
  private val getApkPreviewInfoUseCase: GetApkPreviewInfoUseCase,
  private val getAppDetailPermissionChipsUseCase: GetAppDetailPermissionChipsUseCase,
  private val getAppDetailSignatureChipsUseCase: GetAppDetailSignatureChipsUseCase,
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
  private val buildPackageComparisonSnapshotItemUseCase: BuildPackageComparisonSnapshotItemUseCase
) : ViewModel() {
  private var allNativeLibItems: Map<String, List<LibStringItem>> = emptyMap()
  val nativeLibTabs: MutableStateFlow<Collection<String>?> = MutableStateFlow(null)
  val nativeLibItems: MutableStateFlow<List<LibStringItemChip>?> = MutableStateFlow(null)
  val staticLibItems: MutableStateFlow<List<LibStringItemChip>?> = MutableStateFlow(null)
  val metaDataItems: MutableStateFlow<List<LibStringItemChip>?> = MutableStateFlow(null)
  val permissionsItems: MutableStateFlow<List<LibStringItemChip>?> = MutableStateFlow(null)
  val dexLibItems: MutableStateFlow<List<LibStringItemChip>?> = MutableStateFlow(null)
  val signaturesLibItems: MutableStateFlow<List<LibStringItemChip>?> = MutableStateFlow(null)
  val componentsMap = SparseArray<MutableStateFlow<List<LibStringItemChip>?>>()
  val abilitiesMap = SparseArray<MutableStateFlow<List<LibStringItemChip>?>>()
  val itemsCountStateFlow: MutableStateFlow<LocatedCount> = MutableStateFlow(LocatedCount(0, 0))
  val processToolIconVisibilityStateFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)
  val processMapStateFlow = MutableStateFlow<Map<String, Int>>(emptyMap())
  val itemsCountList = MutableList(12) { 0 }
  val is64Bit = MutableStateFlow<Boolean?>(null)

  var isApk = false
  var isApkPreview = false

  var queriedText: String? = null
  var queriedProcess: String? = null
  var processesMap: Map<String, Int> = mapOf()
  var nativeSourceMap: Map<String, Int> = mapOf()
  var appIcons: List<AppIconItem> = listOf()

  lateinit var packageInfo: PackageInfo
    private set
  var apkPreviewInfo: ApkPreviewInfo? = null
  val packageInfoStateFlow = MutableStateFlow<PackageInfo?>(null)

  private val _featuresFlow = MutableSharedFlow<VersionedFeature>()
  val featuresFlow = _featuresFlow.asSharedFlow()

  val abiBundleStateFlow = MutableStateFlow<AppDetailAbi?>(null)

  init {
    componentsMap.put(SERVICE, MutableStateFlow(null))
    componentsMap.put(ACTIVITY, MutableStateFlow(null))
    componentsMap.put(RECEIVER, MutableStateFlow(null))
    componentsMap.put(PROVIDER, MutableStateFlow(null))
  }

  fun initPackageInfo(pi: PackageInfo) {
    packageInfo = pi
    viewModelScope.launch {
      packageInfoStateFlow.emit(pi)
    }
  }

  fun isPackageInfoAvailable(): Boolean {
    return this::packageInfo.isInitialized
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
      apkPreviewInfo = apkPreviewInfo,
      isApkPreview = isApkPreview,
      showAndroidVersion = showAndroidVersion
    )
  }

  fun buildAppDetailHeaderTitleData(
    packageInfo: PackageInfo,
    apkAnalyticsMode: Boolean
  ) = buildAppDetailHeaderTitleDataUseCase(
    packageInfo = packageInfo,
    apkPreviewInfo = apkPreviewInfo,
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

  suspend fun extractNativeLibrary(item: LibStringItem) = extractNativeLibraryUseCase(packageInfo, item, isApkPreview)

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
    initSoAnalysisJob?.cancel()
    initStaticJob?.cancel()
    initMetaDataJob?.cancel()
    initPermissionJob?.cancel()
    initDexJob?.cancel()
    initSignaturesJob?.cancel()
    initComponentsJob?.cancel()
    allNativeLibItems = emptyMap()
    nativeLibTabs.value = null
    nativeLibItems.value = null
    staticLibItems.value = null
    metaDataItems.value = null
    permissionsItems.value = null
    dexLibItems.value = null
    signaturesLibItems.value = null
    componentsMap.forEach { key, value -> value.value = null }
    abilitiesMap.forEach { key, value -> value.value = null }
    itemsCountStateFlow.value = LocatedCount(0, 0)
    processToolIconVisibilityStateFlow.value = false
    processMapStateFlow.value = emptyMap()
    itemsCountList.fill(0)
  }

  private var initSoAnalysisJob: Job? = null

  fun initSoAnalysisData() {
    initSoAnalysisJob = launchDetailDataJob(
      currentJob = initSoAnalysisJob,
      hasData = nativeLibItems.value != null
    ) {
      val abi = (abiBundleStateFlow.value ?: abiBundleStateFlow.filterNotNull().first()).abi
      val nativeLibraries = getAppDetailNativeLibrariesUseCase(
        packageInfo = packageInfo,
        apkPreviewInfo = apkPreviewInfo,
        isApk = isApk,
        isApkPreview = isApkPreview,
        abi = abi
      )
      allNativeLibItems = nativeLibraries.itemsByAbi

      nativeLibTabs.emit(allNativeLibItems.keys)
      if (allNativeLibItems.isEmpty()) {
        nativeLibItems.emit(emptyList())
      }

      if (nativeLibraries.selectedAbiSupports16KbPageSize) {
        _featuresFlow.emit(VersionedFeature(Features.Ext.ELF_PAGE_SIZE_16KB))
      }
    }
  }

  fun loadSoAnalysisData(tab: String) {
    allNativeLibItems[tab]?.let {
      viewModelScope.launch(Dispatchers.IO) {
        nativeLibItems.emit(
          getAppDetailNativeLibrariesUseCase.buildChipList(
            packageInfo = packageInfo,
            apkPreviewInfo = apkPreviewInfo,
            isApkPreview = isApkPreview,
            items = it,
            sortBySize = appDetailSettingsRepository.sortMode == MODE_SORT_BY_SIZE
          )
        )
      }
    }
  }

  private var initStaticJob: Job? = null

  fun initStaticData() {
    initStaticJob = launchDetailDataJob(
      currentJob = initStaticJob,
      hasData = staticLibItems.value != null
    ) {
      staticLibItems.emit(
        getAppDetailStaticLibraryChipsUseCase(
          packageInfo = packageInfo,
          sortBySizeMode = appDetailSettingsRepository.sortMode == MODE_SORT_BY_SIZE
        )
      )
    }
  }

  private var initMetaDataJob: Job? = null

  fun initMetaDataData() {
    initMetaDataJob = launchDetailDataJob(
      currentJob = initMetaDataJob,
      hasData = metaDataItems.value != null
    ) {
      metaDataItems.emit(
        getAppDetailMetadataChipsUseCase(
          packageInfo = packageInfo,
          apkPreviewInfo = apkPreviewInfo,
          isApkPreview = isApkPreview
        )
      )
    }
  }

  private var initPermissionJob: Job? = null

  fun initPermissionData() {
    initPermissionJob = launchDetailDataJob(
      currentJob = initPermissionJob,
      hasData = permissionsItems.value != null
    ) {
      val permissions = getAppDetailPermissionChipsUseCase(
        packageInfo = packageInfo,
        apkPreviewInfo = apkPreviewInfo,
        isApk = isApk,
        isApkPreview = isApkPreview
      )
      permissionsItems.emit(permissions.items)

      if (permissions.hasLiveUpdateNotification) {
        _featuresFlow.emit(VersionedFeature(Features.LIVE_UPDATE_NOTIFICATION))
      }
    }
  }

  var initDexJob: Job? = null

  fun initDexData() {
    initDexJob = launchDetailDataJob(
      currentJob = initDexJob,
      hasData = dexLibItems.value != null
    ) {
      val list = getAppDetailDexChipsUseCase(
        packageInfo = packageInfo,
        sortBySizeMode = appDetailSettingsRepository.sortMode == MODE_SORT_BY_SIZE
      )
      dexLibItems.emit(list)
    }
  }

  private var initSignaturesJob: Job? = null

  fun initSignatures() {
    initSignaturesJob = launchDetailDataJob(
      currentJob = initSignaturesJob,
      hasData = signaturesLibItems.value != null
    ) {
      signaturesLibItems.emit(getAppDetailSignatureChipsUseCase(packageInfo, isApk))
    }
  }

  private var initComponentsJob: Job? = null

  fun initComponentsData() {
    initComponentsJob = launchDetailDataJob(
      currentJob = initComponentsJob,
      hasData = hasComponentsData()
    ) {
      try {
        val components = getAppDetailComponentChipsUseCase(packageInfo, isApk)
        processesMap = components.processNames.associateWith { UiUtils.getRandomColor() }
        componentsMap[SERVICE]?.emit(components.services)
        componentsMap[ACTIVITY]?.emit(components.activities)
        componentsMap[RECEIVER]?.emit(components.receivers)
        componentsMap[PROVIDER]?.emit(components.providers)
      } catch (e: Exception) {
        Timber.e(e)
      }
    }
  }

  private fun hasComponentsData(): Boolean {
    return componentsMap[SERVICE]?.value != null &&
      componentsMap[ACTIVITY]?.value != null &&
      componentsMap[RECEIVER]?.value != null &&
      componentsMap[PROVIDER]?.value != null
  }

  private fun launchDetailDataJob(
    currentJob: Job?,
    hasData: Boolean,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    block: suspend CoroutineScope.() -> Unit
  ): Job? {
    if (currentJob?.isActive == true || hasData) {
      return currentJob
    }
    return viewModelScope.launch(dispatcher, block = block)
  }

  fun initComponentsDataInPreview() = viewModelScope.launch(Dispatchers.IO) {
    val previewInfo = apkPreviewInfo ?: return@launch
    val components = getAppDetailComponentChipsUseCase(previewInfo)
    componentsMap[SERVICE]?.emit(components.services)
    componentsMap[ACTIVITY]?.emit(components.activities)
    componentsMap[RECEIVER]?.emit(components.receivers)
    componentsMap[PROVIDER]?.emit(components.providers)
  }

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

  fun initAbilities(packageName: String) = viewModelScope.launch(Dispatchers.IO) {
    abilitiesMap.put(AbilityType.PAGE, MutableStateFlow(null))
    abilitiesMap.put(AbilityType.SERVICE, MutableStateFlow(null))
    abilitiesMap.put(AbilityType.WEB, MutableStateFlow(null))
    abilitiesMap.put(AbilityType.DATA, MutableStateFlow(null))

    runCatching {
      getAppDetailAbilityChipsUseCase(packageName)
    }.onSuccess { abilityChips ->
      abilityChips.forEach { (type, items) ->
        abilitiesMap[type]?.emit(items)
      }
    }.onFailure {
      Timber.e(it)
    }
  }

  fun emitFeature(feature: VersionedFeature) = viewModelScope.launch {
    _featuresFlow.emit(feature)
  }

  suspend fun getRelatedAppListItem(packageName: String): RelatedAppListItem? {
    return getRelatedAppListItemUseCase(packageName)
  }

  fun buildRelatedAppDisplayData(packageName: String, relatedApp: RelatedAppListItem) = buildRelatedAppDisplayDataUseCase(packageName, relatedApp)

  fun buildSignatureDetailItems(detail: String) = buildSignatureDetailItemsUseCase(detail)

  fun initFeatures(packageInfo: PackageInfo, features: Int) = viewModelScope.launch(Dispatchers.IO) {
    Timber.d("initFeatures: features = $features")

    val detailFeatures = getAppDetailFeaturesUseCase(packageInfo, features, isApk)
    appIcons = detailFeatures.appIcons
    detailFeatures.features.forEach {
      _featuresFlow.emit(it)
    }
  }

  fun initAbiInfo(packageInfo: PackageInfo, apkAnalyticsMode: Boolean) = viewModelScope.launch(Dispatchers.IO) {
    getAppDetailAbiUseCase(packageInfo, apkAnalyticsMode)?.let {
      abiBundleStateFlow.emit(it)
    }
  }

  fun initAbiInfo(apkPreviewInfo: ApkPreviewInfo) = viewModelScope.launch(Dispatchers.IO) {
    getAppDetailAbiUseCase(apkPreviewInfo)?.let {
      abiBundleStateFlow.emit(it)
    }
  }

  fun updateProcessMap(map: Map<String, Int>) = viewModelScope.launch {
    processMapStateFlow.emit(map)
  }

  fun updateProcessToolIconVisibility(visible: Boolean) = viewModelScope.launch {
    processToolIconVisibilityStateFlow.emit(visible)
  }

  fun updateItemsCountStateFlow(locate: Int, count: Int) = viewModelScope.launch {
    itemsCountStateFlow.value = LocatedCount(locate, count)
    itemsCountList[locate] = count
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
    return sortAppDetailItemsUseCase(items, type, appDetailSettingsRepository.sortMode == MODE_SORT_BY_LIB)
  }
}
