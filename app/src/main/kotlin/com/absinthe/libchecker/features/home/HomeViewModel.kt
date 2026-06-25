package com.absinthe.libchecker.features.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.annotation.STATUS_INIT_END
import com.absinthe.libchecker.annotation.STATUS_NOT_START
import com.absinthe.libchecker.annotation.STATUS_START_INIT
import com.absinthe.libchecker.annotation.STATUS_START_REQUEST_CHANGE
import com.absinthe.libchecker.annotation.STATUS_START_REQUEST_CHANGE_END
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.AppListRepository
import com.absinthe.libchecker.domain.app.AppListSettingsRepository
import com.absinthe.libchecker.domain.app.ClearApkCacheUseCase
import com.absinthe.libchecker.domain.app.ExportAppListToUriUseCase
import com.absinthe.libchecker.domain.app.ExportAppListUseCase
import com.absinthe.libchecker.domain.app.FeatureInitializationRepository
import com.absinthe.libchecker.domain.app.GetAppListContentUseCase
import com.absinthe.libchecker.domain.app.InitializeAppListUseCase
import com.absinthe.libchecker.domain.app.InstalledAppRepository
import com.absinthe.libchecker.domain.app.PackageChangeState
import com.absinthe.libchecker.domain.app.SyncAppListChangesUseCase
import com.absinthe.libchecker.domain.app.sync.AppListChangeRequestQueue
import com.absinthe.libchecker.services.IWorkerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import timber.log.Timber

class HomeViewModel(
  private val installedAppRepository: InstalledAppRepository,
  private val appListRepository: AppListRepository,
  private val initializeAppListUseCase: InitializeAppListUseCase,
  private val syncAppListChangesUseCase: SyncAppListChangesUseCase,
  private val exportAppListToUriUseCase: ExportAppListToUriUseCase,
  private val getAppListContentUseCase: GetAppListContentUseCase,
  private val appListSettingsRepository: AppListSettingsRepository,
  private val clearApkCacheUseCase: ClearApkCacheUseCase,
  featureInitializationRepository: FeatureInitializationRepository
) : ViewModel() {

  val dbItemsFlow: Flow<List<LCItem>> = appListRepository.items
  val packageChanges = installedAppRepository.packageChanges
  val appListDisplayOptionsChanges = appListSettingsRepository.displayOptionsChanges

  private val _effect: MutableSharedFlow<Effect> = MutableSharedFlow()
  val effect = _effect.asSharedFlow()

  private val _isRequestChangeRunning = MutableStateFlow(false)
  private val isRequestChangeRunning = _isRequestChangeRunning.asStateFlow()
  val toolbarLoading: Flow<Boolean> = combine(
    isRequestChangeRunning,
    featureInitializationRepository.state,
    dbItemsFlow
  ) { requestChangeRunning, featureInitializationState, dbItems ->
    requestChangeRunning ||
      featureInitializationState.running ||
      dbItems.any { item -> item.features == FEATURES_NOT_INITIALIZED }
  }.distinctUntilChanged()

  var appListStatus: Int = STATUS_NOT_START
  var checkPackagesPermission: Boolean = false

  // Simple menu state management
  var isSearchMenuExpanded: Boolean = false
  var currentSearchQuery: String = ""

  private val appListChangeRequestQueue = AppListChangeRequestQueue()
  private val featureInitializationController = WorkerFeatureInitializationController()

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
    featureInitializationController.connect(binder, appListStatus)
  }

  fun disconnectWorkerBinder() {
    featureInitializationController.disconnect()
  }

  fun requestFeatureInitialization() {
    featureInitializationController.request(appListStatus)
  }

  fun getWorkerLastPackageChangedTime(): Long? {
    return featureInitializationController.getLastPackageChangedTime()
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

  private fun setEffect(builder: () -> Effect) {
    val newEffect = builder()
    viewModelScope.launch {
      _effect.emit(newEffect)
    }
  }

  suspend fun getAppListContent(
    keyword: String,
    isCurrentProcess64Bit: Boolean
  ): GetAppListContentUseCase.Result {
    return getAppListContentUseCase(
      GetAppListContentUseCase.Request(
        keyword = keyword,
        isCurrentProcess64Bit = isCurrentProcess64Bit
      )
    )
  }

  suspend fun isOnlySelfAppInDatabase(): Boolean {
    return getAppListContentUseCase.isOnlySelfAppInDatabase()
  }

  suspend fun notifyAppListDisplayOptionsChanged(diff: Int) {
    appListSettingsRepository.notifyDisplayOptionsChanged(diff)
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

  fun requestChange(packageChangeState: PackageChangeState? = null) {
    viewModelScope.launch {
      if (appListStatus == STATUS_START_INIT) {
        Timber.d("Request change canceled: STATUS_START_INIT")
        return@launch
      }
      val changeRequest = appListChangeRequestQueue.start(packageChangeState)
      requestChangeJob?.cancel()
      requestChangeJob = requestChangeImpl(changeRequest)
    }
  }

  private fun requestChangeImpl(
    changeRequest: AppListChangeRequestQueue.ChangeRequest
  ) = viewModelScope.launch(Dispatchers.IO) {
    val dbItems = appListRepository.getItems()
    if (dbItems.isEmpty()) {
      if (appListChangeRequestQueue.isCurrent(changeRequest)) {
        _isRequestChangeRunning.value = false
        updateAppListStatus(STATUS_NOT_START)
        requestChangeJob = null
      }
      return@launch
    }

    _isRequestChangeRunning.value = true
    updateAppListStatus(STATUS_START_REQUEST_CHANGE)

    try {
      val syncRequest = appListChangeRequestQueue.buildSyncRequest(changeRequest)

      if (syncAppListChangesUseCase(syncRequest, dbItems) == SyncAppListChangesUseCase.Result.Canceled) {
        return@launch
      }

      appListChangeRequestQueue.consumeSyncedRequest(changeRequest, syncRequest)

      refreshList()

      updateAppListStatus(STATUS_START_REQUEST_CHANGE_END)
    } finally {
      if (appListChangeRequestQueue.isCurrent(changeRequest)) {
        updateAppListStatus(STATUS_NOT_START)
        _isRequestChangeRunning.value = false
        requestChangeJob = null
        requestFeatureInitialization()
      }
    }
  }

  private var clearApkCacheJob: Job? = null

  fun clearApkCache() {
    clearApkCacheJob?.cancel()
    clearApkCacheJob = viewModelScope.launch {
      clearApkCacheUseCase()
    }
  }

  sealed class Effect {
    data class ReloadApps(val obj: Any? = null) : Effect()
    data class UpdateInitProgress(val progress: Int) : Effect()
    data class UpdateAppListStatus(val status: Int) : Effect()
    data class PackageChanged(val packageChangeState: PackageChangeState) : Effect()
    data class RefreshList(val obj: Any? = null) : Effect()
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

  private companion object {
    const val FEATURES_NOT_INITIALIZED = -1
  }
}
