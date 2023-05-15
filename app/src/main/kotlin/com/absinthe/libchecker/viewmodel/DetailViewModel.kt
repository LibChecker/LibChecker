package com.absinthe.libchecker.viewmodel

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.util.SparseArray
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.LibCheckerApp
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
import com.absinthe.libchecker.constant.AbilityType
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.database.entity.Features
import com.absinthe.libchecker.model.DISABLED
import com.absinthe.libchecker.model.LibChip
import com.absinthe.libchecker.model.LibStringItem
import com.absinthe.libchecker.model.LibStringItemChip
import com.absinthe.libchecker.model.StatefulComponent
import com.absinthe.libchecker.ui.detail.VersionedFeature
import com.absinthe.libchecker.ui.fragment.detail.LocatedCount
import com.absinthe.libchecker.ui.fragment.detail.MODE_SORT_BY_SIZE
import com.absinthe.libchecker.utils.DateUtils
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.extensions.getAGPVersion
import com.absinthe.libchecker.utils.extensions.getFeatures
import com.absinthe.libchecker.utils.extensions.getJetpackComposeVersion
import com.absinthe.libchecker.utils.extensions.getKotlinPluginVersion
import com.absinthe.libchecker.utils.extensions.getRxAndroidVersion
import com.absinthe.libchecker.utils.extensions.getRxJavaVersion
import com.absinthe.libchecker.utils.extensions.getRxKotlinVersion
import com.absinthe.libchecker.utils.extensions.getSignatures
import com.absinthe.libchecker.utils.extensions.getStatefulPermissionsList
import com.absinthe.libchecker.utils.extensions.isTempApk
import com.absinthe.libchecker.utils.harmony.ApplicationDelegate
import com.absinthe.rulesbundle.LCRules
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ohos.bundle.AbilityInfo
import ohos.bundle.IBundleManager
import retrofit2.HttpException
import timber.log.Timber

class DetailViewModel(application: Application) : AndroidViewModel(application) {

  val detailBean: MutableLiveData<LibDetailBean?> = MutableLiveData()

  val nativeLibItems: MutableLiveData<List<LibStringItemChip>> = MutableLiveData()
  val staticLibItems: MutableLiveData<List<LibStringItemChip>> = MutableLiveData()
  val metaDataItems: MutableLiveData<List<LibStringItemChip>> = MutableLiveData()
  val permissionsItems: MutableLiveData<List<LibStringItemChip>> = MutableLiveData()
  val dexLibItems: MutableLiveData<List<LibStringItemChip>> = MutableLiveData()
  val signaturesLibItems: MutableLiveData<List<LibStringItemChip>> = MutableLiveData()
  val componentsMap = SparseArray<MutableLiveData<List<StatefulComponent>>>()
  val abilitiesMap = SparseArray<MutableLiveData<List<StatefulComponent>>>()
  val itemsCountLiveData: MutableLiveData<LocatedCount> = MutableLiveData(LocatedCount(0, 0))
  val processToolIconVisibilityLiveData: MutableLiveData<Boolean> = MutableLiveData(false)
  val processMapLiveData = MutableLiveData<Map<String, Int>>()
  val itemsCountList = MutableList(12) { 0 }
  val is64Bit = MutableLiveData<Boolean>(null)

  var sortMode = GlobalValues.libSortMode
  var isApk = false
  var queriedText: String? = null
  var queriedProcess: String? = null
  var processesMap: Map<String, Int> = mapOf()
  var processMode: Boolean = GlobalValues.processMode
  var nativeSourceMap: Map<String, Int> = mapOf()

  lateinit var packageInfo: PackageInfo
  val packageInfoLiveData = MutableLiveData<PackageInfo>(null)

  private val _featuresFlow = MutableSharedFlow<VersionedFeature>()
  val featuresFlow = _featuresFlow.asSharedFlow()

  val abiBundle = MutableLiveData<AbiBundle>(null)

  init {
    componentsMap.put(SERVICE, MutableLiveData())
    componentsMap.put(ACTIVITY, MutableLiveData())
    componentsMap.put(RECEIVER, MutableLiveData())
    componentsMap.put(PROVIDER, MutableLiveData())
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
      processMapLiveData.postValue(sourceMap)
      processToolIconVisibilityLiveData.postValue(true)
    }

