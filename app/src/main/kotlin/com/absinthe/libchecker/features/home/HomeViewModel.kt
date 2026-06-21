package com.absinthe.libchecker.features.home

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.LibCheckerApp
import com.absinthe.libchecker.annotation.STATUS_INIT_END
import com.absinthe.libchecker.annotation.STATUS_NOT_START
import com.absinthe.libchecker.annotation.STATUS_START_INIT
import com.absinthe.libchecker.annotation.STATUS_START_REQUEST_CHANGE
import com.absinthe.libchecker.annotation.STATUS_START_REQUEST_CHANGE_END
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.options.LibReferenceOptions
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.AppListRepository
import com.absinthe.libchecker.domain.app.InitializeAppListUseCase
import com.absinthe.libchecker.domain.app.InstalledAppRepository
import com.absinthe.libchecker.domain.app.PackageChangeState
import com.absinthe.libchecker.domain.app.SyncAppListChangesUseCase
import com.absinthe.libchecker.domain.statistics.ComputeLibReferenceUseCase
import com.absinthe.libchecker.domain.statistics.LibReferenceItem
import com.absinthe.libchecker.features.statistics.bean.LibReference
import com.absinthe.libchecker.services.IWorkerService
import com.absinthe.libchecker.ui.base.IListController
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.requireAvailableCacheDir
import com.absinthe.rulesbundle.IconResMap
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okio.buffer
import okio.sink
import timber.log.Timber

