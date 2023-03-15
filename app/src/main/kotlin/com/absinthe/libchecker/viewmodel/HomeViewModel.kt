package com.absinthe.libchecker.viewmodel

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.ComponentInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.LibCheckerApp
import com.absinthe.libchecker.SystemServices
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.ALL
import com.absinthe.libchecker.annotation.DEX
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.NOT_MARKED
import com.absinthe.libchecker.annotation.PACKAGE
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.annotation.SHARED_UID
import com.absinthe.libchecker.annotation.STATUS_INIT_END
import com.absinthe.libchecker.annotation.STATUS_NOT_START
import com.absinthe.libchecker.annotation.STATUS_START_INIT
import com.absinthe.libchecker.annotation.STATUS_START_REQUEST_CHANGE
import com.absinthe.libchecker.annotation.STATUS_START_REQUEST_CHANGE_END
import com.absinthe.libchecker.bean.LibChip
import com.absinthe.libchecker.bean.LibReference
import com.absinthe.libchecker.bean.LibStringItem
import com.absinthe.libchecker.bean.StatefulComponent
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.OnceTag
import com.absinthe.libchecker.database.AppItemRepository
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.services.IWorkerService
import com.absinthe.libchecker.ui.fragment.IListController
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.PackageUtils.getFeatures
import com.absinthe.libchecker.utils.harmony.ApplicationDelegate
import com.absinthe.libchecker.utils.harmony.HarmonyOsUtil
import com.absinthe.libraries.utils.manager.TimeRecorder
import com.absinthe.rulesbundle.LCRules
import com.absinthe.rulesbundle.Rule
import com.microsoft.appcenter.analytics.Analytics
import jonathanfinerty.once.Once
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import ohos.bundle.IBundleManager
import timber.log.Timber

class HomeViewModel(application: Application) : AndroidViewModel(application) {

  val dbItems: LiveData<List<LCItem>> = Repositories.lcRepository.allDatabaseItems
  val libReference: MutableLiveData<List<LibReference>?> = MutableLiveData()

  private var savedRefList: MutableLiveData<List<LibReference>?> = MutableLiveData()
  private var referenceMap: HashMap<String, Pair<MutableSet<String>, Int>>? = null
  var savedThreshold = GlobalValues.libReferenceThreshold

  private val _effect: MutableSharedFlow<Effect> = MutableSharedFlow()
  val effect = _effect.asSharedFlow()

  var controller: IListController? = null
  var libRefSystemApps: Boolean? = null
  var libRefType: Int? = null
  var appListStatus: Int = STATUS_NOT_START
  var workerBinder: IWorkerService? = null

  fun reloadApps() {
    setEffect {
      Effect.ReloadApps()
    }
  }

  fun refreshList() {
    setEffect {
      Effect.RefreshList()
    }
  }

  fun packageChanged(packageName: String, action: String) {
    setEffect {
      Effect.PackageChanged(packageName, action)
    }
  }

  private fun updateInitProgress(progress: Int) {
    setEffect {
      Effect.UpdateInitProgress(progress)
    }
  }

  private fun updateAppListStatus(status: Int) {
    setEffect {
      Effect.UpdateAppListStatus(status)
    }
    appListStatus = status
  }

  private fun updateLibRefProgress(progress: Int) {
    setEffect {
      Effect.UpdateLibRefProgress(progress)
    }
  }

  private fun setEffect(builder: () -> Effect) {
    val newEffect = builder()
    viewModelScope.launch {
      _effect.emit(newEffect)
    }
  }

  private var initJob: Job? = null