    nativeLibItems.postValue(list)
  }

  fun initStaticData() = viewModelScope.launch(Dispatchers.IO) {
    staticLibItems.postValue(getStaticChipList())
  }

  fun initMetaDataData() = viewModelScope.launch(Dispatchers.IO) {
    metaDataItems.postValue(getMetaDataChipList())
  }

  fun initPermissionData() = viewModelScope.launch(Dispatchers.IO) {
    permissionsItems.postValue(getPermissionChipList())
  }

  var initDexJob: Job? = null

  fun initDexData(packageName: String) {
    initDexJob?.cancel()
    initDexJob = viewModelScope.launch(Dispatchers.IO) {
      val list = getDexChipList(packageName)
      dexLibItems.postValue(list)
    }.also {
      it.start()
    }
  }

  fun initSignatures() = viewModelScope.launch {
    signaturesLibItems.value = getSignatureChipList()
  }

  fun initComponentsData() = viewModelScope.launch(Dispatchers.IO) {
    val processesSet = hashSetOf<String>()
    try {
      packageInfo.let {
        val services = if (it.services?.isNotEmpty() == true) {
          it.services
        } else {
          PackageUtils.getPackageInfo(it.packageName, PackageManager.GET_SERVICES).services
        }.let { list ->
          PackageUtils.getComponentList(it.packageName, list, true)
        }
        val activities = if (it.activities?.isNotEmpty() == true) {
          it.activities
        } else {
          PackageUtils.getPackageInfo(it.packageName, PackageManager.GET_ACTIVITIES).activities
        }.let { list ->
          PackageUtils.getComponentList(it.packageName, list, true)
        }
        val receivers = if (it.receivers?.isNotEmpty() == true) {
          it.receivers
        } else {
          PackageUtils.getPackageInfo(it.packageName, PackageManager.GET_RECEIVERS).receivers
        }.let { list ->
          PackageUtils.getComponentList(it.packageName, list, true)
        }
        val providers = if (it.providers?.isNotEmpty() == true) {
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
        componentsMap[SERVICE]?.postValue(services)
        componentsMap[ACTIVITY]?.postValue(activities)
        componentsMap[RECEIVER]?.postValue(receivers)
        componentsMap[PROVIDER]?.postValue(providers)
      }
      processesMap =
        processesSet.filter { it.isNotEmpty() }.associateWith { UiUtils.getRandomColor() }
    } catch (e: Exception) {
      Timber.e(e)
    }
  }

  private val request: LibDetailRequest = ApiManager.create()

  fun requestLibDetail(libName: String, @LibType type: Int, isRegex: Boolean = false) =
    viewModelScope.launch(Dispatchers.IO) {
      Timber.d("requestLibDetail")
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

      detailBean.postValue(
        try {
          request.requestLibDetail(categoryDir, libName)
        } catch (t: Throwable) {
          Timber.e(t, "DetailViewModel")
          null
        }
      )
    }

  private suspend fun getNativeChipList(
    info: ApplicationInfo,
    specifiedAbi: Int? = null
  ): List<LibStringItemChip> {
    val list =
      PackageUtils.getNativeDirLibs(packageInfo, specifiedAbi = specifiedAbi).toMutableList()
    val chipList = mutableListOf<LibStringItemChip>()
    var chip: LibChip?

    if (list.isEmpty()) {
      return chipList
    } else {
      list.forEach {
        chip = LCAppUtils.getRuleWithRegex(it.name, NATIVE, info.packageName, list)?.let { rule ->
          LibChip(
            iconRes = rule.iconRes,
            name = rule.label,
            regexName = rule.regexName
          )
        }
        chipList.add(LibStringItemChip(it, chip))
      }
      if (GlobalValues.libSortMode == MODE_SORT_BY_SIZE) {
        chipList.sortByDescending { it.item.size }
      } else {
        chipList.sortWith(compareByDescending<LibStringItemChip> { it.chip != null }.thenByDescending { it.item.size })
      }
    }
    return chipList
  }

  private suspend fun getStaticChipList(): List<LibStringItemChip> {
    Timber.d("getStaticChipList")
    val list = runCatching { PackageUtils.getStaticLibs(packageInfo) }.getOrDefault(emptyList())
    val chipList = mutableListOf<LibStringItemChip>()
    var chip: LibChip?

    if (list.isEmpty()) {
      return chipList
    } else {
      list.forEach {
        chip = null
        LCRules.getRule(it.name, STATIC, false)?.let { rule ->
          chip = LibChip(
            iconRes = rule.iconRes,
            name = rule.label,
            regexName = rule.regexName
          )
        }
        chipList.add(LibStringItemChip(it, chip))
      }
      if (GlobalValues.libSortMode == MODE_SORT_BY_SIZE) {
        chipList.sortByDescending { it.item.name }
      } else {
        chipList.sortByDescending { it.chip != null }
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

  private suspend fun getDexChipList(packageName: String): List<LibStringItemChip> {
    Timber.d("getDexChipList")
    val list = try {
      PackageUtils.getDexList(packageName, packageName.isTempApk()).toMutableList()
    } catch (e: Exception) {
      Timber.e(e)
      emptyList()
    }
    val chipList = mutableListOf<LibStringItemChip>()
    var chip: LibChip?

    if (list.isEmpty()) {
      return chipList
    } else {
      list.forEach {
        chip = null
        LCRules.getRule(it.name, DEX, true)?.let { rule ->
          chip = LibChip(
            iconRes = rule.iconRes,
            name = rule.label,
            regexName = rule.regexName
          )
        }
        chipList.add(LibStringItemChip(it, chip))
      }
      if (GlobalValues.libSortMode == MODE_SORT_BY_SIZE) {
        chipList.sortByDescending { it.item.name }
      } else {
        chipList.sortByDescending { it.chip != null }
      }
    }
    return chipList
  }

  private suspend fun getSignatureChipList(): List<LibStringItemChip> =
    withContext(Dispatchers.IO) {
      packageInfo.getSignatures(getApplication()).map {
        LibStringItemChip(it, null)
      }.toList()
    }

  fun initAbilities(packageName: String) = viewModelScope.launch(Dispatchers.IO) {
    abilitiesMap.put(AbilityType.PAGE, MutableLiveData())
    abilitiesMap.put(AbilityType.SERVICE, MutableLiveData())
    abilitiesMap.put(AbilityType.WEB, MutableLiveData())
    abilitiesMap.put(AbilityType.DATA, MutableLiveData())

    val context: Context = getApplication<LibCheckerApp>()

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

        abilitiesMap[AbilityType.PAGE]?.postValue(pages)
        abilitiesMap[AbilityType.SERVICE]?.postValue(services)
        abilitiesMap[AbilityType.WEB]?.postValue(webs)
        abilitiesMap[AbilityType.DATA]?.postValue(datas)
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
        GlobalValues.isGitHubUnreachable = false
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
      _featuresFlow.emit(VersionedFeature(Features.SPLIT_APKS, null))
    }
    if ((feat and Features.KOTLIN_USED) > 0) {
      val version = packageInfo.getKotlinPluginVersion()
      _featuresFlow.emit(VersionedFeature(Features.KOTLIN_USED, version))
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
      _featuresFlow.emit(VersionedFeature(Features.XPOSED_MODULE, null))
    }
    if ((feat and Features.PLAY_SIGNING) > 0) {
      _featuresFlow.emit(VersionedFeature(Features.PLAY_SIGNING, null))
    }
    if ((feat and Features.PWA) > 0) {
      _featuresFlow.emit(VersionedFeature(Features.PWA, null))
    }
    if ((feat and Features.JETPACK_COMPOSE) > 0) {
      val version = packageInfo.getJetpackComposeVersion()
      _featuresFlow.emit(VersionedFeature(Features.JETPACK_COMPOSE, version))
    }

    _featuresFlow.emit(VersionedFeature(Features.Ext.APPLICATION_PROP, null))

    if (OsUtils.atLeastR()) {
      runCatching {
        val info = PackageUtils.getInstallSourceInfo(packageInfo.packageName) ?: throw NullPointerException()
        if (info.originatingPackageName != null || info.installingPackageName != null) {
          _featuresFlow.emit(VersionedFeature(Features.Ext.APPLICATION_INSTALL_SOURCE, info.initiatingPackageName))
        }
      }
    }
  }

  fun initAbiInfo(packageInfo: PackageInfo, apkAnalyticsMode: Boolean) = viewModelScope.launch(Dispatchers.IO) {
    val abiSet = PackageUtils.getAbiSet(
      file = File(packageInfo.applicationInfo.sourceDir),
      packageInfo = packageInfo,
      isApk = apkAnalyticsMode,
      ignoreArch = true
    ).toSet()
    val abi = PackageUtils.getAbi(packageInfo, isApk = apkAnalyticsMode, abiSet = abiSet)
    abiBundle.postValue(AbiBundle(abi, abiSet.sortedByDescending { it == abi }.toSet()))
  }

  data class AbiBundle(val abi: Int, val abiSet: Set<Int>)
}
