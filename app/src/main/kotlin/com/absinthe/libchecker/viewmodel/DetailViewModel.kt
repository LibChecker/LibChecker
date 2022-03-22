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
import com.absinthe.libchecker.SystemServices
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.DEX
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.annotation.STATIC
import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.api.bean.LibDetailBean
import com.absinthe.libchecker.api.request.LibDetailRequest
import com.absinthe.libchecker.bean.LibChip
import com.absinthe.libchecker.bean.LibStringItemChip
import com.absinthe.libchecker.bean.StatefulComponent
import com.absinthe.libchecker.constant.AbilityType
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.ui.fragment.detail.LocatedCount
import com.absinthe.libchecker.ui.fragment.detail.MODE_SORT_BY_SIZE
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.VersionCompat
import com.absinthe.libchecker.utils.extensions.isTempApk
import com.absinthe.libchecker.utils.harmony.ApplicationDelegate
import com.absinthe.libchecker.utils.manifest.ManifestReader
import com.absinthe.rulesbundle.LCRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ohos.bundle.AbilityInfo
import ohos.bundle.IBundleManager
import timber.log.Timber
import java.io.File

class DetailViewModel(application: Application) : AndroidViewModel(application) {

  val detailBean: MutableLiveData<LibDetailBean?> = MutableLiveData()

  val nativeLibItems: MutableLiveData<List<LibStringItemChip>> = MutableLiveData()
  val staticLibItems: MutableLiveData<List<LibStringItemChip>> = MutableLiveData()
  val metaDataItems: MutableLiveData<List<LibStringItemChip>> = MutableLiveData()
  val permissionsItems: MutableLiveData<List<LibStringItemChip>> = MutableLiveData()
  val dexLibItems: MutableLiveData<List<LibStringItemChip>> = MutableLiveData()
  val componentsMap = SparseArray<MutableLiveData<List<StatefulComponent>>>()
  val abilitiesMap = SparseArray<MutableLiveData<List<StatefulComponent>>>()
  val itemsCountLiveData: MutableLiveData<LocatedCount> = MutableLiveData(LocatedCount(0, 0))
  val itemsCountList = MutableList(9) { 0 }
  val processesSet = mutableSetOf<String>()

  var sortMode = GlobalValues.libSortMode
  var packageName: String = ""
  var isApk = false
  var abiSet: Set<Int>? = null
  var extractNativeLibs: Boolean? = null
  var queriedText: String? = null

  init {
    componentsMap.put(SERVICE, MutableLiveData())
    componentsMap.put(ACTIVITY, MutableLiveData())
    componentsMap.put(RECEIVER, MutableLiveData())
    componentsMap.put(PROVIDER, MutableLiveData())
  }

  fun initSoAnalysisData(packageName: String) = viewModelScope.launch(Dispatchers.IO) {
    val context: Context = getApplication<LibCheckerApp>()
    val list = ArrayList<LibStringItemChip>()

    try {
      val isApk = packageName.isTempApk()
      val info = if (isApk) {
        context.packageManager.getPackageArchiveInfo(
          packageName,
          0
        )?.applicationInfo?.apply {
          sourceDir = packageName
          publicSourceDir = packageName
        }
      } else {
        PackageUtils.getPackageInfo(packageName).applicationInfo
      }

      info?.let {
        val demands = arrayOf("extractNativeLibs")
        val properties = ManifestReader.getManifestProperties(File(it.sourceDir), demands)
        extractNativeLibs = properties["extractNativeLibs"] as? Boolean

        list.addAll(
          getNativeChipList(info, isApk, abiSet?.firstOrNull())
        )
      }
    } catch (e: PackageManager.NameNotFoundException) {
      Timber.e(e)
    }

    nativeLibItems.postValue(list)
  }

  fun initStaticData(packageName: String) = viewModelScope.launch(Dispatchers.IO) {
    staticLibItems.postValue(getStaticChipList(packageName))
  }

  fun initMetaDataData(packageName: String) = viewModelScope.launch(Dispatchers.IO) {
    metaDataItems.postValue(getMetaDataChipList(packageName))
  }

