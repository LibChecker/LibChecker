package com.absinthe.libchecker.domain.snapshot.list.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.domain.app.AppListRepository
import com.absinthe.libchecker.domain.app.PackageChangeState
import com.absinthe.libchecker.domain.snapshot.CompareSnapshotDiffsUseCase
import com.absinthe.libchecker.domain.snapshot.CompareSnapshotItemWithInstalledAppUseCase
import com.absinthe.libchecker.domain.snapshot.DeleteSnapshotTimeStampUseCase
import com.absinthe.libchecker.domain.snapshot.FormatSnapshotTimestampUseCase
import com.absinthe.libchecker.domain.snapshot.GetSnapshotDashboardCountUseCase
import com.absinthe.libchecker.domain.snapshot.SnapshotLibraryUseCase
import com.absinthe.libchecker.domain.snapshot.SnapshotRepository
import com.absinthe.libchecker.domain.snapshot.SnapshotSelectionUseCase
import com.absinthe.libchecker.domain.snapshot.SnapshotSettingsRepository
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailSection
import com.absinthe.libchecker.domain.snapshot.detail.usecase.BuildSnapshotDetailItemsUseCase
import com.absinthe.libchecker.domain.snapshot.detail.usecase.BuildSnapshotDetailSectionsUseCase
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotCapturePlan
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotSystemPropDisplayData
import com.absinthe.libchecker.domain.snapshot.list.usecase.BuildSnapshotCapturePlanUseCase
import com.absinthe.libchecker.domain.snapshot.list.usecase.BuildSnapshotListUpdatePlanUseCase
import com.absinthe.libchecker.domain.snapshot.list.usecase.BuildSnapshotSystemPropDisplayDataUseCase
import com.absinthe.libchecker.domain.snapshot.list.usecase.GetSnapshotPackageIconSourcesUseCase
import com.absinthe.libchecker.domain.snapshot.list.usecase.UpdateSnapshotDiffItemsUseCase
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.domain.snapshot.model.SnapshotPackageIconSource
import com.absinthe.libchecker.domain.snapshot.sync.SnapshotPackageChangeProcessor
import com.absinthe.libchecker.domain.snapshot.timenode.model.SnapshotTimeNodeItem
import com.absinthe.libchecker.domain.snapshot.timenode.usecase.BuildSnapshotTimeNodeItemsUseCase
import com.absinthe.libchecker.domain.snapshot.timenode.usecase.UpdateSnapshotAutoRemoveThresholdUseCase
import com.absinthe.libchecker.domain.snapshot.track.repository.SnapshotTrackChangeRepository
import com.absinthe.libraries.utils.manager.TimeRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber

const val CURRENT_SNAPSHOT = -1L

