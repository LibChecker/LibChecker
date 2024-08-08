package com.absinthe.libchecker.features.applist.detail

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.util.SparseArray
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.database.entity.Features
import com.absinthe.libchecker.features.applist.LocatedCount
import com.absinthe.libchecker.features.applist.MODE_SORT_BY_SIZE
import com.absinthe.libchecker.features.applist.detail.bean.StatefulComponent
import com.absinthe.libchecker.features.statistics.bean.DISABLED
import com.absinthe.libchecker.features.statistics.bean.LibStringItem
import com.absinthe.libchecker.features.statistics.bean.LibStringItemChip
import com.absinthe.libchecker.utils.DateUtils
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.extensions.getAGPVersion
import com.absinthe.libchecker.utils.extensions.getFeatures
import com.absinthe.libchecker.utils.extensions.getJetpackComposeVersion
import com.absinthe.libchecker.utils.extensions.getKotlinPluginInfo
import com.absinthe.libchecker.utils.extensions.getRxAndroidVersion
import com.absinthe.libchecker.utils.extensions.getRxJavaVersion
import com.absinthe.libchecker.utils.extensions.getRxKotlinVersion
import com.absinthe.libchecker.utils.extensions.getSignatures
import com.absinthe.libchecker.utils.extensions.getStatefulPermissionsList
import com.absinthe.libchecker.utils.harmony.ApplicationDelegate
import com.absinthe.rulesbundle.LCRules
import com.absinthe.rulesbundle.Rule
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ohos.bundle.AbilityInfo
import ohos.bundle.IBundleManager
import retrofit2.HttpException
import timber.log.Timber

