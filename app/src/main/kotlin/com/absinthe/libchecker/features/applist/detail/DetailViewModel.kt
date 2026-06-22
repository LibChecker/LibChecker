package com.absinthe.libchecker.features.applist.detail

import android.content.Context
import android.content.pm.PackageInfo
import android.util.SparseArray
import androidx.core.util.forEach
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.DEX
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.api.bean.LibDetailBean
import com.absinthe.libchecker.constant.AbilityType
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.database.RulesRepository
import com.absinthe.libchecker.database.entity.Features
import com.absinthe.libchecker.domain.app.AppBundleSplitItem
import com.absinthe.libchecker.domain.app.AppDetailAbi
import com.absinthe.libchecker.domain.app.AppDetailPackageSize
import com.absinthe.libchecker.domain.app.AppIconItem
import com.absinthe.libchecker.domain.app.AppListRepository
import com.absinthe.libchecker.domain.app.AppManifestProperty
import com.absinthe.libchecker.domain.app.GetApkPreviewInfoUseCase
import com.absinthe.libchecker.domain.app.GetAppBundleItemsUseCase
import com.absinthe.libchecker.domain.app.GetAppDetailAbiUseCase
import com.absinthe.libchecker.domain.app.GetAppDetailComponentChipsUseCase
import com.absinthe.libchecker.domain.app.GetAppDetailFeaturesUseCase
import com.absinthe.libchecker.domain.app.GetAppDetailMetadataChipsUseCase
import com.absinthe.libchecker.domain.app.GetAppDetailNativeLibrariesUseCase
import com.absinthe.libchecker.domain.app.GetAppDetailPackageSizeUseCase
import com.absinthe.libchecker.domain.app.GetAppDetailPackageUseCase
import com.absinthe.libchecker.domain.app.GetAppDetailPermissionChipsUseCase
import com.absinthe.libchecker.domain.app.GetAppDetailSignatureChipsUseCase
import com.absinthe.libchecker.domain.app.GetAppDetailStaticLibraryChipsUseCase
import com.absinthe.libchecker.domain.app.GetAppInfoActionsUseCase
import com.absinthe.libchecker.domain.app.GetAppInstallSourceDetailsUseCase
import com.absinthe.libchecker.domain.app.GetAppManifestPropertiesUseCase
import com.absinthe.libchecker.domain.app.GetArchivePackageInfoUseCase
import com.absinthe.libchecker.domain.app.GetInstalledAppComparisonPackageUseCase
import com.absinthe.libchecker.domain.app.GetLibraryDetailUseCase
import com.absinthe.libchecker.domain.app.GetRelatedAppListItemUseCase
import com.absinthe.libchecker.domain.app.HasInstalledStaticLibrariesUseCase
import com.absinthe.libchecker.domain.app.RelatedAppListItem
import com.absinthe.libchecker.domain.app.SortAppDetailItemsUseCase
import com.absinthe.libchecker.domain.app.VersionedFeature
import com.absinthe.libchecker.domain.snapshot.BuildPackageComparisonSnapshotItemUseCase
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.features.applist.LocatedCount
import com.absinthe.libchecker.features.applist.MODE_SORT_BY_LIB
import com.absinthe.libchecker.features.applist.MODE_SORT_BY_SIZE
import com.absinthe.libchecker.features.applist.detail.bean.StatefulComponent
import com.absinthe.libchecker.features.statistics.bean.DISABLED
import com.absinthe.libchecker.features.statistics.bean.EXPORTED
import com.absinthe.libchecker.features.statistics.bean.LibStringItem
import com.absinthe.libchecker.features.statistics.bean.LibStringItemChip
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.apk.ApkPreviewInfo
import com.absinthe.libchecker.utils.harmony.ApplicationDelegate
import com.absinthe.rulesbundle.Rule
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ohos.bundle.AbilityInfo
import ohos.bundle.IBundleManager
import timber.log.Timber