  fun initItems() {
    if (initJob == null || initJob?.isActive == false) {
      initJob = viewModelScope.launch(Dispatchers.IO) {
        Timber.d("initItems: START")

        val context: Context = getApplication<LibCheckerApp>()
        val timeRecorder = TimeRecorder()
        timeRecorder.start()

        updateAppListStatus(STATUS_START_INIT)
        Repositories.lcRepository.deleteAllItems()
        updateInitProgress(0)

        val appList = PackageUtils.getAppsList()
        val lcItems = mutableListOf<LCItem>()
        val isHarmony = HarmonyOsUtil.isHarmonyOs()
        val bundleManager by lazy { ApplicationDelegate(context).iBundleManager }

        var ai: ApplicationInfo
        var abiType: Int
        var variant: Short

        var lcItem: LCItem
        var progressCount = 0

        for (info in appList) {
          try {
            ai = info.applicationInfo
            abiType = PackageUtils.getAbi(info)

            variant = if (isHarmony && bundleManager?.getBundleInfo(
                info.packageName,
                IBundleManager.GET_BUNDLE_DEFAULT
              ) != null
            ) {
              Constants.VARIANT_HAP
            } else {
              Constants.VARIANT_APK
            }

            lcItem = LCItem(
              packageName = info.packageName,
              label = ai.loadLabel(context.packageManager).toString(),
              versionName = info.versionName.orEmpty(),
              versionCode = PackageUtils.getVersionCode(info),
              installedTime = info.firstInstallTime,
              lastUpdatedTime = info.lastUpdateTime,
              isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM,
              abi = abiType.toShort(),
              features = -1 /* delay init */,
              targetApi = ai.targetSdkVersion.toShort(),
              variant = variant
            )

            lcItems.add(lcItem)
            progressCount++
            updateInitProgress(progressCount * 100 / appList.size)
          } catch (e: Throwable) {
            Timber.e(e, "initItems: ${info.packageName}")
            continue
          }

          if (lcItems.size == 50) {
            insert(lcItems)
            lcItems.clear()
          }
        }

        if (lcItems.isNotEmpty()) {
          insert(lcItems)
        }
        updateAppListStatus(STATUS_INIT_END)

        timeRecorder.end()
        Timber.d("initItems: END, $timeRecorder")
        updateAppListStatus(STATUS_NOT_START)
        initJob = null
      }.also {
        it.start()
      }
    }
  }

  fun requestChange(needRefresh: Boolean = false) =
    viewModelScope.launch(Dispatchers.IO) {
      if (appListStatus == STATUS_START_REQUEST_CHANGE || appListStatus == STATUS_START_INIT) {
        Timber.d("Request change appListStatusLiveData not equals STATUS_START")
        return@launch
      }

      requestChangeImpl(SystemServices.packageManager, needRefresh)
    }

