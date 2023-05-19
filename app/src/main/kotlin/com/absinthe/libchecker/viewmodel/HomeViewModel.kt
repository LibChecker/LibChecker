package com.absinthe.libchecker.viewmodel

import android.content.pm.ApplicationInfo
import android.content.pm.ComponentInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.LibCheckerApp
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.DEX
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.annotation.NATIVE
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
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.LibReferenceOptions
import com.absinthe.libchecker.constant.OnceTag
import com.absinthe.libchecker.data.app.LocalAppDataSource
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.model.LibChip
import com.absinthe.libchecker.model.LibReference
import com.absinthe.libchecker.model.LibStringItem
import com.absinthe.libchecker.model.StatefulComponent
import com.absinthe.libchecker.services.IWorkerService
import com.absinthe.libchecker.ui.fragment.IListController
import com.absinthe.libchecker.utils.FileUtils
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.getAppName
import com.absinthe.libchecker.utils.extensions.getFeatures
import com.absinthe.libchecker.utils.extensions.getVersionCode
import com.absinthe.libchecker.utils.harmony.ApplicationDelegate
import com.absinthe.libchecker.utils.harmony.HarmonyOsUtil
import com.absinthe.libraries.utils.manager.TimeRecorder
import com.absinthe.rulesbundle.LCRules
import com.absinthe.rulesbundle.Rule
import com.microsoft.appcenter.analytics.Analytics
import java.io.File
import jonathanfinerty.once.Once
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import ohos.bundle.IBundleManager
import timber.log.Timber

class HomeViewModel : ViewModel() {

  val dbItemsFlow: Flow<List<LCItem>> = Repositories.lcRepository.allLCItemsFlow

  private val _effect: MutableSharedFlow<Effect> = MutableSharedFlow()
  val effect = _effect.asSharedFlow()

  private val _libReference: MutableSharedFlow<List<LibReference>?> = MutableSharedFlow()
  val libReference = _libReference.asSharedFlow()

  private var _savedRefList: List<LibReference>? = null
  val savedRefList: List<LibReference>?
    get() = _savedRefList

  private var referenceMap: HashMap<String, Pair<MutableSet<String>, Int>>? = null
  var savedThreshold = GlobalValues.libReferenceThreshold

  var controller: IListController? = null
  var libRefSystemApps: Boolean? = null
  var appListStatus: Int = STATUS_NOT_START
  var workerBinder: IWorkerService? = null

  fun reloadApps() {
    if (appListStatus != STATUS_NOT_START || (initJob?.isActive == false && requestChangeJob?.isActive == false)) {
      Timber.d("reloadApps: ignore, appListStatus: $appListStatus")
      return
    }
    setEffect {
      Effect.ReloadApps()
    }
  }

  private fun refreshList() {
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
    if (initJob?.isActive == true) {
      return
    }
    viewModelScope.launch {
      initJob = initItemsImpl(LocalAppDataSource.getApplicationList())
    }
  }

  private val bundleManager by lazy { ApplicationDelegate(LibCheckerApp.app).iBundleManager }

