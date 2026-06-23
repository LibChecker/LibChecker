package com.absinthe.libchecker.features.home

import android.annotation.SuppressLint
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.absinthe.libchecker.domain.app.ExportAppListUseCase
import com.absinthe.libchecker.domain.app.ExportAppListToUriUseCase
import com.absinthe.libchecker.domain.app.GetAppListContentUseCase
import com.absinthe.libchecker.domain.app.InitializeAppListUseCase
import com.absinthe.libchecker.domain.app.InstalledAppRepository
import com.absinthe.libchecker.domain.app.PackageChangeState
import com.absinthe.libchecker.domain.app.SyncAppListChangesUseCase
import com.absinthe.libchecker.domain.statistics.ComputeLibReferenceUseCase
import com.absinthe.libchecker.domain.statistics.GetLibReferenceIconPackagesUseCase
import com.absinthe.libchecker.domain.statistics.LibReferenceItem
import com.absinthe.libchecker.features.statistics.bean.LibReference
import com.absinthe.libchecker.services.IWorkerService
import com.absinthe.libchecker.ui.base.IListController
import com.absinthe.libchecker.utils.extensions.requireAvailableCacheDir
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
import timber.log.Timber

class HomeViewModel(
  private val installedAppRepository: InstalledAppRepository,
  private val appListRepository: AppListRepository,
  private val initializeAppListUseCase: InitializeAppListUseCase,
  private val syncAppListChangesUseCase: SyncAppListChangesUseCase,
  private val computeLibReferenceUseCase: ComputeLibReferenceUseCase,
  private val exportAppListToUriUseCase: ExportAppListToUriUseCase,
  private val getAppListContentUseCase: GetAppListContentUseCase,
  private val getLibReferenceIconPackagesUseCase: GetLibReferenceIconPackagesUseCase
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

  suspend fun getAppListContent(
    options: Int,
    keyword: String,
    isCurrentProcess64Bit: Boolean
  ): GetAppListContentUseCase.Result {
    return getAppListContentUseCase(
      GetAppListContentUseCase.Request(
        options = options,
        keyword = keyword,
        isCurrentProcess64Bit = isCurrentProcess64Bit
      )
    )
  }

  suspend fun isOnlySelfAppInDatabase(): Boolean {
    return getAppListContentUseCase.isOnlySelfAppInDatabase()
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
      type,
      iconPackages = getLibReferenceIconPackagesUseCase(referredList)
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
  }

  fun dumpAppsInfo(uri: Uri, saveAsMarkDown: Boolean) {
    viewModelScope.launch(Dispatchers.IO) {
      val format = if (saveAsMarkDown) {
        ExportAppListUseCase.Format.Markdown
      } else {
        ExportAppListUseCase.Format.PlainText
      }
      runCatching {
        exportAppListToUriUseCase(uri, format)
      }.onFailure {
        Timber.e(it)
      }
    }
  }

  fun clearMenuState() {
    isSearchMenuExpanded = false
    currentSearchQuery = ""
  }
}