class SnapshotViewModel(
  private val repository: SnapshotRepository,
  private val appListRepository: AppListRepository,
  private val compareSnapshotDiffs: CompareSnapshotDiffsUseCase,
  private val compareSnapshotItemWithInstalledApp: CompareSnapshotItemWithInstalledAppUseCase,
  private val getSnapshotDashboardCount: GetSnapshotDashboardCountUseCase,
  private val buildSnapshotDetailItems: BuildSnapshotDetailItemsUseCase,
  private val buildSnapshotDetailSections: BuildSnapshotDetailSectionsUseCase,
  private val snapshotLibrary: SnapshotLibraryUseCase,
  private val buildSnapshotCapturePlanUseCase: BuildSnapshotCapturePlanUseCase,
  private val getSnapshotPackageIconSourcesUseCase: GetSnapshotPackageIconSourcesUseCase,
  private val buildSnapshotListUpdatePlanUseCase: BuildSnapshotListUpdatePlanUseCase,
  private val buildSnapshotSystemPropDisplayDataUseCase: BuildSnapshotSystemPropDisplayDataUseCase,
  private val buildSnapshotTimeNodeItemsUseCase: BuildSnapshotTimeNodeItemsUseCase,
  private val deleteSnapshotTimeStampUseCase: DeleteSnapshotTimeStampUseCase,
  private val formatSnapshotTimestampUseCase: FormatSnapshotTimestampUseCase,
  private val snapshotSelectionUseCase: SnapshotSelectionUseCase,
  private val snapshotSettingsRepository: SnapshotSettingsRepository,
  private val updateSnapshotAutoRemoveThresholdUseCase: UpdateSnapshotAutoRemoveThresholdUseCase,
  private val updateSnapshotDiffItemsUseCase: UpdateSnapshotDiffItemsUseCase,
  private val snapshotTrackChangeRepository: SnapshotTrackChangeRepository
) : ViewModel() {

  val allSnapshots = repository.currentSnapshotCount
  private val _snapshotDiffItemsUpdates: MutableSharedFlow<Unit> = MutableSharedFlow()
  val snapshotDiffItemsUpdates = _snapshotDiffItemsUpdates.asSharedFlow()
  val snapshotDetailSectionsFlow: MutableSharedFlow<List<SnapshotDetailSection>> = MutableSharedFlow()

  private val _effect: MutableSharedFlow<Effect> = MutableSharedFlow()
  val effect = _effect.asSharedFlow()

  private var snapshotDiffItems: List<SnapshotDiffItem> = emptyList()
  private val pendingParticleRemovePackageNames = linkedSetOf<String>()
  private var snapshotSearchKeyword: String = ""
  private var snapshotAutoCompareEnabled = true
  private var compareDiffJob: Job? = null
  private val packageChangeProcessor = SnapshotPackageChangeProcessor(::processPackageChange)

  val selectedSnapshotTimestamp: Long
    get() = snapshotSelectionUseCase.getCurrentTimestamp()

  var currentTimeStamp: Long = selectedSnapshotTimestamp
    private set

  fun isComparingActive(): Boolean = compareDiffJob == null || compareDiffJob?.isActive == true

  fun shouldAutoCompareSnapshot(): Boolean = snapshotAutoCompareEnabled

  fun onSnapshotServiceStateObserved(isComputing: Boolean) {
    snapshotAutoCompareEnabled = !isComputing
  }

  fun onSnapshotCaptureStarted() {
    snapshotAutoCompareEnabled = false
  }

  fun onSnapshotCaptureFinished(timestamp: Long) {
    refreshSnapshotTimestamp(timestamp)
    snapshotAutoCompareEnabled = true
  }

  fun compareDiff(
    preTimeStamp: Long,
    currTimeStamp: Long = CURRENT_SNAPSHOT,
    shouldClearDiff: Boolean = false
  ) {
    if (compareDiffJob?.isActive == true) {
      compareDiffJob?.cancel()
    }
    compareDiffJob = viewModelScope.launch(Dispatchers.IO) {
      currentTimeStamp = preTimeStamp
      val timer = TimeRecorder().apply { start() }

      val diffItems = compareSnapshotDiffs(
        previousTimestamp = preTimeStamp,
        currentTimestamp = currTimeStamp.takeUnless { it == CURRENT_SNAPSHOT },
        shouldClearDiff = shouldClearDiff,
        onProgress = ::changeComparingProgress
      )
      if (diffItems != null) {
        emitSnapshotDiffItemsUpdate(diffItems)
      }
      timer.end()
      Timber.d("compareDiff: $timer")
    }.also {
      it.start()
    }
  }

  fun buildSnapshotCapturePlan(): SnapshotCapturePlan {
    return buildSnapshotCapturePlanUseCase(selectedSnapshotTimestamp)
  }

  fun getSnapshotOptions(): Int {
    return snapshotSettingsRepository.options
  }

  fun getSnapshotOptionsDiff(previousOptions: Int): Int {
    return previousOptions.xor(snapshotSettingsRepository.options)
  }

  fun setSnapshotOption(option: Int, enabled: Boolean): Int {
    val newOptions = if (enabled) {
      snapshotSettingsRepository.options or option
    } else {
      snapshotSettingsRepository.options and option.inv()
    }
    snapshotSettingsRepository.options = newOptions
    return newOptions
  }

  fun getSnapshotAutoRemoveThreshold(): Int {
    return updateSnapshotAutoRemoveThresholdUseCase.currentThreshold
  }

  fun disableSnapshotAutoRemoveThreshold() {
    updateSnapshotAutoRemoveThresholdUseCase.disable()
  }

  suspend fun enableSnapshotAutoRemoveAndRetainLatest(threshold: Int): List<TimeStampItem> {
    return updateSnapshotAutoRemoveThresholdUseCase.enableAndRetainLatest(threshold)
  }

  suspend fun compareItemDiff(
    timeStamp: Long = selectedSnapshotTimestamp,
    packageName: String
  ) {
    val diffItem = compareSnapshotItemWithInstalledApp(timeStamp, packageName)

    diffItem?.let {
      changeDiffItem(it)
    } ?: run {
      removeDiffItem(packageName)
    }
  }

  fun handlePackageChanged(packageChangeState: PackageChangeState) {
    packageChangeProcessor.enqueue(viewModelScope, packageChangeState)
  }

  fun computeDiffDetail(entity: SnapshotDiffItem) = viewModelScope.launch {
    val details = buildSnapshotDetailItems(entity)
    snapshotDetailSectionsFlow.emit(buildSnapshotDetailSections(details))
  }

  fun getTimeStamps(): List<TimeStampItem> {
    return snapshotLibrary.getTimeStamps()
  }

  suspend fun getSnapshots(timestamp: Long, packageName: String? = null): List<SnapshotItem> {
    return snapshotLibrary.getSnapshots(timestamp, packageName)
  }

  suspend fun getAppListItem(packageName: String): LCItem? {
    return appListRepository.getItem(packageName)
  }

  suspend fun getSnapshotPackageIconSources(packageNames: Collection<String>) = getSnapshotPackageIconSourcesUseCase(packageNames)

  suspend fun buildSnapshotTimeNodeListData(
    timeStamps: List<TimeStampItem>
  ): SnapshotTimeNodeListData {
    val result = buildSnapshotTimeNodeItemsUseCase(timeStamps)
    return SnapshotTimeNodeListData(
      items = result.items,
      packageIconSources = getSnapshotPackageIconSourcesUseCase(result.topAppPackageNames)
    )
  }

  fun updateSnapshotSearchKeyword(keyword: String): Boolean {
    if (snapshotSearchKeyword == keyword) {
      return false
    }
    snapshotSearchKeyword = keyword
    return true
  }

  suspend fun buildSnapshotListUpdatePlan(
    currentItems: List<SnapshotDiffItem>,
    highlightRefresh: Boolean
  ): BuildSnapshotListUpdatePlanUseCase.Plan {
    val plan = buildSnapshotListUpdatePlanUseCase(
      BuildSnapshotListUpdatePlanUseCase.Request(
        currentItems = currentItems,
        sourceItems = snapshotDiffItems,
        searchKeyword = snapshotSearchKeyword,
        pendingRemovePackageNames = pendingParticleRemovePackageNames.toSet(),
        highlightRefresh = highlightRefresh
      )
    )
    pendingParticleRemovePackageNames.removeAll(plan.consumedRemovePackageNames)
    return plan
  }

  suspend fun getSystemPropDisplayData(timestamp: Long): List<SnapshotSystemPropDisplayData> {
    return buildSnapshotSystemPropDisplayDataUseCase(timestamp)
  }

  suspend fun clearSnapshotDiffItems() {
    emitSnapshotDiffItemsUpdate(emptyList())
  }

  suspend fun deleteSnapshotTimeStamp(timestamp: Long): List<TimeStampItem> {
    val result = deleteSnapshotTimeStampUseCase(timestamp)
    currentTimeStamp = result.selectedTimestamp
    return result.remainingTimeStamps
  }

  fun getFormatDateString(timestamp: Long): String {
    return formatSnapshotTimestampUseCase(timestamp)
  }

  fun consumeTrackItemsChanged(): Boolean {
    return snapshotTrackChangeRepository.consumeChanged()
  }

  fun changeTimeStamp(timestamp: Long) {
    setSelectedSnapshotTimestamp(timestamp)
    setEffect {
      Effect.TimeStampChange(timestamp)
    }
  }

  fun refreshSelectedSnapshot(shouldClearDiff: Boolean = false) {
    refreshSnapshotTimestamp(selectedSnapshotTimestamp, shouldClearDiff)
  }

  fun refreshSnapshotTimestamp(
    timestamp: Long,
    shouldClearDiff: Boolean = false
  ) {
    changeTimeStamp(timestamp)
    getDashboardCount(timestamp, true)
    compareDiff(timestamp, shouldClearDiff = shouldClearDiff)
  }

  fun getDashboardCount(timestamp: Long, isLeft: Boolean) = viewModelScope.launch(Dispatchers.IO) {
    emitDashboardCount(timestamp, isLeft)
  }

  override fun onCleared() {
    super.onCleared()
    packageChangeProcessor.cancel()
  }

  private suspend fun processPackageChange(packageChangeState: PackageChangeState) {
    compareItemDiff(packageName = packageChangeState.packageName)
    emitDashboardCount(selectedSnapshotTimestamp, true)
  }

  private suspend fun emitDashboardCount(timestamp: Long, isLeft: Boolean) {
    Timber.d("getDashboardCount: $timestamp, $isLeft")
    val count = getSnapshotDashboardCount(timestamp)
    setEffect {
      Effect.DashboardCountChange(count.snapshotCount, count.appCount, isLeft)
    }
  }

  private suspend fun changeDiffItem(item: SnapshotDiffItem) {
    val update = updateSnapshotDiffItemsUseCase.applyChange(snapshotDiffItems, item)
    emitSnapshotDiffItemsUpdate(update.items, update.pendingRemovePackageNames)
  }

  private suspend fun removeDiffItem(packageName: String) {
    val update = updateSnapshotDiffItemsUseCase.applyRemove(snapshotDiffItems, packageName)
    emitSnapshotDiffItemsUpdate(update.items, update.pendingRemovePackageNames)
  }

  private fun changeComparingProgress(progress: Int) {
    setEffect {
      Effect.ComparingProgressChange(progress)
    }
  }

  private fun setSelectedSnapshotTimestamp(timestamp: Long) {
    snapshotSelectionUseCase.setCurrentTimestamp(timestamp)
    currentTimeStamp = timestamp
  }

  private fun setEffect(builder: () -> Effect) {
    val newEffect = builder()
    viewModelScope.launch {
      _effect.emit(newEffect)
    }
  }

  private suspend fun emitSnapshotDiffItemsUpdate(
    items: List<SnapshotDiffItem>,
    pendingRemovePackageNames: Set<String> = emptySet()
  ) {
    snapshotDiffItems = items
    pendingParticleRemovePackageNames += pendingRemovePackageNames
    _snapshotDiffItemsUpdates.emit(Unit)
  }

  sealed class Effect {
    data class DashboardCountChange(
      val snapshotCount: Int,
      val appCount: Int,
      val isLeft: Boolean
    ) : Effect()

    data class TimeStampChange(val timestamp: Long) : Effect()
    data class ComparingProgressChange(val progress: Int) : Effect()
  }

  data class SnapshotTimeNodeListData(
    val items: List<SnapshotTimeNodeItem>,
    val packageIconSources: Map<String, SnapshotPackageIconSource>
  )
}