  private suspend fun requestChangeImpl(
    packageManager: PackageManager,
    needRefresh: Boolean = false
  ) {
    Timber.d("Request change: START")
    val timeRecorder = TimeRecorder()
    var appMap = AppItemRepository.getApplicationInfoMap().toMutableMap()

    timeRecorder.start()
    updateAppListStatus(STATUS_START_REQUEST_CHANGE)

    if (needRefresh) {
      appMap = PackageUtils.getAppsList().asSequence()
        .map { it.packageName to it }
        .toMap()
        .toMutableMap()
    }

    dbItems.value?.let { value ->
      val isHarmony = HarmonyOsUtil.isHarmonyOs()
      val bundleManager by lazy { ApplicationDelegate(LibCheckerApp.app).iBundleManager }
      var ai: ApplicationInfo
      var versionCode: Long
      var lcItem: LCItem
      var variant: Short

      for (dbItem in value) {
        try {
          appMap[dbItem.packageName]?.let {
            ai = it.applicationInfo
            versionCode = PackageUtils.getVersionCode(it)

            if (versionCode != dbItem.versionCode || it.lastUpdateTime != dbItem.lastUpdatedTime || dbItem.lastUpdatedTime == 0L) {
              variant = if (isHarmony && bundleManager?.getBundleInfo(
                  it.packageName,
                  IBundleManager.GET_BUNDLE_DEFAULT
                ) != null
              ) {
                Constants.VARIANT_HAP
              } else {
                Constants.VARIANT_APK
              }

              lcItem = LCItem(
                it.packageName,
                ai.loadLabel(packageManager).toString(),
                it.versionName ?: "null",
                versionCode,
                it.firstInstallTime,
                it.lastUpdateTime,
                (ai.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM,
                PackageUtils.getAbi(it).toShort(),
                it.getFeatures(),
                ai.targetSdkVersion.toShort(),
                variant
              )
              update(lcItem)
            }

            appMap.remove(dbItem.packageName)
          } ?: run {
            delete(dbItem)
          }
        } catch (e: Exception) {
          Timber.e(e)
        }
      }

      for (info in appMap.values) {
        try {
          ai = info.applicationInfo
          versionCode = PackageUtils.getVersionCode(info)

          variant = if (isHarmony && bundleManager?.getBundleInfo(
              info.packageName,
              IBundleManager.GET_BUNDLE_DEFAULT
            ) != null
          ) {
            Constants.VARIANT_HAP
          } else {
            Constants.VARIANT_APK
          }

          lcItem = LCItem(
            info.packageName,
            ai.loadLabel(packageManager).toString(),
            info.versionName ?: "null",
            versionCode,
            info.firstInstallTime,
            info.lastUpdateTime,
            (ai.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM,
            PackageUtils.getAbi(info).toShort(),
            info.getFeatures(),
            ai.targetSdkVersion.toShort(),
            variant
          )

          insert(lcItem)
        } catch (e: Exception) {
          Timber.e(e)
        }
      }
      refreshList()
    }

    updateAppListStatus(STATUS_START_REQUEST_CHANGE_END)
    timeRecorder.end()
    Timber.d("Request change: END, $timeRecorder")
    updateAppListStatus(STATUS_NOT_START)

    if (!Once.beenDone(Once.THIS_APP_VERSION, OnceTag.HAS_COLLECT_LIB)) {
      delay(10000)
      collectPopularLibraries()
      Once.markDone(OnceTag.HAS_COLLECT_LIB)
    }
  }

  private fun collectPopularLibraries() = viewModelScope.launch(Dispatchers.IO) {
    if (GlobalValues.isAnonymousAnalyticsEnabled.value == false) {
      return@launch
    }
    val appList = AppItemRepository.getApplicationInfoMap().values
    val map = HashMap<String, Int>()
    var libList: List<LibStringItem>
    var count: Int

    try {
      for (item in appList) {
        libList =
          PackageUtils.getNativeDirLibs(PackageUtils.getPackageInfo(item.packageName))

        for (lib in libList) {
          count = map[lib.name] ?: 0
          map[lib.name] = count + 1
        }
      }
      val properties: MutableMap<String, String> = HashMap()
      properties["Version"] = Build.VERSION.SDK_INT.toString()
      Analytics.trackEvent("OS Version", properties)

      for (entry in map) {
        if (entry.value > 3 && LCAppUtils.getRuleWithRegex(entry.key, NATIVE) == null) {
          properties.clear()
          properties["Library name"] = entry.key
          properties["Library count"] = entry.value.toString()

          Analytics.trackEvent("Native Library", properties)
        }
      }

      collectComponentPopularLibraries(
        appList,
        SERVICE,
        "Service"
      )
      collectComponentPopularLibraries(
        appList,
        ACTIVITY,
        "Activity"
      )
      collectComponentPopularLibraries(
        appList,
        RECEIVER,
        "Receiver"
      )
      collectComponentPopularLibraries(
        appList,
        PROVIDER,
        "Provider"
      )
    } catch (ignore: Exception) {
      Timber.e(ignore, "collectPopularLibraries failed")
    }
  }

  private suspend fun collectComponentPopularLibraries(
    appList: Collection<PackageInfo>,
    @LibType type: Int,
    label: String
  ) {
    val map = HashMap<String, Int>()
    var compLibList: List<StatefulComponent>
    var count: Int

    for (item in appList) {
      try {
        compLibList = PackageUtils.getComponentList(item.packageName, type, false)

        for (lib in compLibList) {
          count = map[lib.componentName] ?: 0
          map[lib.componentName] = count + 1
        }
      } catch (e: Exception) {
        Timber.e(e)
        continue
      }
    }

    val properties: MutableMap<String, String> = HashMap()

    for (entry in map) {
      if (entry.value > 3 && LCAppUtils.getRuleWithRegex(entry.key, type) == null) {
        properties.clear()
        properties["Library name"] = entry.key
        properties["Library count"] = entry.value.toString()

        Analytics.trackEvent("$label Library", properties)
      }
    }
  }

  private var computeLibReferenceJob: Job? = null

  fun computeLibReference(@LibType type: Int) {
    computeLibReferenceJob = viewModelScope.launch(Dispatchers.IO) {
      referenceMap = null
      libReference.postValue(null)
      val appMap = AppItemRepository.getApplicationInfoMap()
      val map = HashMap<String, Pair<MutableSet<String>, Int>>()
      val showSystem = GlobalValues.isShowSystemApps.value ?: false

      var progressCount = 0

      fun updateLibRefProgressImpl() {
        val size = appMap.size
        if (size > 0) {
          updateLibRefProgress(progressCount * 100 / size)
        }
      }

      updateLibRefProgress(0)

      when (type) {
        ALL, NOT_MARKED -> {
          for (item in appMap.values) {
            if (!showSystem && ((item.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM)) {
              progressCount++
              updateLibRefProgressImpl()
              continue
            }

            arrayOf(NATIVE, SERVICE, ACTIVITY, RECEIVER, PROVIDER, PERMISSION, METADATA).forEach {
              computeComponentReference(map, item.packageName, it)
            }
            progressCount++
            updateLibRefProgressImpl()
          }
        }
        NATIVE -> {
          for (item in appMap.values) {
            if (!showSystem && ((item.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM)) {
              progressCount++
              updateLibRefProgressImpl()
              continue
            }

            computeComponentReference(map, item.packageName, NATIVE)
            progressCount++
            updateLibRefProgressImpl()
          }
        }
        SERVICE -> {
          for (item in appMap.values) {
            if (!showSystem && ((item.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM)) {
              progressCount++
              updateLibRefProgressImpl()
              continue
            }

            computeComponentReference(map, item.packageName, SERVICE)
            progressCount++
            updateLibRefProgressImpl()
          }
        }
        ACTIVITY -> {
          for (item in appMap.values) {
            if (!showSystem && ((item.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM)) {
              progressCount++
              updateLibRefProgressImpl()
              continue
            }

            computeComponentReference(map, item.packageName, ACTIVITY)
            progressCount++
            updateLibRefProgressImpl()
          }
        }
        RECEIVER -> {
          for (item in appMap.values) {
            if (!showSystem && ((item.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM)) {
              progressCount++
              updateLibRefProgressImpl()
              continue
            }

            computeComponentReference(map, item.packageName, RECEIVER)
            progressCount++
            updateLibRefProgressImpl()
          }
        }
        PROVIDER -> {
          for (item in appMap.values) {
            if (!showSystem && ((item.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM)) {
              progressCount++
              updateLibRefProgressImpl()
              continue
            }

            computeComponentReference(map, item.packageName, PROVIDER)
            progressCount++
            updateLibRefProgressImpl()
          }
        }
        DEX -> {
          for (item in appMap.values) {
            if (!showSystem && ((item.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM)) {
              progressCount++
              updateLibRefProgressImpl()
              continue
            }

            computeComponentReference(map, item.packageName, DEX)
            progressCount++
            updateLibRefProgressImpl()
          }
        }
        PERMISSION -> {
          for (item in appMap.values) {
            if (!showSystem && ((item.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM)) {
              progressCount++
              updateLibRefProgressImpl()
              continue
            }

            computeComponentReference(map, item.packageName, PERMISSION)
            progressCount++
            updateLibRefProgressImpl()
          }
        }
        METADATA -> {
          for (item in appMap.values) {
            if (!showSystem && ((item.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM)) {
              progressCount++
              updateLibRefProgressImpl()
              continue
            }

            computeComponentReference(map, item.packageName, METADATA)
            progressCount++
            updateLibRefProgressImpl()
          }
        }
        PACKAGE -> {
          for (item in appMap.values) {
            if (!showSystem && ((item.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM)) {
              progressCount++
              updateLibRefProgressImpl()
              continue
            }

            val split = item.packageName.split(".")
            val packagePrefix = split.subList(0, split.size.coerceAtMost(2)).joinToString(".")
            if (map[packagePrefix] == null) {
              map[packagePrefix] = mutableSetOf<String>() to PACKAGE
            }
            map[packagePrefix]!!.first.add(item.packageName)
            progressCount++
            updateLibRefProgressImpl()
          }
        }
        SHARED_UID -> {
          for (item in appMap.values) {
            if (!showSystem && ((item.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM)) {
              progressCount++
              updateLibRefProgressImpl()
              continue
            }

            if (item.sharedUserId?.isNotBlank() == true) {
              if (map[item.sharedUserId] == null) {
                map[item.sharedUserId] = mutableSetOf<String>() to SHARED_UID
              }
              map[item.sharedUserId]!!.first.add(item.packageName)
            }
            progressCount++
            updateLibRefProgressImpl()
          }
        }
      }

      referenceMap = map
      matchingRules(type)
    }
  }

  fun cancelComputingLibReference() {
    computeLibReferenceJob?.cancel()
    computeLibReferenceJob = null
  }

  private fun computeComponentReference(
    referenceMap: HashMap<String, Pair<MutableSet<String>, Int>>,
    packageName: String,
    @LibType type: Int
  ) {
    try {
      when (type) {
        NATIVE -> {
          val packageInfo = PackageUtils.getPackageInfo(packageName)
          computeNativeReferenceInternal(
            referenceMap,
            packageName,
            PackageUtils.getNativeDirLibs(packageInfo)
          )
        }
        SERVICE -> {
          val packageInfo = PackageUtils.getPackageInfo(
            packageName,
            PackageManager.GET_SERVICES
          )
          computeComponentReferenceInternal(referenceMap, packageName, type, packageInfo.services)
        }
        ACTIVITY -> {
          val packageInfo = PackageUtils.getPackageInfo(
            packageName,
            PackageManager.GET_ACTIVITIES
          )
          computeComponentReferenceInternal(referenceMap, packageName, type, packageInfo.activities)
        }
        RECEIVER -> {
          val packageInfo = PackageUtils.getPackageInfo(
            packageName,
            PackageManager.GET_RECEIVERS
          )
          computeComponentReferenceInternal(referenceMap, packageName, type, packageInfo.receivers)
        }
        PROVIDER -> {
          val packageInfo = PackageUtils.getPackageInfo(
            packageName,
            PackageManager.GET_PROVIDERS
          )
          computeComponentReferenceInternal(referenceMap, packageName, type, packageInfo.providers)
        }
        DEX -> {
          computeDexReferenceInternal(
            referenceMap,
            packageName,
            PackageUtils.getDexList(packageName)
          )
        }
        PERMISSION -> {
          val packageInfo = PackageUtils.getPackageInfo(
            packageName,
            PackageManager.GET_PERMISSIONS
          )
          computePermissionReferenceInternal(
            referenceMap,
            packageName,
            packageInfo.requestedPermissions
          )
        }
        METADATA -> {
          val packageInfo = PackageUtils.getPackageInfo(
            packageName,
            PackageManager.GET_META_DATA
          )
          computeMetadataReferenceInternal(
            referenceMap,
            packageName,
            packageInfo.applicationInfo.metaData
          )
        }
      }
    } catch (e: Exception) {
      Timber.e(e)
    }
  }

  private fun computeNativeReferenceInternal(
    referenceMap: HashMap<String, Pair<MutableSet<String>, Int>>,
    packageName: String,
    list: List<LibStringItem>
  ) {
    list.forEach {
      if (referenceMap[it.name] == null) {
        referenceMap[it.name] = mutableSetOf<String>() to NATIVE
      }
      if (LCAppUtils.checkNativeLibValidation(packageName, it.name)) {
        referenceMap[it.name]!!.first.add(packageName)
      }
    }
  }

  private fun computeComponentReferenceInternal(
    referenceMap: HashMap<String, Pair<MutableSet<String>, Int>>,
    packageName: String,
    @LibType type: Int,
    components: Array<out ComponentInfo>?
  ) {
    components?.forEach {
      if (referenceMap[it.name] == null) {
        referenceMap[it.name] = mutableSetOf<String>() to type
      }
      referenceMap[it.name]!!.first.add(packageName)
    }
  }

  private fun computeDexReferenceInternal(
    referenceMap: HashMap<String, Pair<MutableSet<String>, Int>>,
    packageName: String,
    list: List<LibStringItem>
  ) {
    list.forEach {
      if (referenceMap[it.name] == null) {
        referenceMap[it.name] = mutableSetOf<String>() to DEX
      }
      referenceMap[it.name]!!.first.add(packageName)
    }
  }

  private fun computePermissionReferenceInternal(
    referenceMap: HashMap<String, Pair<MutableSet<String>, Int>>,
    packageName: String,
    list: Array<out String>?
  ) {
    list?.forEach {
      if (referenceMap[it] == null) {
        referenceMap[it] = mutableSetOf<String>() to PERMISSION
      }
      referenceMap[it]!!.first.add(packageName)
    }
  }

  private fun computeMetadataReferenceInternal(
    referenceMap: HashMap<String, Pair<MutableSet<String>, Int>>,
    packageName: String,
    bundle: Bundle?
  ) {
    bundle?.keySet()?.forEach {
      if (referenceMap[it] == null) {
        referenceMap[it] = mutableSetOf<String>() to METADATA
      }
      referenceMap[it]!!.first.add(packageName)
    }
  }

  private var matchingJob: Job? = null

  fun matchingRules(
    @LibType type: Int
  ) {
    matchingJob = viewModelScope.launch(Dispatchers.IO) {
      referenceMap?.let { map ->
        var progressCount = 0

        fun updateLibRefProgressImpl() {
          val size = map.size
          if (size > 0) {
            updateLibRefProgress(progressCount * 100 / size)
          }
        }

        updateLibRefProgressImpl()

        val refList = mutableListOf<LibReference>()

        var chip: LibChip?
        var rule: Rule?
        for (entry in map) {
          if (entry.value.first.size >= GlobalValues.libReferenceThreshold && entry.key.isNotBlank()) {
            rule = LCRules.getRule(entry.key, entry.value.second, true)
            chip = null
            rule?.let {
              chip = LibChip(
                iconRes = it.iconRes,
                name = it.label,
                regexName = it.regexName
              )
            }
            if (type != NOT_MARKED) {
              refList.add(
                LibReference(
                  entry.key,
                  chip,
                  entry.value.first,
                  entry.value.second
                )
              )
            } else {
              if (rule == null && entry.value.second != PERMISSION && entry.value.second != METADATA) {
                refList.add(
                  LibReference(
                    entry.key,
                    null,
                    entry.value.first,
                    entry.value.second
                  )
                )
              }
            }
          }
          progressCount++
          updateLibRefProgressImpl()
        }

        refList.sortByDescending { it.referredList.size }
        libReference.postValue(refList)
        savedRefList.postValue(refList)
      }
    }.also {
      it.start()
    }
  }

  fun cancelMatchingJob() {
    matchingJob?.cancel()
    matchingJob = null
  }

  fun refreshRef() {
    savedRefList.value?.let { ref ->
      libReference.value =
        ref.filter { it.referredList.size >= GlobalValues.libReferenceThreshold }
    }
  }

  private suspend fun insert(item: LCItem) = Repositories.lcRepository.insert(item)

  private suspend fun insert(list: List<LCItem>) = Repositories.lcRepository.insert(list)

  private suspend fun update(item: LCItem) = Repositories.lcRepository.update(item)

  private suspend fun delete(item: LCItem) = Repositories.lcRepository.delete(item)

  sealed class Effect {
    data class ReloadApps(val obj: Any? = null) : Effect()
    data class UpdateInitProgress(val progress: Int) : Effect()
    data class UpdateAppListStatus(val status: Int) : Effect()
    data class PackageChanged(val packageName: String, val action: String) : Effect()
    data class RefreshList(val obj: Any? = null) : Effect()
    data class UpdateLibRefProgress(val progress: Int) : Effect()
  }
}