  private fun initItemsImpl(appList: List<PackageInfo>) =
    viewModelScope.launch(Dispatchers.IO) {
      Timber.d("initItems: START")

      val timeRecorder = TimeRecorder()
      timeRecorder.start()

      updateAppListStatus(STATUS_START_INIT)
      Repositories.lcRepository.deleteAllItems()
      updateInitProgress(0)

      val lcItems = mutableListOf<LCItem>()
      val isHarmony = HarmonyOsUtil.isHarmonyOs()
      var progressCount = 0

      for (info in appList) {
        try {
          lcItems.add(generateLCItemFromPackageInfo(info, isHarmony, true))
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
    }

  private var requestChangeJob: Job? = null

  fun requestChange() =
    viewModelScope.launch {
      if (appListStatus == STATUS_START_INIT) {
        Timber.d("Request change canceled: STATUS_START_INIT")
        return@launch
      }
      if (requestChangeJob?.isActive == true) {
        requestChangeJob?.cancel()
      }

      requestChangeJob = requestChangeImpl(LocalAppDataSource.getApplicationMap())
    }

  private fun requestChangeImpl(appMap: Map<String, PackageInfo>) =
    viewModelScope.launch(Dispatchers.IO) {
      val dbItems = Repositories.lcRepository.getLCItems()
      if (dbItems.isEmpty()) {
        return@launch
      }
      Timber.d("Request change: START")
      val timeRecorder = TimeRecorder()

      timeRecorder.start()
      updateAppListStatus(STATUS_START_REQUEST_CHANGE)

      val isHarmony = HarmonyOsUtil.isHarmonyOs()

      val localApps = appMap.map { it.key }.toSet()
      val dbApps = dbItems.map { it.packageName }.toSet()
      val newApps = localApps - dbApps
      val removedApps = dbApps - localApps

      newApps.forEach {
        runCatching {
          val info = appMap[it] ?: return@runCatching
          insert(generateLCItemFromPackageInfo(info, isHarmony))
        }.onFailure { e ->
          Timber.e(e, "requestChange: $it")
        }
      }

      removedApps.forEach {
        Repositories.lcRepository.deleteLCItemByPackageName(it)
      }

      localApps.intersect(dbApps).asSequence()
        .mapNotNull { appMap[it] }
        .filter { pi ->
          dbItems.find { it.packageName == pi.packageName }?.let {
            it.versionCode != pi.getVersionCode() ||
              pi.lastUpdateTime != it.lastUpdatedTime ||
              it.lastUpdatedTime == 0L
          } ?: false
        }.forEach {
          update(generateLCItemFromPackageInfo(it, isHarmony))
        }

      refreshList()

      updateAppListStatus(STATUS_START_REQUEST_CHANGE_END)
      timeRecorder.end()
      Timber.d("Request change: END, $timeRecorder")
      updateAppListStatus(STATUS_NOT_START)

      if (!Once.beenDone(Once.THIS_APP_VERSION, OnceTag.HAS_COLLECT_LIB)) {
        delay(10000)
        collectPopularLibraries(appMap)
        Once.markDone(OnceTag.HAS_COLLECT_LIB)
      }
    }

  private fun generateLCItemFromPackageInfo(
    pi: PackageInfo,
    isHarmony: Boolean,
    delayInitFeatures: Boolean = false
  ): LCItem {
    val variant = if (isHarmony && bundleManager?.getBundleInfo(
        pi.packageName,
        IBundleManager.GET_BUNDLE_DEFAULT
      ) != null
    ) {
      Constants.VARIANT_HAP
    } else {
      Constants.VARIANT_APK
    }

    return LCItem(
      pi.packageName,
      pi.getAppName() ?: "null",
      pi.versionName ?: "null",
      pi.getVersionCode(),
      pi.firstInstallTime,
      pi.lastUpdateTime,
      (pi.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM,
      PackageUtils.getAbi(pi).toShort(),
      if (delayInitFeatures) -1 else pi.getFeatures(),
      pi.applicationInfo.targetSdkVersion.toShort(),
      variant
    )
  }

  private fun collectPopularLibraries(appMap: Map<String, PackageInfo>) =
    viewModelScope.launch(Dispatchers.IO) {
      if (GlobalValues.isAnonymousAnalyticsEnabled.value == false) {
        return@launch
      }
      val appList = appMap.values
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

  fun computeLibReference() {
    computeLibReferenceJob?.cancel()
    computeLibReferenceJob = viewModelScope.launch(Dispatchers.IO) {
      computeLibReferenceImpl(LocalAppDataSource.getApplicationMap())
    }
  }

  private suspend fun computeLibReferenceImpl(appMap: Map<String, PackageInfo>) {
    referenceMap = null
    _libReference.emit(null)
    val map = HashMap<String, Pair<MutableSet<String>, Int>>()
    val showSystem = GlobalValues.isShowSystemApps.value ?: false

    var progressCount = 0

    fun updateLibRefProgressImpl() {
      val size = appMap.size
      if (size > 0) {
        updateLibRefProgress(progressCount * 100 / size)
      }
    }

    fun computeInternal(@LibType type: Int) {
      for (item in appMap.values) {
        if (!showSystem && ((item.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM)) {
          progressCount++
          updateLibRefProgressImpl()
          continue
        }

        computeComponentReference(map, item.packageName, type)
        progressCount++
        updateLibRefProgressImpl()
      }
    }

    updateLibRefProgress(0)

    if (GlobalValues.libReferenceOptions and LibReferenceOptions.NATIVE_LIBS > 0) {
      computeInternal(NATIVE)
    }
    if (GlobalValues.libReferenceOptions and LibReferenceOptions.SERVICES > 0) {
      computeInternal(SERVICE)
    }
    if (GlobalValues.libReferenceOptions and LibReferenceOptions.ACTIVITIES > 0) {
      computeInternal(ACTIVITY)
    }
    if (GlobalValues.libReferenceOptions and LibReferenceOptions.RECEIVERS > 0) {
      computeInternal(RECEIVER)
    }
    if (GlobalValues.libReferenceOptions and LibReferenceOptions.PROVIDERS > 0) {
      computeInternal(PROVIDER)
    }
    if (GlobalValues.libReferenceOptions and LibReferenceOptions.PERMISSIONS > 0) {
      computeInternal(PERMISSION)
    }
    if (GlobalValues.libReferenceOptions and LibReferenceOptions.METADATA > 0) {
      computeInternal(METADATA)
    }
    if (GlobalValues.libReferenceOptions and LibReferenceOptions.PACKAGES > 0) {
      computeInternal(PACKAGE)
    }
    if (GlobalValues.libReferenceOptions and LibReferenceOptions.SHARED_UID > 0) {
      computeInternal(SHARED_UID)
    }

    referenceMap = map
    matchingRules()
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

        PACKAGE -> {
          val split = packageName.split(".")
          val packagePrefix = split.subList(0, split.size.coerceAtMost(2)).joinToString(".")
          if (referenceMap[packagePrefix] == null) {
            referenceMap[packagePrefix] = mutableSetOf<String>() to PACKAGE
          }
          referenceMap[packagePrefix]!!.first.add(packageName)
        }

        SHARED_UID -> {
          val packageInfo = PackageUtils.getPackageInfo(packageName)
          if (packageInfo.sharedUserId?.isNotBlank() == true) {
            if (referenceMap[packageInfo.sharedUserId] == null) {
              referenceMap[packageInfo.sharedUserId] = mutableSetOf<String>() to SHARED_UID
            }
            referenceMap[packageInfo.sharedUserId]!!.first.add(packageName)
          }
        }

        else -> {}
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

  fun matchingRules() {
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
            val shouldAdd = if (GlobalValues.libReferenceOptions and LibReferenceOptions.ONLY_NOT_MARKED > 0) {
              rule == null && entry.value.second != PERMISSION && entry.value.second != METADATA
            } else {
              true
            }
            if (shouldAdd) {
              refList.add(
                LibReference(
                  entry.key,
                  chip,
                  entry.value.first,
                  entry.value.second
                )
              )
            }
          }
          progressCount++
          updateLibRefProgressImpl()
        }

        refList.sortByDescending { it.referredList.size }
        _libReference.emit(refList)
        _savedRefList = refList
      }
    }
  }

  fun cancelMatchingJob() {
    matchingJob?.cancel()
    matchingJob = null
  }

  fun refreshRef() = viewModelScope.launch(Dispatchers.IO) {
    _savedRefList?.let { ref ->
      _libReference.emit(ref.filter { it.referredList.size >= GlobalValues.libReferenceThreshold })
    }
  }

  fun clearApkCache() {
    FileUtils.delete(File(LibCheckerApp.app.externalCacheDir, Constants.TEMP_PACKAGE))
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