class DetailViewModel : ViewModel() {
  val nativeLibItems: MutableStateFlow<List<LibStringItemChip>?> = MutableStateFlow(null)
  val staticLibItems: MutableStateFlow<List<LibStringItemChip>?> = MutableStateFlow(null)
  val metaDataItems: MutableStateFlow<List<LibStringItemChip>?> = MutableStateFlow(null)
  val permissionsItems: MutableStateFlow<List<LibStringItemChip>?> = MutableStateFlow(null)
  val dexLibItems: MutableStateFlow<List<LibStringItemChip>?> = MutableStateFlow(null)
  val signaturesLibItems: MutableStateFlow<List<LibStringItemChip>?> = MutableStateFlow(null)
  val componentsMap = SparseArray<MutableStateFlow<List<StatefulComponent>>>()
  val abilitiesMap = SparseArray<MutableStateFlow<List<StatefulComponent>>>()
  val itemsCountStateFlow: MutableStateFlow<LocatedCount> = MutableStateFlow(LocatedCount(0, 0))
  val processToolIconVisibilityStateFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)
  val processMapStateFlow = MutableStateFlow<Map<String, Int>>(emptyMap())
  val itemsCountList = MutableList(12) { 0 }
  val is64Bit = MutableStateFlow<Boolean?>(null)

  var isApk = false
  var queriedText: String? = null
  var queriedProcess: String? = null
  var processesMap: Map<String, Int> = mapOf()
  var nativeSourceMap: Map<String, Int> = mapOf()

  lateinit var packageInfo: PackageInfo
  val packageInfoStateFlow = MutableStateFlow<PackageInfo?>(null)

  private val _featuresFlow = MutableSharedFlow<VersionedFeature>()
  val featuresFlow = _featuresFlow.asSharedFlow()

  val abiBundleStateFlow = MutableStateFlow<AbiBundle?>(null)

  init {
    componentsMap.put(SERVICE, MutableStateFlow(emptyList()))
    componentsMap.put(ACTIVITY, MutableStateFlow(emptyList()))
    componentsMap.put(RECEIVER, MutableStateFlow(emptyList()))
    componentsMap.put(PROVIDER, MutableStateFlow(emptyList()))
  }

  fun isPackageInfoAvailable(): Boolean {
    return this::packageInfo.isInitialized
  }

  fun initSoAnalysisData() = viewModelScope.launch(Dispatchers.IO) {
    val list = ArrayList<LibStringItemChip>()
    val sourceSet = hashSetOf<String>()

    try {
      packageInfo.applicationInfo?.let { info ->
        list.addAll(
          getNativeChipList(info, PackageUtils.getAbi(packageInfo, isApk))
        )
      }
    } catch (e: PackageManager.NameNotFoundException) {
      Timber.e(e)
    }

    list.forEach { item ->
      item.item.process?.let { process ->
        sourceSet.add(process)
      }
    }
    val sourceMap = sourceSet.filter { source -> source.isNotEmpty() }
      .associateWith { UiUtils.getRandomColor() }
    nativeSourceMap = sourceMap

    if (sourceMap.isNotEmpty()) {
      processMapStateFlow.emit(sourceMap)
      processToolIconVisibilityStateFlow.emit(true)
    }

    nativeLibItems.emit(list)
  }

  fun initStaticData() = viewModelScope.launch(Dispatchers.IO) {
    staticLibItems.emit(getStaticChipList())
  }

  fun initMetaDataData() = viewModelScope.launch(Dispatchers.IO) {
    metaDataItems.emit(getMetaDataChipList())
  }

  fun initPermissionData() = viewModelScope.launch(Dispatchers.IO) {
    permissionsItems.emit(getPermissionChipList())
  }

  var initDexJob: Job? = null

  fun initDexData() {
    initDexJob?.cancel()
    initDexJob = viewModelScope.launch(Dispatchers.IO) {
      val list = getDexChipList()
      dexLibItems.emit(list)
    }
  }

  fun initSignatures(context: Context) = viewModelScope.launch {
    signaturesLibItems.emit(getSignatureChipList(context))
  }

  fun initComponentsData() = viewModelScope.launch(Dispatchers.IO) {
    val processesSet = hashSetOf<String>()
    try {
      packageInfo.let {
        val services = if (it.services?.isNotEmpty() == true || isApk) {
          it.services
        } else {
          PackageUtils.getPackageInfo(it.packageName, PackageManager.GET_SERVICES).services
        }.let { list ->
          PackageUtils.getComponentList(it.packageName, list, true)
        }
        val activities = if (it.activities?.isNotEmpty() == true || isApk) {
          it.activities
        } else {
          PackageUtils.getPackageInfo(it.packageName, PackageManager.GET_ACTIVITIES).activities
        }.let { list ->
          PackageUtils.getComponentList(it.packageName, list, true)
        }
        val receivers = if (it.receivers?.isNotEmpty() == true || isApk) {
          it.receivers
        } else {
          PackageUtils.getPackageInfo(it.packageName, PackageManager.GET_RECEIVERS).receivers
        }.let { list ->
          PackageUtils.getComponentList(it.packageName, list, true)
        }
        val providers = if (it.providers?.isNotEmpty() == true || isApk) {
          it.providers
        } else {
          PackageUtils.getPackageInfo(it.packageName, PackageManager.GET_PROVIDERS).providers
        }.let { list ->
          PackageUtils.getComponentList(it.packageName, list, true)
        }

        services.forEach { sc -> processesSet.add(sc.processName) }
        activities.forEach { sc -> processesSet.add(sc.processName) }
        receivers.forEach { sc -> processesSet.add(sc.processName) }
        providers.forEach { sc -> processesSet.add(sc.processName) }
        componentsMap[SERVICE]?.emit(services)
        componentsMap[ACTIVITY]?.emit(activities)
        componentsMap[RECEIVER]?.emit(receivers)
        componentsMap[PROVIDER]?.emit(providers)
      }
      processesMap =
        processesSet.filter { it.isNotEmpty() }.associateWith { UiUtils.getRandomColor() }
    } catch (e: Exception) {
      Timber.e(e)
    }
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
      Timber.e(it)
    }.getOrNull()
  }

  private suspend fun getNativeChipList(
    info: ApplicationInfo,
    specifiedAbi: Int? = null
  ): List<LibStringItemChip> {
    val list =
      PackageUtils.getNativeDirLibs(packageInfo, specifiedAbi = specifiedAbi).toMutableList()
    val chipList = mutableListOf<LibStringItemChip>()
    var rule: Rule?

    if (list.isEmpty()) {
      return chipList
    } else {
      list.forEach {
        rule = LCAppUtils.getRuleWithRegex(it.name, NATIVE, info.packageName, list)
        chipList.add(LibStringItemChip(it, rule))
      }
      if (GlobalValues.libSortMode == MODE_SORT_BY_SIZE) {
        chipList.sortByDescending { it.item.size }
      } else {
        chipList.sortWith(compareByDescending<LibStringItemChip> { it.rule != null }.thenByDescending { it.item.size })
      }
    }
    return chipList
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
        rule = LCRules.getRule(it.name, STATIC, false)
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
    val list = PackageUtils.getMetaDataItems(packageInfo)
    val chipList = mutableListOf<LibStringItemChip>()

    list.forEach {
      chipList.add(LibStringItemChip(it, null))
    }
    chipList.sortByDescending { it.item.name }
    return chipList
  }

  private fun getPermissionChipList(): List<LibStringItemChip> {
    Timber.d("getPermissionChipList")
    val list = packageInfo.getStatefulPermissionsList().asSequence()
      .map { perm -> LibStringItem(perm.first, if (perm.second) 1 else 0, if (perm.first.contains("maxSdkVersion")) DISABLED else null) }
      .toList()
    val chipList = mutableListOf<LibStringItemChip>()

    list.forEach {
      chipList.add(LibStringItemChip(it, null))
    }
    chipList.sortByDescending { it.item.name }
    return chipList
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
        rule = LCRules.getRule(it.name, DEX, true)
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

  private suspend fun getSignatureChipList(context: Context): List<LibStringItemChip> =
    withContext(Dispatchers.IO) {
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
    abilitiesMap.put(AbilityType.PAGE, MutableStateFlow(emptyList()))
    abilitiesMap.put(AbilityType.SERVICE, MutableStateFlow(emptyList()))
    abilitiesMap.put(AbilityType.WEB, MutableStateFlow(emptyList()))
    abilitiesMap.put(AbilityType.DATA, MutableStateFlow(emptyList()))

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

        abilitiesMap[AbilityType.PAGE]?.emit(pages)
        abilitiesMap[AbilityType.SERVICE]?.emit(services)
        abilitiesMap[AbilityType.WEB]?.emit(webs)
        abilitiesMap[AbilityType.DATA]?.emit(datas)
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

  fun initFeatures(packageInfo: PackageInfo, features: Int) = viewModelScope.launch(Dispatchers.IO) {
    Timber.d("initFeatures: features = $features")
    var feat = features
    if (feat == -1) {
      feat = packageInfo.getFeatures()
      Repositories.lcRepository.updateFeatures(packageInfo.packageName, feat)
    }

    if ((feat and Features.SPLIT_APKS) > 0) {
      _featuresFlow.emit(VersionedFeature(Features.SPLIT_APKS))
    }
    if ((feat and Features.KOTLIN_USED) > 0) {
      val versionInfo = packageInfo.getKotlinPluginInfo()
      _featuresFlow.emit(VersionedFeature(Features.KOTLIN_USED, extras = versionInfo))
    }
    if ((feat and Features.RX_JAVA) > 0) {
      val version = packageInfo.getRxJavaVersion()
      _featuresFlow.emit(VersionedFeature(Features.RX_JAVA, version))
    }
    if ((feat and Features.RX_KOTLIN) > 0) {
      val version = packageInfo.getRxKotlinVersion()
      _featuresFlow.emit(VersionedFeature(Features.RX_KOTLIN, version))
    }
    if ((feat and Features.RX_ANDROID) > 0) {
      val version = packageInfo.getRxAndroidVersion()
      _featuresFlow.emit(VersionedFeature(Features.RX_ANDROID, version))
    }
    if ((feat and Features.AGP) > 0) {
      val version = packageInfo.getAGPVersion()
      _featuresFlow.emit(VersionedFeature(Features.AGP, version))
    }
    if ((feat and Features.XPOSED_MODULE) > 0) {
      _featuresFlow.emit(VersionedFeature(Features.XPOSED_MODULE))
    }
    if ((feat and Features.PLAY_SIGNING) > 0) {
      _featuresFlow.emit(VersionedFeature(Features.PLAY_SIGNING))
    }
    if ((feat and Features.PWA) > 0) {
      _featuresFlow.emit(VersionedFeature(Features.PWA))
    }
    if ((feat and Features.JETPACK_COMPOSE) > 0) {
      val version = packageInfo.getJetpackComposeVersion()
      _featuresFlow.emit(VersionedFeature(Features.JETPACK_COMPOSE, version))
    }

    _featuresFlow.emit(VersionedFeature(Features.Ext.APPLICATION_PROP))

    if (OsUtils.atLeastR()) {
      runCatching {
        val info = SystemServices.packageManager.getInstallSourceInfo(packageInfo.packageName)
        if (info.installingPackageName != null) {
          _featuresFlow.emit(VersionedFeature(Features.Ext.APPLICATION_INSTALL_SOURCE, info.initiatingPackageName))
        }
      }
    }
  }

  fun initAbiInfo(packageInfo: PackageInfo, apkAnalyticsMode: Boolean) = viewModelScope.launch(Dispatchers.IO) {
    val abiSet = PackageUtils.getAbiSet(
      file = File(packageInfo.applicationInfo!!.sourceDir),
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
}
