package com.absinthe.libchecker.features.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.annotation.ACTION
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.AppListItemViewState
import com.absinthe.libchecker.domain.app.AppListRepository
import com.absinthe.libchecker.domain.app.BuildAppListItemViewStatesUseCase
import com.absinthe.libchecker.domain.statistics.GetLibReferenceAppsUseCase
import com.absinthe.libchecker.domain.statistics.LibReferenceSettingsRepository
import com.absinthe.libchecker.features.statistics.bean.LibReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibReferenceViewModel(
  appListRepository: AppListRepository,
  private val buildAppListItemViewStatesUseCase: BuildAppListItemViewStatesUseCase,
  private val getLibReferenceAppsUseCase: GetLibReferenceAppsUseCase,
  private val libReferenceSettingsRepository: LibReferenceSettingsRepository,
  libReferenceComputationControllerFactory: LibReferenceComputationController.Factory
) : ViewModel() {

  val libRefListFlow: MutableSharedFlow<List<LCItem>> = MutableSharedFlow()
  val dbItemsFlow: Flow<List<LCItem>> = appListRepository.items
  private val _progress = MutableSharedFlow<Int>()
  val progress = _progress.asSharedFlow()
  private val libReferenceComputationController =
    libReferenceComputationControllerFactory.create(viewModelScope, ::updateProgress)
  val libReference = libReferenceComputationController.libReference

  val savedRefList: List<LibReference>?
    get() = libReferenceComputationController.savedRefList

  private var hasRequestedInitialCompute = false
  private var deferredReferenceWork: DeferredReferenceWork? = null
  private var deferredReferenceWorkNeedsLoading = false

  var savedThreshold: Int
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
    viewModelScope.launch {
      _progress.emit(progress)
    }
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
    libRefListFlow.emit(result.items)
  }

  data class ReferenceWorkPlan(
    val shouldShowLoading: Boolean
  )

  private enum class DeferredReferenceWork {
    COMPUTE,
    MATCH
  }
}
