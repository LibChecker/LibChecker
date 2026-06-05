package com.absinthe.libchecker.features.home

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.ComponentInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.LibCheckerApp
import com.absinthe.libchecker.annotation.ACTION
import com.absinthe.libchecker.annotation.ACTION_IN_RULES
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
import com.absinthe.libchecker.constant.options.LibReferenceOptions
import com.absinthe.libchecker.data.app.LocalAppDataSource
import com.absinthe.libchecker.data.app.PackageChangeState
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.database.RulesRepository
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.features.statistics.bean.LibReference
import com.absinthe.libchecker.services.IWorkerService
import com.absinthe.libchecker.ui.base.IListController
import com.absinthe.libchecker.utils.IntentFilterUtils
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getAppName
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getFeatures
import com.absinthe.libchecker.utils.extensions.getVersionCode
import com.absinthe.libchecker.utils.extensions.isArchivedPackage
import com.absinthe.libchecker.utils.extensions.requireAvailableCacheDir
import com.absinthe.libchecker.utils.harmony.ApplicationDelegate
import com.absinthe.libchecker.utils.harmony.HarmonyOsUtil
import com.absinthe.libraries.utils.manager.TimeRecorder
import com.absinthe.rulesbundle.IconResMap
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ohos.bundle.IBundleManager
import okio.buffer
import okio.sink
import timber.log.Timber

class HomeViewModel : ViewModel() {

  val dbItemsFlow: Flow<List<LCItem>> = Repositories.lcRepository.allLCItemsFlow

  private val _effect: MutableSharedFlow<Effect> = MutableSharedFlow()
  val effect = _effect.asSharedFlow()

  private val _libReference: MutableSharedFlow<List<LibReference>?> = MutableSharedFlow()
  val libReference = _libReference.asSharedFlow()

  private val _isRequestChangeRunning = MutableStateFlow(false)
  val isRequestChangeRunning = _isRequestChangeRunning.asStateFlow()

  private var _savedRefList: List<LibReference>? = null
  val savedRefList: List<LibReference>?
    get() = _savedRefList

  private var referenceMap: HashMap<String, Pair<MutableSet<String>, Int>>? = null
  var savedThreshold = GlobalValues.libReferenceThreshold

  var controller: IListController? = null
  var appListStatus: Int = STATUS_NOT_START
  var workerBinder: IWorkerService? = null
  var checkPackagesPermission: Boolean = false

  // Simple menu state management
  var isSearchMenuExpanded: Boolean = false
  var currentSearchQuery: String = ""

  private val pendingChangedPackages = ArrayDeque<PackageChangeState>()

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

  fun packageChanged(packageChangeState: PackageChangeState) {
    setEffect {
      Effect.PackageChanged(packageChangeState)
    }
  }

