package com.absinthe.libchecker.features.applist.detail

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.AdaptiveIconDrawable
import android.util.SparseArray
import androidx.core.util.forEach
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.annotation.ACTION
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.DEX
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.annotation.STATIC
import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.api.bean.LibDetailBean
import com.absinthe.libchecker.api.request.CloudRuleBundleRequest
import com.absinthe.libchecker.api.request.LibDetailRequest
import com.absinthe.libchecker.app.SystemServices
import com.absinthe.libchecker.compat.PackageManagerCompat
import com.absinthe.libchecker.constant.AbilityType
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.database.RulesRepository
import com.absinthe.libchecker.database.entity.Features
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.AppBundleSplitItem
import com.absinthe.libchecker.domain.app.AppDetailPackageSize
import com.absinthe.libchecker.domain.app.AppListRepository
import com.absinthe.libchecker.domain.app.AppManifestProperty
import com.absinthe.libchecker.domain.app.GetApkPreviewInfoUseCase
import com.absinthe.libchecker.domain.app.GetAppBundleItemsUseCase
import com.absinthe.libchecker.domain.app.GetAppDetailComponentChipsUseCase
import com.absinthe.libchecker.domain.app.GetAppDetailNativeLibrariesUseCase
import com.absinthe.libchecker.domain.app.GetAppDetailPackageSizeUseCase
import com.absinthe.libchecker.domain.app.GetAppDetailPackageUseCase
import com.absinthe.libchecker.domain.app.GetAppManifestPropertiesUseCase
import com.absinthe.libchecker.domain.app.GetArchivePackageInfoUseCase
import com.absinthe.libchecker.domain.app.GetInstalledAppComparisonPackageUseCase
import com.absinthe.libchecker.domain.app.HasInstalledStaticLibrariesUseCase
import com.absinthe.libchecker.domain.snapshot.BuildPackageComparisonSnapshotItemUseCase
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.features.applist.LocatedCount
import com.absinthe.libchecker.features.applist.MODE_SORT_BY_SIZE
import com.absinthe.libchecker.features.applist.detail.bean.AppIconItem
import com.absinthe.libchecker.features.applist.detail.bean.StatefulComponent
import com.absinthe.libchecker.features.statistics.bean.DISABLED
import com.absinthe.libchecker.features.statistics.bean.EXPORTED
import com.absinthe.libchecker.features.statistics.bean.LibStringItem
import com.absinthe.libchecker.features.statistics.bean.LibStringItemChip
import com.absinthe.libchecker.utils.DateUtils
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.apk.ApkPreviewInfo
import com.absinthe.libchecker.utils.extensions.getAGPVersion
import com.absinthe.libchecker.utils.extensions.getFeatures
import com.absinthe.libchecker.utils.extensions.getJetpackComposeVersion
import com.absinthe.libchecker.utils.extensions.getKotlinPluginInfo
import com.absinthe.libchecker.utils.extensions.getRxAndroidVersion
import com.absinthe.libchecker.utils.extensions.getRxJavaVersion
import com.absinthe.libchecker.utils.extensions.getRxKotlinVersion
import com.absinthe.libchecker.utils.extensions.getSignatures
import com.absinthe.libchecker.utils.extensions.getStatefulPermissionsList
import com.absinthe.libchecker.utils.extensions.isPWA
import com.absinthe.libchecker.utils.extensions.isPageSizeCompat
import com.absinthe.libchecker.utils.extensions.isPlayAppSigning
import com.absinthe.libchecker.utils.extensions.isUseKMP
import com.absinthe.libchecker.utils.extensions.isXposedModule
import com.absinthe.libchecker.utils.extensions.maybeResourceId
import com.absinthe.libchecker.utils.extensions.toClassDefType
import com.absinthe.libchecker.utils.harmony.ApplicationDelegate
import com.absinthe.rulesbundle.Rule
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
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
import retrofit2.HttpException
import timber.log.Timber

