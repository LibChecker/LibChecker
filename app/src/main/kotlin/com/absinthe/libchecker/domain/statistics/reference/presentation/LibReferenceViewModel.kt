package com.absinthe.libchecker.domain.statistics.reference.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.annotation.ACTION
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.constant.options.LibReferenceOptions
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.list.model.AppListItemViewState
import com.absinthe.libchecker.domain.app.list.usecase.BuildAppListItemViewStatesUseCase
import com.absinthe.libchecker.domain.app.repository.AppListRepository
import com.absinthe.libchecker.domain.statistics.reference.model.LibReference
import com.absinthe.libchecker.domain.statistics.reference.repository.LibReferenceSettingsRepository
import com.absinthe.libchecker.domain.statistics.reference.usecase.BuildLibReferenceDetailDialogRequestUseCase
import com.absinthe.libchecker.domain.statistics.reference.usecase.GetLibReferenceAppsUseCase
import com.absinthe.libchecker.domain.statistics.reference.usecase.LibReferenceDetailDialogRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LibReferenceViewModel(
  appListRepository: AppListRepository,
  private val buildAppListItemViewStatesUseCase: BuildAppListItemViewStatesUseCase,
  private val getLibReferenceAppsUseCase: GetLibReferenceAppsUseCase,
  private val buildLibReferenceDetailDialogRequestUseCase: BuildLibReferenceDetailDialogRequestUseCase,
  private val libReferenceSettingsRepository: LibReferenceSettingsRepository,
  libReferenceComputationControllerFactory: LibReferenceComputationController.Factory
) : ViewModel() {

  private val _libRefListFlow = MutableSharedFlow<List<LCItem>>()
  val libRefListFlow = _libRefListFlow.asSharedFlow()
  private val dbItemsFlow: Flow<List<LCItem>> = appListRepository.items
  private val _progress = MutableStateFlow(0)
  val progress = _progress.asStateFlow()
  private val libReferenceComputationController =
    libReferenceComputationControllerFactory.create(viewModelScope, ::updateProgress)
  val libReference = libReferenceComputationController.libReference
  val thresholdChanges: Flow<Int> = libReferenceSettingsRepository.thresholdChanges
  val showSystemAppsChanges: Flow<Unit> = libReferenceSettingsRepository.showSystemAppsChanges
  val colorfulRuleIconChanges: Flow<Boolean> = libReferenceSettingsRepository.colorfulRuleIconChanges

  val colorfulRuleIcon: Boolean
    get() = libReferenceSettingsRepository.colorfulRuleIcon

  private val savedRefList: List<LibReference>?
    get() = libReferenceComputationController.savedRefList

  private var hasRequestedInitialCompute = false
  private var deferredReferenceWork: DeferredReferenceWork? = null
  private var deferredReferenceWorkNeedsLoading = false
  private var searchQuery: String = ""
  private var searchableReferences: List<LibReference>? = null

  private var savedThreshold: Int
    get() = libReferenceComputationController.savedThreshold
    set(value) {
      libReferenceComputationController.savedThreshold = value
    }

  private var actionTargets: Map<String, GetLibReferenceAppsUseCase.ActionTarget> = emptyMap()

  fun requestComputeReference(
    isVisible: Boolean,
    needShowLoading: Boolean
  ): ReferenceWorkPlan? {
    if (!isVisible) {
      deferReferenceWork(DeferredReferenceWork.COMPUTE, needShowLoading)
      return null
    }
    hasRequestedInitialCompute = true
    computeLibReference()
    return ReferenceWorkPlan(shouldShowLoading = needShowLoading)
  }

  fun requestMatchRules(
    isVisible: Boolean,
    needShowLoading: Boolean
  ): ReferenceWorkPlan? {
    if (!isVisible) {
      deferReferenceWork(DeferredReferenceWork.MATCH, needShowLoading)
      return null
    }
    hasRequestedInitialCompute = true
    matchRules()
    return ReferenceWorkPlan(shouldShowLoading = needShowLoading)
  }

  fun onReferencePageVisible(hasDisplayedReferences: Boolean): ReferenceWorkPlan? {
    if (!hasRequestedInitialCompute && !hasDisplayedReferences) {
      hasRequestedInitialCompute = true
      computeLibReference()
      return ReferenceWorkPlan(shouldShowLoading = true)
    }

    val work = deferredReferenceWork ?: return null
    val needShowLoading = deferredReferenceWorkNeedsLoading || !hasDisplayedReferences
    deferredReferenceWork = null
    deferredReferenceWorkNeedsLoading = false
    hasRequestedInitialCompute = true

    when (work) {
      DeferredReferenceWork.COMPUTE -> computeLibReference()
      DeferredReferenceWork.MATCH -> matchRules()
    }
    return ReferenceWorkPlan(shouldShowLoading = needShowLoading)
  }

  fun refreshRef() {
    libReferenceComputationController.refresh()
  }

  fun getLibReferenceOptions(): Int {
    return libReferenceSettingsRepository.options
  }

  fun setLibReferenceOption(option: Int, enabled: Boolean): Int {
    val newOptions = if (enabled) {
      libReferenceSettingsRepository.options or option
    } else {
      libReferenceSettingsRepository.options and option.inv()
    }
    libReferenceSettingsRepository.options = newOptions
    return newOptions
  }

  fun getLibReferenceOptionsString(): String {
    return LibReferenceOptions.getOptionsString(libReferenceSettingsRepository.options)
  }

  fun onThresholdChanged(
    threshold: Int,
    isVisible: Boolean
  ): ReferenceWorkPlan? {
    if (threshold < savedThreshold) {
      val plan = requestMatchRules(
        isVisible = isVisible,
        needShowLoading = true
      )
      savedThreshold = threshold
      return plan
    }
    refreshRef()
    return null
  }

  fun onShowSystemAppsChanged(isVisible: Boolean): ReferenceWorkPlan? {
    return requestComputeReference(
      isVisible = isVisible,
      needShowLoading = true
    )
  }

  fun setData(name: String, @LibType type: Int) = viewModelScope.launch(Dispatchers.IO) {
    dbItemsFlow.collectLatest {
      emitLibReferenceApps(it, name, type)
    }
  }

  fun setData(
    name: String,
    @LibType type: Int,
    packagesList: List<String>
  ) = viewModelScope.launch(Dispatchers.IO) {
    val dbItemsStateFlow = dbItemsFlow.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val dbItems = dbItemsStateFlow.value.takeUnless { it.isNullOrEmpty() }
      ?: dbItemsStateFlow.filterNotNull().first()

    emitLibReferenceApps(dbItems, name, type, packagesList)
  }

  suspend fun buildAppListItemViewStates(items: List<LCItem>): Map<String, AppListItemViewState> {
    return buildAppListItemViewStatesUseCase(
      BuildAppListItemViewStatesUseCase.Request(
        items = items,
        options = libReferenceSettingsRepository.appListDisplayOptions
      )
    )
  }

  fun getActionTarget(packageName: String): Pair<String, Int>? {
    val target = actionTargets[packageName] ?: return null
    return target.name to target.type
  }

  suspend fun buildDetailDialogRequest(
    name: String,
    @LibType type: Int
  ): LibReferenceDetailDialogRequest? {
    return buildLibReferenceDetailDialogRequestUseCase(name, type)
  }

  fun onSearchQueryChanged(query: String): SearchQueryChange {
    if (searchQuery == query) {
      return SearchQueryChange(shouldRefreshItems = false)
    }
    searchQuery = query
    return SearchQueryChange(shouldRefreshItems = true)
  }

  suspend fun buildCurrentSearchResult(): SearchResult? {
    val references = searchableReferences ?: savedRefList ?: return null
    return buildSearchResult(references)
  }

  suspend fun onReferenceListChanged(references: List<LibReference>): SearchResult {
    searchableReferences = references
    return buildSearchResult(references)
  }

  private suspend fun buildSearchResult(references: List<LibReference>): SearchResult {
    val query = searchQuery
    val filteredReferences = withContext(Dispatchers.Default) {
      if (query.isEmpty()) {
        references
      } else {
        references.filter {
          it.libName.contains(query, ignoreCase = true) ||
            it.rule?.label?.contains(
              query,
              ignoreCase = true
            ) == true
        }
      }
    }
    return SearchResult(
      query = query,
      references = filteredReferences,
      shouldShowEasterEgg = query.equals(EASTER_EGG_QUERY, ignoreCase = true)
    )
  }

  private fun computeLibReference() {
    libReferenceComputationController.compute()
  }

  private fun matchRules() {
    libReferenceComputationController.cancelMatchingJob()
    libReferenceComputationController.match()
  }

  private fun deferReferenceWork(work: DeferredReferenceWork, needShowLoading: Boolean) {
    deferredReferenceWork = when {
      work == DeferredReferenceWork.COMPUTE -> DeferredReferenceWork.COMPUTE
      deferredReferenceWork == DeferredReferenceWork.COMPUTE -> DeferredReferenceWork.COMPUTE
      else -> work
    }
    deferredReferenceWorkNeedsLoading = deferredReferenceWorkNeedsLoading || needShowLoading
  }

  private fun updateProgress(progress: Int) {
    _progress.value = progress
  }

  private suspend fun emitLibReferenceApps(
    items: List<LCItem>,
    name: String,
    @LibType type: Int,
    packageNames: Collection<String>? = null
  ) {
    val result = getLibReferenceAppsUseCase(
      GetLibReferenceAppsUseCase.Request(
        items = items,
        name = name,
        type = type,
        showSystemApps = libReferenceSettingsRepository.showSystemApps,
        packageNames = packageNames
      )
    )
    actionTargets = result.actionTargets.takeIf { type == ACTION }.orEmpty()
    _libRefListFlow.emit(result.items)
  }

  data class ReferenceWorkPlan(
    val shouldShowLoading: Boolean
  )

  data class SearchQueryChange(
    val shouldRefreshItems: Boolean
  )

  data class SearchResult(
    val query: String,
    val references: List<LibReference>,
    val shouldShowEasterEgg: Boolean
  )

  private enum class DeferredReferenceWork {
    COMPUTE,
    MATCH
  }

  private companion object {
    private const val EASTER_EGG_QUERY = "Easter Egg"
  }
}