class DetailViewModel(
  private val appListRepository: AppListRepository,
  private val getAppDetailPackage: GetAppDetailPackageUseCase,
  private val getAppInfoActionsUseCase: GetAppInfoActionsUseCase,
  private val getAppBundleItemsUseCase: GetAppBundleItemsUseCase,
  private val getAppDetailAbiUseCase: GetAppDetailAbiUseCase,
  private val getAppInstallSourceDetailsUseCase: GetAppInstallSourceDetailsUseCase,
  private val getAppDetailComponentChipsUseCase: GetAppDetailComponentChipsUseCase,
  private val getAppDetailFeaturesUseCase: GetAppDetailFeaturesUseCase,
  private val getAppDetailMetadataChipsUseCase: GetAppDetailMetadataChipsUseCase,
  private val getAppDetailNativeLibrariesUseCase: GetAppDetailNativeLibrariesUseCase,
  private val getAppDetailStaticLibraryChipsUseCase: GetAppDetailStaticLibraryChipsUseCase,
  private val getAppDetailPackageSizeUseCase: GetAppDetailPackageSizeUseCase,
  private val getApkPreviewInfoUseCase: GetApkPreviewInfoUseCase,
  private val getAppDetailPermissionChipsUseCase: GetAppDetailPermissionChipsUseCase,
  private val getAppDetailSignatureChipsUseCase: GetAppDetailSignatureChipsUseCase,
  private val getAppManifestPropertiesUseCase: GetAppManifestPropertiesUseCase,
  private val getArchivePackageInfoUseCase: GetArchivePackageInfoUseCase,
  private val getInstalledAppComparisonPackageUseCase: GetInstalledAppComparisonPackageUseCase,
  private val hasInstalledStaticLibrariesUseCase: HasInstalledStaticLibrariesUseCase,
  private val getLibraryDetailUseCase: GetLibraryDetailUseCase,
  private val getRelatedAppListItemUseCase: GetRelatedAppListItemUseCase,
  private val sortAppDetailItemsUseCase: SortAppDetailItemsUseCase,
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

  fun getAppDetailPackageSize(packageInfo: PackageInfo): AppDetailPackageSize {
    return getAppDetailPackageSizeUseCase(packageInfo, apkPreviewInfo, isApkPreview)
  }

  suspend fun getAppBundleItems(packageInfo: PackageInfo): List<AppBundleSplitItem> {
    return withContext(Dispatchers.IO) {
      getAppBundleItemsUseCase(packageInfo)
    }
  }

  suspend fun getAppInfoActions(packageName: String) = withContext(Dispatchers.IO) {
    getAppInfoActionsUseCase(packageName)
  }

  suspend fun getAppInstallSourceDetails(packageName: String) = withContext(Dispatchers.IO) {
    getAppInstallSourceDetailsUseCase(packageName)
  }

  suspend fun getApkPreviewInfo(url: String): Result<ApkPreviewInfo> {
    return withContext(Dispatchers.IO) {
      getApkPreviewInfoUseCase(url)
    }
  }

  suspend fun getAppManifestProperties(
    packageInfo: PackageInfo?,
    properties: Map<String, String>?
  ): List<AppManifestProperty> {
    return withContext(Dispatchers.IO) {
      getAppManifestPropertiesUseCase(packageInfo, properties)
    }
  }

  suspend fun getArchivePackageInfo(file: File): PackageInfo? {
    return withContext(Dispatchers.IO) {
      getArchivePackageInfoUseCase(file)
    }
  }

  suspend fun isInstalledAppComparisonAvailable(packageName: String): Boolean {
    return withContext(Dispatchers.IO) {
      getInstalledAppComparisonPackageUseCase.isAvailable(packageName)
    }
  }

  suspend fun hasInstalledStaticLibraries(packageName: String): Boolean {
    return withContext(Dispatchers.IO) {
      hasInstalledStaticLibrariesUseCase(packageName)
    }
  }

  suspend fun loadInstalledAppComparisonPackage(packageName: String): PackageInfo? {
    return withContext(Dispatchers.IO) {
      getInstalledAppComparisonPackageUseCase(packageName)
    }
  }

  suspend fun buildPackageComparisonSnapshotItem(
    basePackage: PackageInfo,
    analysisPackage: PackageInfo
  ): SnapshotDiffItem {
    return withContext(Dispatchers.IO) {
      buildPackageComparisonSnapshotItemUseCase(basePackage, analysisPackage)
    }
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
    if (initSoAnalysisJob?.isActive == true) {
      return
    }
    if (nativeLibItems.value != null) {
      return
    }
    initSoAnalysisJob = viewModelScope.launch(Dispatchers.IO) {
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
            sortBySize = GlobalValues.libSortMode == MODE_SORT_BY_SIZE
          )
        )
      }
    }
  }

  private var initStaticJob: Job? = null

  fun initStaticData() {
    if (initStaticJob?.isActive == true || staticLibItems.value != null) {
      return
    }
    initStaticJob = viewModelScope.launch(Dispatchers.IO) {
      staticLibItems.emit(
        getAppDetailStaticLibraryChipsUseCase(
          packageInfo = packageInfo,
          sortBySizeMode = GlobalValues.libSortMode == MODE_SORT_BY_SIZE
        )
      )
    }
  }

  private var initMetaDataJob: Job? = null

  fun initMetaDataData() {
    if (initMetaDataJob?.isActive == true || metaDataItems.value != null) {
      return
    }
    initMetaDataJob = viewModelScope.launch(Dispatchers.IO) {
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
    if (initPermissionJob?.isActive == true || permissionsItems.value != null) {
      return
    }
    initPermissionJob = viewModelScope.launch(Dispatchers.IO) {
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
    if (initDexJob?.isActive == true || dexLibItems.value != null) {
      return
    }
    initDexJob = viewModelScope.launch(Dispatchers.IO) {
      val list = getDexChipList()
      dexLibItems.emit(list)
    }
  }

  private var initSignaturesJob: Job? = null

  fun initSignatures() {
    if (initSignaturesJob?.isActive == true || signaturesLibItems.value != null) {
      return
    }
    initSignaturesJob = viewModelScope.launch {
      signaturesLibItems.emit(getAppDetailSignatureChipsUseCase(packageInfo, isApk))
    }
  }

  private var initComponentsJob: Job? = null

  fun initComponentsData() {
    if (initComponentsJob?.isActive == true) {
      return
    }
    if (
      componentsMap[SERVICE]?.value != null &&
      componentsMap[ACTIVITY]?.value != null &&
      componentsMap[RECEIVER]?.value != null &&
      componentsMap[PROVIDER]?.value != null
    ) {
      return
    }
    initComponentsJob = viewModelScope.launch(Dispatchers.IO) {
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

  fun initComponentsDataInPreview() = viewModelScope.launch(Dispatchers.IO) {
    val previewInfo = apkPreviewInfo ?: return@launch
    val components = getAppDetailComponentChipsUseCase(previewInfo)
    componentsMap[SERVICE]?.emit(components.services)
    componentsMap[ACTIVITY]?.emit(components.activities)
    componentsMap[RECEIVER]?.emit(components.receivers)
    componentsMap[PROVIDER]?.emit(components.providers)
  }

  suspend fun requestLibDetail(
    libName: String,
    @LibType type: Int,
    isRegex: Boolean = false
  ): LibDetailBean? = getLibraryDetailUseCase(libName, type, isRegex)

  private suspend fun getDexChipList(): List<LibStringItemChip> {
    Timber.d("getDexChipList")
    val list = try {
      PackageUtils.getDexList(packageInfo)
    } catch (e: Exception) {
      Timber.e(e)
      emptyList()
    }
    val chipList = mutableListOf<LibStringItemChip>()
    var rule: Rule?

    if (list.isEmpty()) {
      return chipList
    } else {
      list.forEach {
        rule = RulesRepository.getRule(it.name, DEX, true)
        chipList.add(LibStringItemChip(it, rule))
      }
      if (GlobalValues.libSortMode == MODE_SORT_BY_SIZE) {
        chipList.sortByDescending { it.item.name }
      } else {
        chipList.sortByDescending { it.rule != null }
      }
    }
    return chipList
  }

  fun initAbilities(context: Context, packageName: String) = viewModelScope.launch(Dispatchers.IO) {
    abilitiesMap.put(AbilityType.PAGE, MutableStateFlow(null))
    abilitiesMap.put(AbilityType.SERVICE, MutableStateFlow(null))
    abilitiesMap.put(AbilityType.WEB, MutableStateFlow(null))
    abilitiesMap.put(AbilityType.DATA, MutableStateFlow(null))

    try {
      ApplicationDelegate(context).iBundleManager?.getBundleInfo(
        packageName,
        IBundleManager.GET_BUNDLE_WITH_ABILITIES
      )?.abilityInfos?.let { abilities ->
        val pages = abilities.asSequence()
          .filter { it.type == AbilityInfo.AbilityType.PAGE }
          .map {
            StatefulComponent(
              it.className,
              it.enabled,
              false,
              it.process.removePrefix((it.bundleName))
            )
          }
          .toList()
        val services = abilities.asSequence()
          .filter { it.type == AbilityInfo.AbilityType.SERVICE }
          .map {
            StatefulComponent(
              it.className,
              it.enabled,
              false,
              it.process.removePrefix((it.bundleName))
            )
          }
          .toList()
        val webs = abilities.asSequence()
          .filter { it.type == AbilityInfo.AbilityType.WEB }
          .map {
            StatefulComponent(
              it.className,
              it.enabled,
              false,
              it.process.removePrefix((it.bundleName))
            )
          }
          .toList()
        val datas = abilities.asSequence()
          .filter { it.type == AbilityInfo.AbilityType.DATA }
          .map {
            StatefulComponent(
              it.className,
              it.enabled,
              false,
              it.process.removePrefix((it.bundleName))
            )
          }
          .toList()

        val transform: suspend (StatefulComponent) -> LibStringItemChip =
          { item ->
            val source = when {
              !item.enabled -> DISABLED
              item.exported -> EXPORTED
              else -> null
            }

            LibStringItemChip(
              LibStringItem(
                name = item.componentName,
                source = source
              ),
              null
            )
          }
        abilitiesMap[AbilityType.PAGE]?.emit(pages.map { transform(it) })
        abilitiesMap[AbilityType.SERVICE]?.emit(services.map { transform(it) })
        abilitiesMap[AbilityType.WEB]?.emit(webs.map { transform(it) })
        abilitiesMap[AbilityType.DATA]?.emit(datas.map { transform(it) })
      }
    } catch (e: Exception) {
      Timber.e(e)
    }
  }

  suspend fun getRepoUpdatedTime(owner: String, repo: String): String? {
    return getLibraryDetailUseCase.getRepoUpdatedTime(owner, repo)
  }

  fun emitFeature(feature: VersionedFeature) = viewModelScope.launch {
    _featuresFlow.emit(feature)
  }

  suspend fun getRelatedAppListItem(packageName: String): RelatedAppListItem? {
    return withContext(Dispatchers.IO) {
      getRelatedAppListItemUseCase(packageName)
    }
  }

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

  fun sortDetailItems(items: List<LibStringItemChip>, @LibType type: Int): List<LibStringItemChip> {
    return sortAppDetailItemsUseCase(items, type, GlobalValues.libSortMode == MODE_SORT_BY_LIB)
  }
}