  fun sphereTextureAvailable() {
    setEffect {
      Effect.SphereTextureAvailable()
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

  fun initItems(context: Context) {
    if (initJob?.isActive == true) {
      return
    }
    initJob = viewModelScope.launch(Dispatchers.IO) {
      try {
        initItemsImpl(context, LocalAppDataSource.getApplicationList(true))
      } finally {
        initJob = null
      }
    }
  }

  private val bundleManager by lazy { ApplicationDelegate(LibCheckerApp.app).iBundleManager }

  private suspend fun initItemsImpl(context: Context, appList: List<PackageInfo>) {
    Timber.d("initItems: START")

    val packageManager = context.packageManager
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
        lcItems.add(generateLCItemFromPackageInfo(packageManager, info, isHarmony, true))
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

      if (!currentCoroutineContext().isActive) return
    }

    if (lcItems.isNotEmpty()) {
      insert(lcItems)
    }
    updateAppListStatus(STATUS_INIT_END)

    timeRecorder.end()
    Timber.d("initItems: END, $timeRecorder")
    updateAppListStatus(STATUS_NOT_START)
  }

  private var requestChangeJob: Job? = null
  private val requestChangeGeneration = AtomicInteger()

  fun requestChange(context: Context, packageChangeState: PackageChangeState? = null) {
    viewModelScope.launch {
      if (appListStatus == STATUS_START_INIT) {
        Timber.d("Request change canceled: STATUS_START_INIT")
        return@launch
      }
      packageChangeState?.let { pendingChangedPackages.add(it) }
      val generation = requestChangeGeneration.incrementAndGet()
      requestChangeJob?.cancel()
      requestChangeJob = requestChangeImpl(context, packageChangeState == null, generation)
    }
  }

  private fun requestChangeImpl(
    context: Context,
    forceUpdate: Boolean,
    generation: Int
  ) = viewModelScope.launch(Dispatchers.IO) {
    val dbItems = Repositories.lcRepository.getLCItems()
    if (dbItems.isEmpty()) {
      if (requestChangeGeneration.get() == generation) {
        _isRequestChangeRunning.value = false
        updateAppListStatus(STATUS_NOT_START)
        requestChangeJob = null
      }
      return@launch
    }

    Timber.d("Request change: START")
    val timeRecorder = TimeRecorder()
    val packageManager = context.packageManager

    timeRecorder.start()
    _isRequestChangeRunning.value = true
    updateAppListStatus(STATUS_START_REQUEST_CHANGE)

    try {
      val isHarmony = HarmonyOsUtil.isHarmonyOs()

      if (!forceUpdate) {
        while (pendingChangedPackages.isNotEmpty() && isActive) {
          val currentState = pendingChangedPackages.removeFirst()
          val currentPackageName = currentState.packageName

          if (pendingChangedPackages.none { it.packageName == currentPackageName }) {
            when (currentState) {
              is PackageChangeState.Added -> {
                val packageInfo = getChangedPackageInfo(currentPackageName) ?: continue
                val item = generateLCItemFromPackageInfo(packageManager, packageInfo, isHarmony)
                insert(item)
              }

              is PackageChangeState.Removed -> {
                Repositories.lcRepository.deleteLCItemByPackageName(currentPackageName)
              }

              is PackageChangeState.Replaced -> {
                val packageInfo = getChangedPackageInfo(currentPackageName) ?: continue
                val item = generateLCItemFromPackageInfo(packageManager, packageInfo, isHarmony)
                update(item)
              }
            }
          }
        }
      } else {
        val dbItemMap = dbItems.associateBy { it.packageName }
        var applications = LocalAppDataSource.getApplicationMap(true)

        /*
         * The application list returned with a probability only contains system applications.
         * When the difference is greater than a certain threshold, we re-request the list.
         */
        if (hasLargeApplicationDiff(applications, dbItemMap)) {
          Timber.w("Request change canceled because of large diff, re-request appMap")
          applications = LocalAppDataSource.getApplicationMap(true)
        }

        applications.values.asSequence()
          .filter { it.packageName !in dbItemMap }
          .forEach {
            if (!isActive) return@launch
            runCatching {
              insert(generateLCItemFromPackageInfo(packageManager, it, isHarmony))
            }.onFailure { e ->
              Timber.e(e, "requestChange: ${it.packageName}")
            }
          }

        dbItemMap.keys.asSequence()
          .filter { it !in applications }
          .forEach {
            if (!isActive) return@launch
            Repositories.lcRepository.deleteLCItemByPackageName(it)
          }

        applications.values.asSequence()
          .mapNotNull { packageInfo ->
            dbItemMap[packageInfo.packageName]?.let { dbItem -> packageInfo to dbItem }
          }
          .filter { (packageInfo, dbItem) ->
            dbItem.versionCode != packageInfo.getVersionCode() ||
              packageInfo.lastUpdateTime != dbItem.lastUpdatedTime ||
              dbItem.lastUpdatedTime == 0L
          }.forEach { (packageInfo, _) ->
            if (!isActive) return@launch
            runCatching {
              update(generateLCItemFromPackageInfo(packageManager, packageInfo, isHarmony))
            }.onFailure { e ->
              Timber.e(e, "requestChange: ${packageInfo.packageName}")
            }
          }
      }

      refreshList()

      updateAppListStatus(STATUS_START_REQUEST_CHANGE_END)
    } finally {
      timeRecorder.end()
      Timber.d("Request change: END, $timeRecorder")
      if (requestChangeGeneration.get() == generation) {
        updateAppListStatus(STATUS_NOT_START)
        _isRequestChangeRunning.value = false
        requestChangeJob = null
      }
    }
  }

  private fun hasLargeApplicationDiff(
    applications: Map<String, PackageInfo>,
    dbItemMap: Map<String, LCItem>
  ): Boolean {
    return applications.keys.count { it !in dbItemMap } > 30 ||
      dbItemMap.keys.count { it !in applications } > 30
  }

  private fun getChangedPackageInfo(packageName: String): PackageInfo? {
    return runCatching { PackageUtils.getPackageInfo(packageName) }.getOrNull()
  }

  private fun generateLCItemFromPackageInfo(
    packageManager: PackageManager,
    pi: PackageInfo,
    isHarmony: Boolean,
    delayInitFeatures: Boolean = false
  ): LCItem {
    val variant = if (
      isHarmony &&
      bundleManager?.getBundleInfo(pi.packageName, IBundleManager.GET_BUNDLE_DEFAULT) != null
    ) {
      Constants.VARIANT_HAP
    } else {
      Constants.VARIANT_APK
    }

    val ai = pi.applicationInfo ?: throw IllegalArgumentException("ApplicationInfo is null")
    return LCItem(
      pi.packageName,
      pi.getAppName(packageManager).toString(),
      if (pi.isArchivedPackage()) "Archived" else pi.versionName.toString(),
      pi.getVersionCode(),
      pi.firstInstallTime,
      pi.lastUpdateTime,
      (ai.flags and ApplicationInfo.FLAG_SYSTEM) > 0,
      PackageUtils.getAbi(pi).toShort(),
      if (delayInitFeatures) -1 else pi.getFeatures(),
      ai.targetSdkVersion.toShort(),
      variant
    )
  }

  private var computeLibReferenceJob: Job? = null

  fun computeLibReference() {
    computeLibReferenceJob?.cancel()
    computeLibReferenceJob = viewModelScope.launch(Dispatchers.IO) {
      computeLibReferenceImpl(LocalAppDataSource.getApplicationList())
    }
  }

  private suspend fun computeLibReferenceImpl(targets: List<PackageInfo>) {
    referenceMap = null
    _libReference.emit(null)
    val map = HashMap<String, Pair<MutableSet<String>, Int>>()
    val showSystem = GlobalValues.isShowSystemApps

    var progressCount = 0
    val types = getSelectedLibReferenceTypes()
    val progressTotal = (targets.size * types.size).coerceAtLeast(1)

    fun updateLibRefProgressImpl() {
      updateLibRefProgress(progressCount * 100 / progressTotal)
    }

    suspend fun computeInternal(@LibType type: Int): Boolean {
      for (target in targets) {
        if (!currentCoroutineContext().isActive) {
          return false
        }
        val applicationInfo = target.applicationInfo
        if (applicationInfo == null) {
          progressCount++
          updateLibRefProgressImpl()
          continue
        }
        if (!showSystem && (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) > 0) {
          progressCount++
          updateLibRefProgressImpl()
          continue
        }

        computeComponentReference(map, target.packageName, type)
        progressCount++
        updateLibRefProgressImpl()
      }
      return true
    }

    updateLibRefProgress(0)

    for (type in types) {
      if (!computeInternal(type)) {
        return
      }
    }

    referenceMap = map
    matchingRules(map)
  }

  private fun getSelectedLibReferenceTypes(): List<Int> {
    val options = GlobalValues.libReferenceOptions
    return mutableListOf<Int>().apply {
      if (options and LibReferenceOptions.NATIVE_LIBS > 0) add(NATIVE)
      if (options and LibReferenceOptions.SERVICES > 0) add(SERVICE)
      if (options and LibReferenceOptions.ACTIVITIES > 0) add(ACTIVITY)
      if (options and LibReferenceOptions.RECEIVERS > 0) add(RECEIVER)
      if (options and LibReferenceOptions.PROVIDERS > 0) add(PROVIDER)
      if (options and LibReferenceOptions.PERMISSIONS > 0) add(PERMISSION)
      if (options and LibReferenceOptions.METADATA > 0) add(METADATA)
      if (options and LibReferenceOptions.PACKAGES > 0) add(PACKAGE)
      if (options and LibReferenceOptions.SHARED_UID > 0) add(SHARED_UID)
      if (options and LibReferenceOptions.ACTION > 0) add(ACTION)
    }
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
          val list = PackageUtils.getNativeDirLibs(packageInfo)
          val nativeLibNames = list.map { it.name }
          val mapped =
            list.asSequence()
              .filter { RulesRepository.checkNativeLibValidation(packageName, it.name, nativeLibNames) }
              .map { it.name }
          computeReferenceInternal(
            referenceMap,
            packageName,
            NATIVE,
            mapped
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
          val packageInfo = PackageUtils.getPackageInfo(packageName)
          val list = PackageUtils.getDexList(packageInfo)
            .asSequence()
            .filter { it.name.startsWith(packageName).not() }
            .map { it.name }
          computeReferenceInternal(
            referenceMap,
            packageName,
            DEX,
            list
          )
        }

        PERMISSION -> {
          val packageInfo = PackageUtils.getPackageInfo(
            packageName,
            PackageManager.GET_PERMISSIONS
          )
          computeReferenceInternal(
            referenceMap,
            packageName,
            PERMISSION,
            packageInfo.requestedPermissions?.asSequence()
          )
        }

        METADATA -> {
          val packageInfo = PackageUtils.getPackageInfo(
            packageName,
            PackageManager.GET_META_DATA
          )
          computeReferenceInternal(
            referenceMap,
            packageName,
            METADATA,
            packageInfo.applicationInfo?.metaData?.keySet()?.asSequence()
          )
        }

        PACKAGE -> {
          val split = packageName.split(".")
          val packagePrefix = split.subList(0, split.size.coerceAtMost(2)).joinToString(".")
          if (referenceMap[packagePrefix] == null) {
            referenceMap[packagePrefix] = HashSet<String>() to PACKAGE
          }
          referenceMap[packagePrefix]!!.first.add(packageName)
        }

        SHARED_UID -> {
          val packageInfo = PackageUtils.getPackageInfo(packageName)
          if (packageInfo.sharedUserId?.isNotBlank() == true) {
            if (referenceMap[packageInfo.sharedUserId] == null) {
              referenceMap[packageInfo.sharedUserId!!] = HashSet<String>() to SHARED_UID
            }
            referenceMap[packageInfo.sharedUserId]!!.first.add(packageName)
          }
        }

        ACTION -> {
          val packageInfo = PackageUtils.getPackageInfo(packageName)
          val list =
            IntentFilterUtils.parseComponentsFromApk(packageInfo.applicationInfo!!.sourceDir)
              .asSequence()
              .flatMap { component ->
                component.intentFilters.asSequence()
                  .flatMap { filter -> filter.actions }
              }
          // .filter { !it.startsWith("android.") }
          computeReferenceInternal(
            referenceMap,
            packageName,
            ACTION,
            list
          )
        }

        else -> {}
      }
    } catch (e: Exception) {
      Timber.e(e)
    }
  }

  private fun computeComponentReferenceInternal(
    referenceMap: HashMap<String, Pair<MutableSet<String>, Int>>,
    packageName: String,
    @LibType type: Int,
    components: Array<out ComponentInfo>?
  ) {
    computeReferenceInternal(
      referenceMap,
      packageName,
      type,
      components.orEmpty()
        .asSequence()
        .filter { it.name.startsWith(packageName).not() }
        .map { it.name }
    )
  }

  private fun computeReferenceInternal(
    referenceMap: HashMap<String, Pair<MutableSet<String>, Int>>,
    packageName: String,
    @LibType type: Int,
    list: Sequence<String>?
  ) {
    list?.forEach {
      referenceMap.getOrPut(it) { HashSet<String>() to type }.first.apply {
        add(packageName)
      }
    }
  }

  private var matchingJob: Job? = null

  fun matchingRules() {
    val map = referenceMap ?: run {
      computeLibReference()
      return
    }
    matchingRules(map)
  }

  private fun matchingRules(map: HashMap<String, Pair<MutableSet<String>, Int>>) {
    matchingJob?.cancel()
    matchingJob = viewModelScope.launch(Dispatchers.IO) {
      try {
        var progressCount = 0

        fun updateLibRefProgressImpl() {
          val size = map.size
          if (size > 0) {
            updateLibRefProgress(progressCount * 100 / size)
          }
        }

        updateLibRefProgressImpl()

        val refList = mutableListOf<LibReference>()
        val threshold = GlobalValues.libReferenceThreshold
        val isOnlyNotMarked =
          GlobalValues.libReferenceOptions and LibReferenceOptions.ONLY_NOT_MARKED > 0

        for (entry in map) {
          if (!isActive) return@launch
          if (entry.value.first.size >= threshold && entry.key.isNotBlank()) {
            val ruleType = if (entry.value.second == ACTION) ACTION_IN_RULES else entry.value.second
            val rule = if (entry.value.second != PERMISSION && entry.value.second != METADATA) {
              RulesRepository.getRule(entry.key, ruleType, true)
            } else {
              null
            }

            if (!isOnlyNotMarked || rule == null) {
              refList.add(
                LibReference(
                  entry.key,
                  rule,
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
      } finally {
        if (referenceMap === map) {
          referenceMap = null
        }
        map.clear()
      }
    }
  }

  fun cancelMatchingJob() {
    matchingJob?.cancel()
    matchingJob = null
  }

  fun refreshRef() = viewModelScope.launch(Dispatchers.IO) {
    _savedRefList?.let { ref ->
      val threshold = GlobalValues.libReferenceThreshold
      _libReference.emit(ref.filter { it.referredList.size >= threshold })
    }
  }

  private var clearApkCacheJob: Job? = null

  fun clearApkCache() {
    clearApkCacheJob?.cancel()
    clearApkCacheJob = viewModelScope.launch(Dispatchers.IO) {
      LibCheckerApp.app.externalCacheDir?.listFiles()?.forEach { it.deleteRecursively() }
      LibCheckerApp.app.requireAvailableCacheDir()
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
    data class PackageChanged(val packageChangeState: PackageChangeState) : Effect()
    data class RefreshList(val obj: Any? = null) : Effect()
    data class UpdateLibRefProgress(val progress: Int) : Effect()
    data class SphereTextureAvailable(val obj: Any? = null) : Effect()
  }

  fun dumpAppsInfo(os: OutputStream, saveAsMarkDown: Boolean) {
    viewModelScope.launch(Dispatchers.IO) {
      os.sink().buffer().use {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd, HH:mm:ss", Locale.getDefault())
        val formattedTime = simpleDateFormat.format(Date(System.currentTimeMillis()))
        it.writeUtf8("Generated by LibChecker ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})${System.lineSeparator()}")
        it.writeUtf8("Generated at ${formattedTime}${System.lineSeparator()}")
        it.writeUtf8(System.lineSeparator())
        val list = Repositories.lcRepository.getLCItems()

        if (saveAsMarkDown) {
          it.writeUtf8("| Package Name | App Name | Version Name | Version Code | System App | Abi | Target SDK | Play Store Link | F-Droid Link |${System.lineSeparator()}")
          it.writeUtf8("|--------------|----------|--------------|--------------|------------|-----|------------|-----------------|--------------|${System.lineSeparator()}")

          list.forEach { item ->
            val abiString = PackageUtils.getAbiString(LibCheckerApp.app, item.abi.toInt(), false)
            it.writeUtf8(
              "| ${item.packageName} | ${item.label} | ${item.versionName} | ${item.versionCode} | ${item.isSystem} | $abiString | ${item.targetApi} | [Link](https://play.google.com/store/apps/details?id=${item.packageName}) | [Link](https://f-droid.org/packages/${item.packageName}) |${System.lineSeparator()}"
            )
          }
        } else {
          list.forEach { item ->
            val abiString = PackageUtils.getAbiString(LibCheckerApp.app, item.abi.toInt(), false)
            it.writeUtf8("Package name: ${item.packageName}${System.lineSeparator()}")
            it.writeUtf8("App name: ${item.label}${System.lineSeparator()}")
            it.writeUtf8("Version name: ${item.versionName}${System.lineSeparator()}")
            it.writeUtf8("Version code: ${item.versionCode}${System.lineSeparator()}")
            it.writeUtf8("System App: ${item.isSystem}${System.lineSeparator()}")
            it.writeUtf8("Abi: ${abiString}${System.lineSeparator()}")
            it.writeUtf8("Target SDK: ${item.targetApi}${System.lineSeparator()}")
            it.writeUtf8("Play Store Link: https://play.google.com/store/apps/details?id=${item.packageName}${System.lineSeparator()}")
            it.writeUtf8("F-Droid Link: https://f-droid.org/packages/${item.packageName}${System.lineSeparator()}")
            it.writeUtf8(System.lineSeparator())
          }
        }
      }
    }
  }

  fun clearMenuState() {
    isSearchMenuExpanded = false
    currentSearchQuery = ""
  }

  fun generateAppsListSphereTexture(context: Context) {
    viewModelScope.launch(Dispatchers.IO) {
      val baseDir = context.filesDir.resolve("sphere_texture")
      if (!baseDir.exists()) {
        baseDir.mkdirs()
      }
      if ((baseDir.listFiles()?.size ?: 0) >= 2) {
        return@launch
      }
      baseDir.resolve("apps").mkdirs()
      baseDir.resolve("libs").mkdirs()

      val icons = mutableListOf<Drawable>()

      // Apps icons
      val defaultIcon = context.packageManager.defaultActivityIcon
      val defaultMonoIcon = if (OsUtils.atLeastT() && defaultIcon is AdaptiveIconDrawable) defaultIcon.monochrome else null
      LocalAppDataSource.getApplicationList().forEach {
        if (icons.size >= 75) return@forEach
        val icon = context.packageManager.getApplicationIcon(it.applicationInfo!!)
        val result = if (OsUtils.atLeastT()) {
          (icon as? AdaptiveIconDrawable)?.monochrome.takeIf { icon -> !UiUtils.drawablesAreEqual(icon, defaultMonoIcon) }
            ?.apply { setTint(context.getColorByAttr(androidx.appcompat.R.attr.colorPrimary)) }
        } else {
          icon.takeIf { icon -> !UiUtils.drawablesAreEqual(icon, defaultIcon) }
        } ?: return@forEach
        icons.add(result)
      }
      val iconSize = 48.dp
      repeat(5) {
        val subIcons = icons.shuffled().take(25)
        val bitmap = UiUtils.getDrawableStrip(context, subIcons, iconSize, iconSize)
        val file = File(baseDir.resolve("apps"), "$it.png")
        file.sink().buffer().use { sink ->
          bitmap.compress(Bitmap.CompressFormat.PNG, 100, sink.outputStream())
        }
      }

      // Libs icons
      icons.clear()
      repeat(50) {
        icons.add(ContextCompat.getDrawable(context, IconResMap.getIconRes(it))!!)
      }
      repeat(5) {
        val subIcons = icons.shuffled().take(25)
        val bitmap = UiUtils.getDrawableStrip(context, subIcons, iconSize, iconSize)
        val file = File(baseDir.resolve("libs"), "$it.png")
        file.sink().buffer().use { sink ->
          bitmap.compress(Bitmap.CompressFormat.PNG, 100, sink.outputStream())
        }
      }

      sphereTextureAvailable()
    }
  }
}