class HomeViewModel(
  private val installedAppRepository: InstalledAppRepository,
  private val appListRepository: AppListRepository,
  private val initializeAppListUseCase: InitializeAppListUseCase,
  private val syncAppListChangesUseCase: SyncAppListChangesUseCase,
  private val computeLibReferenceUseCase: ComputeLibReferenceUseCase
) : ViewModel() {

  val dbItemsFlow: Flow<List<LCItem>> = appListRepository.items
  val packageChanges = installedAppRepository.packageChanges

  private val _effect: MutableSharedFlow<Effect> = MutableSharedFlow()
  val effect = _effect.asSharedFlow()

  private val _libReference: MutableSharedFlow<List<LibReference>?> = MutableSharedFlow()
  val libReference = _libReference.asSharedFlow()

  private val _isRequestChangeRunning = MutableStateFlow(false)
  val isRequestChangeRunning = _isRequestChangeRunning.asStateFlow()

  private var _savedRefList: List<LibReference>? = null
  val savedRefList: List<LibReference>?
    get() = _savedRefList

  private var referenceIndex: ComputeLibReferenceUseCase.ReferenceIndex? = null
  var savedThreshold = GlobalValues.libReferenceThreshold

  var controller: IListController? = null
  var appListStatus: Int = STATUS_NOT_START
  var workerBinder: IWorkerService? = null
  var checkPackagesPermission: Boolean = false

  // Simple menu state management
  var isSearchMenuExpanded: Boolean = false
  var currentSearchQuery: String = ""

  private val pendingChangedPackages = ArrayDeque<PackageChangeState>()
  private val pendingChangedPackagesLock = Any()
  private var pendingFeatureInitializationRequest = false

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

  fun connectWorkerBinder(binder: IWorkerService) {
    workerBinder = binder
    if (pendingFeatureInitializationRequest) {
      requestFeatureInitialization()
    }
  }

  fun disconnectWorkerBinder() {
    workerBinder = null
  }

  fun requestFeatureInitialization() {
    val binder = workerBinder ?: run {
      pendingFeatureInitializationRequest = true
      return
    }
    if (appListStatus == STATUS_START_INIT || appListStatus == STATUS_START_REQUEST_CHANGE) {
      pendingFeatureInitializationRequest = true
      return
    }
    pendingFeatureInitializationRequest = false
    runCatching {
      binder.initFeatures()
    }.onFailure {
      Timber.w(it, "requestFeatureInitialization failed")
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

  fun initItems() {
    if (initJob?.isActive == true) {
      return
    }
    initJob = viewModelScope.launch(Dispatchers.IO) {
      try {
        initItemsImpl()
      } finally {
        initJob = null
      }
    }
  }

  private suspend fun initItemsImpl() {
    updateAppListStatus(STATUS_START_INIT)
    if (!initializeAppListUseCase(::updateInitProgress)) return
    updateAppListStatus(STATUS_INIT_END)

    updateAppListStatus(STATUS_NOT_START)
    requestFeatureInitialization()
  }

  private var requestChangeJob: Job? = null
  private val requestChangeGeneration = AtomicInteger()

  fun requestChange(packageChangeState: PackageChangeState? = null) {
    viewModelScope.launch {
      if (appListStatus == STATUS_START_INIT) {
        Timber.d("Request change canceled: STATUS_START_INIT")
        return@launch
      }
      packageChangeState?.let(::addPendingChangedPackage)
      val generation = requestChangeGeneration.incrementAndGet()
      requestChangeJob?.cancel()
      requestChangeJob = requestChangeImpl(packageChangeState == null, generation)
    }
  }

  private fun requestChangeImpl(
    forceUpdate: Boolean,
    generation: Int
  ) = viewModelScope.launch(Dispatchers.IO) {
    val dbItems = appListRepository.getItems()
    if (dbItems.isEmpty()) {
      if (requestChangeGeneration.get() == generation) {
        _isRequestChangeRunning.value = false
        updateAppListStatus(STATUS_NOT_START)
        requestChangeJob = null
      }
      return@launch
    }

    _isRequestChangeRunning.value = true
    updateAppListStatus(STATUS_START_REQUEST_CHANGE)

    try {
      val syncRequest = if (forceUpdate) {
        SyncAppListChangesUseCase.Request.RefreshAll
      } else {
        SyncAppListChangesUseCase.Request.ApplyPackageChanges(snapshotPendingChangedPackages())
      }

      if (syncAppListChangesUseCase(syncRequest, dbItems) == SyncAppListChangesUseCase.Result.Canceled) {
        return@launch
      }

      if (requestChangeGeneration.get() == generation) {
        when (syncRequest) {
          is SyncAppListChangesUseCase.Request.ApplyPackageChanges -> {
            removePendingChangedPackages(syncRequest.changes.size)
          }

          SyncAppListChangesUseCase.Request.RefreshAll -> {
            clearPendingChangedPackages()
          }
        }
      }

      refreshList()

      updateAppListStatus(STATUS_START_REQUEST_CHANGE_END)
    } finally {
      if (requestChangeGeneration.get() == generation) {
        updateAppListStatus(STATUS_NOT_START)
        _isRequestChangeRunning.value = false
        requestChangeJob = null
        requestFeatureInitialization()
      }
    }
  }

  private fun addPendingChangedPackage(packageChangeState: PackageChangeState) {
    synchronized(pendingChangedPackagesLock) {
      pendingChangedPackages.add(packageChangeState)
    }
  }

  private fun snapshotPendingChangedPackages(): List<PackageChangeState> {
    return synchronized(pendingChangedPackagesLock) {
      pendingChangedPackages.toList()
    }
  }

  private fun removePendingChangedPackages(count: Int) {
    synchronized(pendingChangedPackagesLock) {
      repeat(count.coerceAtMost(pendingChangedPackages.size)) {
        pendingChangedPackages.removeFirst()
      }
    }
  }

  private fun clearPendingChangedPackages() {
    synchronized(pendingChangedPackagesLock) {
      pendingChangedPackages.clear()
    }
  }

  private var computeLibReferenceJob: Job? = null

  fun computeLibReference() {
    computeLibReferenceJob?.cancel()
    computeLibReferenceJob = viewModelScope.launch(Dispatchers.IO) {
      referenceIndex?.clear()
      referenceIndex = null
      _libReference.emit(null)
      val index = computeLibReferenceUseCase.buildIndex(
        ComputeLibReferenceUseCase.ReferenceConfig(
          showSystemApps = GlobalValues.isShowSystemApps,
          options = GlobalValues.libReferenceOptions
        ),
        ::updateLibRefProgress
      ) ?: return@launch
      referenceIndex = index
      matchingRules(index)
    }
  }

  private var matchingJob: Job? = null

  fun matchingRules() {
    val index = referenceIndex ?: run {
      computeLibReference()
      return
    }
    matchingRules(index)
  }

  @SuppressLint("WrongConstant")
  private fun matchingRules(index: ComputeLibReferenceUseCase.ReferenceIndex) {
    matchingJob?.cancel()
    matchingJob = viewModelScope.launch(Dispatchers.IO) {
      try {
        val refList = computeLibReferenceUseCase.matchRules(
          index,
          ComputeLibReferenceUseCase.MatchConfig(
            threshold = GlobalValues.libReferenceThreshold,
            onlyNotMarked = GlobalValues.libReferenceOptions and LibReferenceOptions.ONLY_NOT_MARKED > 0
          ),
          ::updateLibRefProgress
        )?.map { it.toLibReference() } ?: return@launch

        _libReference.emit(refList)
        _savedRefList = refList
      } finally {
        if (referenceIndex === index) {
          referenceIndex = null
        }
        index.clear()
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

  private fun LibReferenceItem.toLibReference(): LibReference {
    return LibReference(
      libName,
      rule,
      referredList,
      type
    )
  }

  private var clearApkCacheJob: Job? = null

  fun clearApkCache() {
    clearApkCacheJob?.cancel()
    clearApkCacheJob = viewModelScope.launch(Dispatchers.IO) {
      LibCheckerApp.app.externalCacheDir?.listFiles()?.forEach { it.deleteRecursively() }
      LibCheckerApp.app.requireAvailableCacheDir()
    }
  }

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
        val list = appListRepository.getItems()

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
      installedAppRepository.getApplicationList().forEach {
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