class DetailViewModel(
  private val appListRepository: AppListRepository,
  private val getAppDetailPackage: GetAppDetailPackageUseCase,
  private val getAppBundleItemsUseCase: GetAppBundleItemsUseCase,
  private val getAppDetailComponentChipsUseCase: GetAppDetailComponentChipsUseCase,
  private val getAppDetailNativeLibrariesUseCase: GetAppDetailNativeLibrariesUseCase,
  private val getAppDetailPackageSizeUseCase: GetAppDetailPackageSizeUseCase,
  private val getApkPreviewInfoUseCase: GetApkPreviewInfoUseCase,
  private val getAppManifestPropertiesUseCase: GetAppManifestPropertiesUseCase,
  private val getArchivePackageInfoUseCase: GetArchivePackageInfoUseCase,
  private val getInstalledAppComparisonPackageUseCase: GetInstalledAppComparisonPackageUseCase,
  private val hasInstalledStaticLibrariesUseCase: HasInstalledStaticLibrariesUseCase,
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

  val abiBundleStateFlow = MutableStateFlow<AbiBundle?>(null)

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
      staticLibItems.emit(getStaticChipList())
    }
  }

  private var initMetaDataJob: Job? = null

  fun initMetaDataData() {
    if (initMetaDataJob?.isActive == true || metaDataItems.value != null) {
      return
    }
    initMetaDataJob = viewModelScope.launch(Dispatchers.IO) {
      metaDataItems.emit(getMetaDataChipList())
    }
  }

  private var initPermissionJob: Job? = null

  fun initPermissionData() {
    if (initPermissionJob?.isActive == true || permissionsItems.value != null) {
      return
    }
    initPermissionJob = viewModelScope.launch(Dispatchers.IO) {
      val permissions = getPermissionChipList()
      permissionsItems.emit(permissions)

      if (permissions.any { it.item.name == "android.permission.POST_PROMOTED_NOTIFICATIONS" }) {
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

  fun initSignatures(context: Context) {
    if (initSignaturesJob?.isActive == true || signaturesLibItems.value != null) {
      return
    }
    initSignaturesJob = viewModelScope.launch {
      signaturesLibItems.emit(getSignatureChipList(context))
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

  private val request: LibDetailRequest = ApiManager.create()

  suspend fun requestLibDetail(
    libName: String,
    @LibType type: Int,
    isRegex: Boolean = false
  ): LibDetailBean? {
    var categoryDir = when (type) {
      NATIVE -> "native-libs"
      SERVICE -> "services-libs"
      ACTIVITY -> "activities-libs"
      RECEIVER -> "receivers-libs"
      PROVIDER -> "providers-libs"
      DEX -> "dex-libs"
      STATIC -> "static-libs"
      ACTION -> "actions-libs"
      else -> throw IllegalArgumentException("Illegal LibType: $type.")
    }
    if (isRegex) {
      categoryDir += "/regex"
    }
    val libPath = if (type in listOf(SERVICE, ACTIVITY, RECEIVER, PROVIDER, STATIC)) {
      libName.replace(".", "/")
    } else {
      libName
    }
    Timber.d("requestLibDetail: categoryDir = $categoryDir, libPath = $libPath")

    return runCatching {
      request.requestLibDetail(categoryDir, libPath)
    }.onFailure {
      Timber.w(it, "Failed to request lib detail: $categoryDir/$libPath")
    }.getOrNull()
  }

  private suspend fun getStaticChipList(): List<LibStringItemChip> {
    Timber.d("getStaticChipList")
    val list = runCatching { PackageUtils.getStaticLibs(packageInfo) }.getOrDefault(emptyList())
    val chipList = mutableListOf<LibStringItemChip>()
    var rule: Rule?

    if (list.isEmpty()) {
      return chipList
    } else {
      list.forEach {
        rule = RulesRepository.getRule(it.name, STATIC, false)
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

  private fun getMetaDataChipList(): List<LibStringItemChip> {
    Timber.d("getMetaDataChipList")
    val chipList = if (!isApkPreview && apkPreviewInfo == null) {
      PackageUtils.getMetaDataItems(packageInfo)
        .map { LibStringItemChip(it, null) }
    } else {
      apkPreviewInfo!!.metadata
        .map {
          var flag = 0L
          val value = if (it.value is Long || (it.value as? String)?.maybeResourceId() == true) {
            flag = -1
            null
          } else {
            it.value.toString()
          }
          LibStringItemChip(
            LibStringItem(it.key, flag, value),
            null
          )
        }
    }.toMutableList()

    chipList.sortByDescending { it.item.name }
    return chipList
  }

  private fun getPermissionChipList(): List<LibStringItemChip> {
    Timber.d("getPermissionChipList")
    val list = if (!isApkPreview && apkPreviewInfo == null) {
      packageInfo.getStatefulPermissionsList().asSequence()
        .map { perm ->
          LibStringItemChip(
            LibStringItem(
              name = perm.first,
              size = if (perm.second && !isApk) PackageInfo.REQUESTED_PERMISSION_GRANTED.toLong() else 0,
              source = if (perm.first.contains("maxSdkVersion")) DISABLED else null,
              process = if (perm.second && !isApk) PackageInfo.REQUESTED_PERMISSION_GRANTED.toString() else null
            ),
            null
          )
        }
    } else {
      apkPreviewInfo!!.permissions.asSequence()
        .map { perm ->
          LibStringItemChip(
            LibStringItem(name = perm, size = 0, source = null, process = null),
            null
          )
        }
    }.toMutableList()
    list.sortByDescending { it.item.name }
    return list
  }

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

  private suspend fun getSignatureChipList(context: Context): List<LibStringItemChip> = withContext(Dispatchers.IO) {
    // lazy load signatures
    runCatching {
      @Suppress("InlinedApi", "DEPRECATION")
      val flags = PackageManager.GET_SIGNATURES or PackageManager.GET_SIGNING_CERTIFICATES
      if (!isApk) {
        PackageUtils.getPackageInfo(packageInfo.packageName, flags).getSignatures(context)
      } else {
        PackageManagerCompat.getPackageArchiveInfo(packageInfo.applicationInfo!!.sourceDir, flags)!!.getSignatures(context)
      }
    }.onFailure {
      Timber.e(it)
    }.getOrDefault(emptySequence())
      .map {
        LibStringItemChip(it, null)
      }.toList()
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
    val request: CloudRuleBundleRequest = ApiManager.create()
    val result = runCatching {
      request.requestRepoInfo(owner, repo) ?: return null
    }.onFailure {
      if (it is HttpException) {
        GlobalValues.isGitHubReachable = false
      }
    }.getOrNull() ?: return null
    val pushedAt = DateUtils.parseIso8601DateTime(result.pushedAt) ?: return null
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return formatter.format(pushedAt)
  }

  fun emitFeature(feature: VersionedFeature) = viewModelScope.launch {
    _featuresFlow.emit(feature)
  }

  suspend fun getAppListItem(packageName: String): LCItem? {
    return appListRepository.getItem(packageName)
  }

  fun initFeatures(packageInfo: PackageInfo, features: Int) = viewModelScope.launch(Dispatchers.IO) {
    Timber.d("initFeatures: features = $features")

    _featuresFlow.emit(VersionedFeature(Features.Ext.APPLICATION_PROP))

    if (OsUtils.atLeastR() && !isApk) {
      runCatching {
        val info = PackageUtils.getInstallSourceInfo(packageInfo.packageName)
        if (info?.installingPackageName != null) {
          _featuresFlow.emit(VersionedFeature(Features.Ext.APPLICATION_INSTALL_SOURCE, info.initiatingPackageName))
        }
      }.onFailure {
        Timber.e(it)
      }
    }

    var feat = features
    if (feat == -1) {
      feat = packageInfo.getFeatures()
      appListRepository.updateFeatures(packageInfo.packageName, feat)
    }

    if ((feat and Features.SPLIT_APKS) > 0) {
      _featuresFlow.emit(VersionedFeature(Features.SPLIT_APKS))
    }
    if ((feat and Features.KOTLIN_USED) > 0) {
      val versionInfo = packageInfo.getKotlinPluginInfo()
      _featuresFlow.emit(VersionedFeature(Features.KOTLIN_USED, extras = versionInfo))
    }
    if ((feat and Features.AGP) > 0) {
      val version = packageInfo.getAGPVersion()
      _featuresFlow.emit(VersionedFeature(Features.AGP, version))
    }
    if ((feat and Features.JETPACK_COMPOSE) > 0) {
      val version = packageInfo.getJetpackComposeVersion()
      _featuresFlow.emit(VersionedFeature(Features.JETPACK_COMPOSE, version))
    }
    if (packageInfo.isXposedModule()) {
      _featuresFlow.emit(VersionedFeature(Features.XPOSED_MODULE))
    }
    if (packageInfo.isPlayAppSigning()) {
      _featuresFlow.emit(VersionedFeature(Features.PLAY_SIGNING))
    }
    if (packageInfo.isPWA()) {
      _featuresFlow.emit(VersionedFeature(Features.PWA))
    }

    appIcons = getAllAppIcons(packageInfo)
    if (appIcons.isNotEmpty()) {
      _featuresFlow.emit(VersionedFeature(Features.Ext.APPLICATION_ICONS))
    }

    if (OsUtils.atLeastBaklava() && packageInfo.isPageSizeCompat()) {
      _featuresFlow.emit(VersionedFeature(Features.Ext.ELF_PAGE_SIZE_16KB_COMPAT))
    }

    packageInfo.applicationInfo?.sourceDir?.let { sourceDir ->
      val foundList = getFeaturesFoundDexList(feat, sourceDir)
      if ((feat and Features.RX_JAVA) > 0) {
        val version = packageInfo.getRxJavaVersion(foundList)
        _featuresFlow.emit(VersionedFeature(Features.RX_JAVA, version))
      }
      if ((feat and Features.RX_KOTLIN) > 0) {
        val version = packageInfo.getRxKotlinVersion(foundList)
        _featuresFlow.emit(VersionedFeature(Features.RX_KOTLIN, version))
      }
      if ((feat and Features.RX_ANDROID) > 0) {
        val version = packageInfo.getRxAndroidVersion(foundList)
        _featuresFlow.emit(VersionedFeature(Features.RX_ANDROID, version))
      }
      if (packageInfo.isUseKMP(foundList)) {
        _featuresFlow.emit(VersionedFeature(Features.KMP))
      }
    }
  }

  fun initAbiInfo(packageInfo: PackageInfo, apkAnalyticsMode: Boolean) = viewModelScope.launch(Dispatchers.IO) {
    val source = runCatching { packageInfo.applicationInfo?.sourceDir }.getOrNull() ?: return@launch
    val abiSet = PackageUtils.getAbiSet(
      file = File(source),
      packageInfo = packageInfo,
      isApk = apkAnalyticsMode,
      ignoreArch = true
    ).toSet()
    val abi = PackageUtils.getAbi(packageInfo, isApk = apkAnalyticsMode, abiSet = abiSet)
    abiBundleStateFlow.emit(
      AbiBundle(
        abi,
        abiSet.sortedByDescending {
          it == abi || PackageUtils.isAbi64Bit(it)
        }
      )
    )
  }

  fun initAbiInfo(apkPreviewInfo: ApkPreviewInfo) = viewModelScope.launch(Dispatchers.IO) {
    val abiSet = apkPreviewInfo.abiSet
    if (abiSet.isEmpty()) return@launch
    val abi = abiSet.first()
    abiBundleStateFlow.emit(
      AbiBundle(
        abi,
        abiSet.sortedByDescending {
          it == abi || PackageUtils.isAbi64Bit(it)
        }
      )
    )
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

  data class AbiBundle(val abi: Int, val abiSet: Collection<Int>)

  private fun getFeaturesFoundDexList(feat: Int, sourceDir: String): List<String>? {
    val dexList = mutableListOf<String>()
    if ((feat and Features.RX_JAVA) > 0) {
      dexList.addAll(
        listOf(
          "rx.schedulers.*".toClassDefType(),
          "io.reactivex.*".toClassDefType(),
          "io.reactivex.rxjava3.*".toClassDefType()
        )
      )
    }
    if ((feat and Features.RX_KOTLIN) > 0) {
      dexList.addAll(
        listOf(
          "io.reactivex.rxjava3.kotlin.*".toClassDefType(),
          "io.reactivex.rxkotlin".toClassDefType(),
          "rx.lang.kotlin".toClassDefType()
        )
      )
    }
    if ((feat and Features.RX_ANDROID) > 0) {
      dexList.addAll(
        listOf(
          "io.reactivex.rxjava3.android.*".toClassDefType(),
          "io.reactivex.android.*".toClassDefType(),
          "rx.android.*".toClassDefType()
        )
      )
    }
    if (dexList.isNotEmpty()) {
      dexList.add("org.jetbrains.compose.*".toClassDefType())
    }
    return if (dexList.isNotEmpty()) {
      PackageUtils.findDexClasses(File(sourceDir), dexList)
    } else {
      null
    }
  }

  private fun getAllAppIcons(pi: PackageInfo): List<AppIconItem> {
    if (!OsUtils.atLeastO()) return emptyList()
    val ai = pi.applicationInfo ?: return emptyList()
    val pm = SystemServices.packageManager
    val icons = mutableListOf<AppIconItem>()

    // Get the icon displayed by the system (potentially themed/overridden by OEM)
    val mainIcon = pm.getApplicationIcon(ai)

    // Check if the system-returned icon has a monochrome layer
    var hasAddedMonochrome = false
    if (OsUtils.atLeastT() && mainIcon is AdaptiveIconDrawable && mainIcon.monochrome != null) {
      icons.add(AppIconItem(mainIcon, true))
      hasAddedMonochrome = true
    }

    // If monochrome is missing (likely due to OEM icon packs),
    // try loading the raw drawable directly from resources to bypass the theme engine.
    if (!hasAddedMonochrome && OsUtils.atLeastT() && ai.icon != 0) {
      try {
        val res = pm.getResourcesForApplication(ai)
        // Load the drawable directly using the resource ID, bypassing PM's icon logic
        val rawIcon = res.getDrawable(ai.icon, null)

        if (rawIcon is AdaptiveIconDrawable && rawIcon.monochrome != null) {
          icons.add(AppIconItem(rawIcon, true))
        }
      } catch (_: Exception) {
      }
    }

    val altIconsIntent = Intent(Intent.ACTION_MAIN).apply {
      addCategory(Intent.CATEGORY_LAUNCHER)
      setPackage(pi.packageName)
    }
    val intents = PackageManagerCompat.queryIntentActivities(altIconsIntent, PackageManager.MATCH_DISABLED_COMPONENTS)
    val iconResSet = mutableSetOf(ai.icon)
    intents
      .asSequence()
      .filter { !iconResSet.contains(it.iconResource) }
      .map {
        iconResSet.add(it.iconResource)
        it.loadIcon(SystemServices.packageManager)
      }
      .forEach { icons.add(AppIconItem(it, false)) }
    return icons
  }
}
