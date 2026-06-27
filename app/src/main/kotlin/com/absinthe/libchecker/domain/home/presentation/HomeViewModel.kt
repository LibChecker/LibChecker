package com.absinthe.libchecker.domain.home.presentation

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
import com.absinthe.libchecker.domain.app.InitializeAppListUseCase
import com.absinthe.libchecker.domain.app.InstalledAppRepository
import com.absinthe.libchecker.domain.app.ObserveAppListLoadingUseCase
import com.absinthe.libchecker.domain.app.PackageChangeState
import com.absinthe.libchecker.domain.app.SyncAppListChangesUseCase
import com.absinthe.libchecker.domain.app.list.export.ExportAppListToUriUseCase
import com.absinthe.libchecker.domain.app.list.export.ExportAppListUseCase
import com.absinthe.libchecker.domain.app.list.usecase.AppListItemsEquivalenceUseCase
import com.absinthe.libchecker.domain.app.list.usecase.BuildAppListUpdatePlanUseCase
import com.absinthe.libchecker.domain.app.list.usecase.GetAppListContentUseCase
import com.absinthe.libchecker.domain.app.search.HandleAppListSearchCommandUseCase
import com.absinthe.libchecker.domain.app.sync.AppListChangeRequestQueue
import com.absinthe.libchecker.services.IWorkerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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
  private val buildAppListUpdatePlanUseCase: BuildAppListUpdatePlanUseCase,
  private val handleAppListSearchCommandUseCase: HandleAppListSearchCommandUseCase,
  private val appListSettingsRepository: AppListSettingsRepository,
  private val clearApkCacheUseCase: ClearApkCacheUseCase,
  appListItemsEquivalenceUseCase: AppListItemsEquivalenceUseCase,
  observeAppListLoadingUseCase: ObserveAppListLoadingUseCase
) : ViewModel() {

  val dbItemsFlow: Flow<List<LCItem>> = appListRepository.items
  val displayItemsFlow: Flow<List<LCItem>> =
    appListRepository.items.distinctUntilChanged(appListItemsEquivalenceUseCase::invoke)
  val packageChanges = installedAppRepository.packageChanges
  val appListDisplayOptionsChanges = appListSettingsRepository.displayOptionsChanges

  private val _effect: MutableSharedFlow<Effect> = MutableSharedFlow()
  val effect = _effect.asSharedFlow()

  private val _isRequestChangeRunning = MutableStateFlow(false)
  private val isRequestChangeRunning = _isRequestChangeRunning.asStateFlow()
  val toolbarLoading: Flow<Boolean> = observeAppListLoadingUseCase(isRequestChangeRunning, dbItemsFlow)

  var appListStatus: Int = STATUS_NOT_START

  private var toolbarSearchMenuState = ToolbarSearchMenuState()
  private var pendingPackagesPermissionCheck = false
  private var pendingReturnTopAfterRequestChange = false
  private var hasUserScrolledAppList = false
  private var appListSearchKeyword: String = ""

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
    pendingReturnTopAfterRequestChange = true
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

  suspend fun buildAppListUpdate(
    isCurrentProcess64Bit: Boolean,
    currentItems: List<LCItem>,
    highlightRefresh: Boolean
  ): AppListUpdate {
    val content = getAppListContentUseCase(
      GetAppListContentUseCase.Request(
        keyword = appListSearchKeyword,
        isCurrentProcess64Bit = isCurrentProcess64Bit
      )
    )
    return when (content) {
      GetAppListContentUseCase.Result.OnlySelf -> AppListUpdate.OnlySelf

      is GetAppListContentUseCase.Result.Content -> {
        AppListUpdate.Content(
          buildAppListUpdatePlanUseCase(
            BuildAppListUpdatePlanUseCase.Request(
              currentItems = currentItems,
              content = content,
              pendingReturnTopAfterRequestChange = pendingReturnTopAfterRequestChange,
              highlightRefresh = highlightRefresh,
              hasUserScrolledList = hasUserScrolledAppList
            )
          )
        )
      }
    }
  }

  suspend fun isOnlySelfAppInDatabase(): Boolean {
    return getAppListContentUseCase.isOnlySelfAppInDatabase()
  }

  fun onAppListUserScrolled() {
    hasUserScrolledAppList = true
  }

  fun onAppListViewCreated() {
    pendingReturnTopAfterRequestChange = false
    hasUserScrolledAppList = false
  }

  fun onAppListUpdatePlanApplied(plan: BuildAppListUpdatePlanUseCase.Plan) {
    if (plan.shouldClearPendingReturnTopAfterRequestChange) {
      pendingReturnTopAfterRequestChange = false
    }
  }

  fun setPackagesPermissionCheckPending(pending: Boolean) {
    pendingPackagesPermissionCheck = pending
  }

  fun shouldCheckPackagesPermissionOnResume(): Boolean {
    return pendingPackagesPermissionCheck
  }

  fun onPackagesPermissionResult(isGranted: Boolean): Boolean {
    if (isGranted) {
      pendingPackagesPermissionCheck = false
    }
    return isGranted
  }

  fun saveToolbarSearchMenuState(isExpanded: Boolean, query: String) {
    toolbarSearchMenuState = ToolbarSearchMenuState(
      isExpanded = isExpanded,
      query = if (isExpanded) query else ""
    )
  }

  fun getToolbarSearchMenuState(): ToolbarSearchMenuState {
    return toolbarSearchMenuState
  }

  fun getAppListAdvancedMenuState(): AppListAdvancedMenuState {
    return AppListAdvancedMenuState(
      displayOptions = appListSettingsRepository.displayOptions,
      itemDisplayOptions = appListSettingsRepository.itemDisplayOptions,
      colorfulRuleIcon = appListSettingsRepository.colorfulRuleIcon
    )
  }

  fun setAppListDisplayOptions(options: Int): Int {
    appListSettingsRepository.setDisplayOptions(options)
    return options
  }

  fun setAppListItemDisplayOptions(options: Int): Int {
    appListSettingsRepository.setItemDisplayOptions(options)
    return options
  }

  fun onAppListAdvancedMenuDismissed(
    displayOptionsDiff: Int,
    itemDisplayOptionsDiff: Int
  ): AppListAdvancedMenuDismissPlan {
    if (displayOptionsDiff > 0) {
      viewModelScope.launch {
        appListSettingsRepository.notifyDisplayOptionsChanged(displayOptionsDiff)
      }
    }
    return AppListAdvancedMenuDismissPlan(
      shouldRefreshItems = displayOptionsDiff > 0 || itemDisplayOptionsDiff > 0
    )
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

  fun onAppListSearchQueryChanged(query: String): AppListSearchQueryChange {
    if (appListSearchKeyword == query) {
      return AppListSearchQueryChange(
        shouldRefreshItems = false,
        action = AppListSearchCommandAction.None
      )
    }
    appListSearchKeyword = query
    return AppListSearchQueryChange(
      shouldRefreshItems = true,
      action = handleAppListSearchQuery(query)
    )
  }

  private fun handleAppListSearchQuery(query: String): AppListSearchCommandAction {
    return when (val result = handleAppListSearchCommandUseCase(query)) {
      HandleAppListSearchCommandUseCase.Result.None -> AppListSearchCommandAction.None

      HandleAppListSearchCommandUseCase.Result.EasterEgg -> AppListSearchCommandAction.EasterEgg

      HandleAppListSearchCommandUseCase.Result.DebugModeEnabled -> AppListSearchCommandAction.DebugModeEnabled

      HandleAppListSearchCommandUseCase.Result.UserModeEnabled -> AppListSearchCommandAction.UserModeEnabled

      is HandleAppListSearchCommandUseCase.Result.DumpAppsInfo -> {
        AppListSearchCommandAction.DumpAppsInfo(
          fileName = result.fileName,
          format = result.format
        )
      }
    }
  }

  sealed class Effect {
    data class ReloadApps(val obj: Any? = null) : Effect()
    data class UpdateInitProgress(val progress: Int) : Effect()
    data class UpdateAppListStatus(val status: Int) : Effect()
    data class PackageChanged(val packageChangeState: PackageChangeState) : Effect()
    data class RefreshList(val obj: Any? = null) : Effect()
  }

  data class AppListAdvancedMenuState(
    val displayOptions: Int,
    val itemDisplayOptions: Int,
    val colorfulRuleIcon: Boolean
  )

  data class AppListAdvancedMenuDismissPlan(
    val shouldRefreshItems: Boolean
  )

  data class ToolbarSearchMenuState(
    val isExpanded: Boolean = false,
    val query: String = ""
  )

  data class AppListSearchQueryChange(
    val shouldRefreshItems: Boolean,
    val action: AppListSearchCommandAction
  )

  sealed interface AppListUpdate {
    data object OnlySelf : AppListUpdate
    data class Content(val plan: BuildAppListUpdatePlanUseCase.Plan) : AppListUpdate
  }

  sealed interface AppListSearchCommandAction {
    data object None : AppListSearchCommandAction
    data object EasterEgg : AppListSearchCommandAction
    data object DebugModeEnabled : AppListSearchCommandAction
    data object UserModeEnabled : AppListSearchCommandAction
    data class DumpAppsInfo(
      val fileName: String,
      val format: ExportAppListUseCase.Format
    ) : AppListSearchCommandAction
  }

  fun dumpAppsInfo(
    uri: Uri,
    format: ExportAppListUseCase.Format
  ) {
    viewModelScope.launch(Dispatchers.IO) {
      runCatching {
        exportAppListToUriUseCase(uri, format)
      }.onFailure {
        Timber.e(it)
      }
    }
  }

  fun clearMenuState() {
    toolbarSearchMenuState = ToolbarSearchMenuState()
  }
}