  fun initPermissionData(packageName: String) = viewModelScope.launch(Dispatchers.IO) {
    permissionsItems.postValue(getPermissionChipList(packageName))
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

  fun initComponentsData(packageName: String) = viewModelScope.launch(Dispatchers.IO) {
    val context: Context = getApplication<LibCheckerApp>()

    try {
      if (packageName.isTempApk()) {
        context.packageManager.getPackageArchiveInfo(
          packageName,
          PackageManager.GET_SERVICES
            or PackageManager.GET_ACTIVITIES
            or PackageManager.GET_RECEIVERS
            or PackageManager.GET_PROVIDERS
            or VersionCompat.MATCH_DISABLED_COMPONENTS
        )?.apply {
          applicationInfo.sourceDir = packageName
          applicationInfo.publicSourceDir = packageName
        }?.let {
          val services = PackageUtils.getComponentList(it.packageName, it.services, true)
          val activities = PackageUtils.getComponentList(it.packageName, it.activities, true)
          val receivers = PackageUtils.getComponentList(it.packageName, it.receivers, true)
          val providers = PackageUtils.getComponentList(it.packageName, it.providers, true)

          services.forEach { sc -> processesSet.add(sc.processName) }
          activities.forEach { sc -> processesSet.add(sc.processName) }
          receivers.forEach { sc -> processesSet.add(sc.processName) }
          providers.forEach { sc -> processesSet.add(sc.processName) }
          componentsMap[SERVICE]?.postValue(services)
          componentsMap[ACTIVITY]?.postValue(activities)
          componentsMap[RECEIVER]?.postValue(receivers)
          componentsMap[PROVIDER]?.postValue(providers)
        }
      } else {
        PackageUtils.getPackageInfo(packageName).let {
          val services = PackageUtils.getComponentList(it.packageName, SERVICE, true)
          val activities = PackageUtils.getComponentList(it.packageName, ACTIVITY, true)
          val receivers = PackageUtils.getComponentList(it.packageName, RECEIVER, true)
          val providers = PackageUtils.getComponentList(it.packageName, PROVIDER, true)

          services.forEach { sc -> processesSet.add(sc.processName) }
          activities.forEach { sc -> processesSet.add(sc.processName) }
          receivers.forEach { sc -> processesSet.add(sc.processName) }
          providers.forEach { sc -> processesSet.add(sc.processName) }
          componentsMap[SERVICE]?.postValue(services)
          componentsMap[ACTIVITY]?.postValue(activities)
          componentsMap[RECEIVER]?.postValue(receivers)
          componentsMap[PROVIDER]?.postValue(providers)
        }
      }
      processesSet.filter { it.isBlank() }
      Timber.d("processesSet=$processesSet")
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
        else -> throw IllegalArgumentException("Illegal LibType.")
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
    isApk: Boolean,
    specifiedAbi: Int? = null
  ): List<LibStringItemChip> {
    val packageInfo = if (!isApk) {
      PackageUtils.getPackageInfo(info.packageName)
    } else {
      PackageInfo().apply {
        packageName = info.packageName
        applicationInfo = info
      }
    }
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
        chipList.sortWith(compareByDescending<LibStringItemChip> { it.chip != null }.thenBy { it.item.name })
      }
    }
    return chipList
  }

  private suspend fun getStaticChipList(packageName: String): List<LibStringItemChip> {
    Timber.d("getStaticChipList")
    val list = PackageUtils.getStaticLibs(PackageUtils.getPackageInfo(packageName))
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

  private suspend fun getMetaDataChipList(packageName: String): List<LibStringItemChip> {
    Timber.d("getMetaDataChipList")
    val isApk = packageName.isTempApk()
    val info = if (isApk) {
      SystemServices.packageManager.getPackageArchiveInfo(
        packageName,
        PackageManager.GET_META_DATA
      )
    } else {
      PackageUtils.getPackageInfo(packageName, PackageManager.GET_META_DATA)
    }
    if (info == null) {
      return emptyList()
    }
    val list = PackageUtils.getMetaDataItems(info)
    val chipList = mutableListOf<LibStringItemChip>()
    var chip: LibChip?

    if (list.isEmpty()) {
      return chipList
    } else {
      list.forEach {
        chip = null
        LCRules.getRule(it.name, METADATA, false)?.let { rule ->
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

  private suspend fun getPermissionChipList(packageName: String): List<LibStringItemChip> {
    Timber.d("getPermissionChipList")
    val isApk = packageName.isTempApk()
    val info = if (isApk) {
      SystemServices.packageManager.getPackageArchiveInfo(
        packageName,
        PackageManager.GET_PERMISSIONS
      )
    } else {
      try {
        PackageUtils.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
      } catch (e: PackageManager.NameNotFoundException) {
        null
      }
    }
    if (info == null) {
      return emptyList()
    }

    val list = PackageUtils.getPermissionsItems(info)
    val chipList = mutableListOf<LibStringItemChip>()
    var chip: LibChip?

    if (list.isEmpty()) {
      return chipList
    } else {
      list.forEach {
        chip = null
        LCRules.getRule(it.name, PERMISSION, false)?.let { rule ->
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

  fun initAbilities(packageName: String) = viewModelScope.launch(Dispatchers.IO) {
    abilitiesMap.put(AbilityType.PAGE, MutableLiveData())
    abilitiesMap.put(AbilityType.SERVICE, MutableLiveData())
    abilitiesMap.put(AbilityType.WEB, MutableLiveData())
    abilitiesMap.put(AbilityType.DATA, MutableLiveData())

    val context: Context = getApplication<LibCheckerApp>()

    try {
      ApplicationDelegate(context).iBundleManager?.getBundleInfo(
        packageName, IBundleManager.GET_BUNDLE_WITH_ABILITIES
      )?.abilityInfos?.let { abilities ->
        val pages = abilities.asSequence()
          .filter { it.type == AbilityInfo.AbilityType.PAGE }
          .map {
            StatefulComponent(
              it.className,
              it.enabled,
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
}
