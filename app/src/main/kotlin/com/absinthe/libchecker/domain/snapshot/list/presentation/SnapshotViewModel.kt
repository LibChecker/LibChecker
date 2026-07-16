package com.absinthe.libchecker.domain.snapshot.list.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.domain.app.model.PackageChangeState
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailContent
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotCapturePlan
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotSystemPropDisplayData
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotTimeNodeListData
import com.absinthe.libchecker.domain.snapshot.list.usecase.BuildSnapshotListUpdatePlanUseCase
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.domain.snapshot.sync.SnapshotPackageChangeProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

const val CURRENT_SNAPSHOT = -1L

class SnapshotViewModel(
  private val snapshotListWorkflow: SnapshotListWorkflow
) : ViewModel() {

  val allSnapshots = snapshotListWorkflow.currentSnapshotCount
  private val _snapshotDiffItemsUpdates: MutableSharedFlow<Unit> = MutableSharedFlow()
  val snapshotDiffItemsUpdates = _snapshotDiffItemsUpdates.asSharedFlow()
  val snapshotDetailContentFlow: MutableSharedFlow<SnapshotDetailContent> = MutableSharedFlow()

  private val _effect: MutableSharedFlow<Effect> = MutableSharedFlow()
  val effect = _effect.asSharedFlow()
  private val _comparingProgress = MutableStateFlow(0)
  val comparingProgress = _comparingProgress.asStateFlow()

  private var snapshotAutoCompareEnabled = true
  private var compareDiffJob: Job? = null
  private val packageChangeProcessor = SnapshotPackageChangeProcessor(::processPackageChange)

  val selectedSnapshotTimestamp: Long
    get() = snapshotListWorkflow.selectedSnapshotTimestamp

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
      val diffItems = snapshotListWorkflow.compareDiff(
        previousTimestamp = preTimeStamp,
        currentTimestamp = currTimeStamp.takeUnless { it == CURRENT_SNAPSHOT },
        shouldClearDiff = shouldClearDiff,
        onProgress = ::changeComparingProgress
      )
      if (diffItems != null) {
        emitSnapshotDiffItemsUpdate()
      }
    }.also {
      it.start()
    }
  }

  fun buildSnapshotCapturePlan(): SnapshotCapturePlan {
    return snapshotListWorkflow.buildSnapshotCapturePlan()
  }

  fun getSnapshotOptions(): Int {
    return snapshotListWorkflow.getSnapshotOptions()
  }

  fun getSnapshotOptionsDiff(previousOptions: Int): Int {
    return snapshotListWorkflow.getSnapshotOptionsDiff(previousOptions)
  }

  fun setSnapshotOption(option: Int, enabled: Boolean): Int {
    return snapshotListWorkflow.setSnapshotOption(option, enabled)
  }

  fun getSnapshotAutoRemoveThreshold(): Int {
    return snapshotListWorkflow.getSnapshotAutoRemoveThreshold()
  }

  fun disableSnapshotAutoRemoveThreshold() {
    snapshotListWorkflow.disableSnapshotAutoRemoveThreshold()
  }

  suspend fun enableSnapshotAutoRemoveAndRetainLatest(threshold: Int): List<TimeStampItem> {
    return snapshotListWorkflow.enableSnapshotAutoRemoveAndRetainLatest(threshold)
  }

  suspend fun compareItemDiff(
    timeStamp: Long = selectedSnapshotTimestamp,
    packageName: String
  ) {
    val diffItem = snapshotListWorkflow.compareItemDiff(timeStamp, packageName)

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
    snapshotDetailContentFlow.emit(snapshotListWorkflow.buildSnapshotDetailContent(entity))
  }

  suspend fun getTimeStamps(): List<TimeStampItem> {
    return snapshotListWorkflow.getTimeStamps()
  }

  suspend fun getSnapshots(timestamp: Long, packageName: String? = null): List<SnapshotItem> {
    return snapshotListWorkflow.getSnapshots(timestamp, packageName)
  }

  suspend fun getAppListItem(packageName: String): LCItem? {
    return snapshotListWorkflow.getAppListItem(packageName)
  }

  suspend fun getSnapshotPackageIconSources(packageNames: Collection<String>) = snapshotListWorkflow.getSnapshotPackageIconSources(packageNames)

  suspend fun buildSnapshotTimeNodeListData(
    timeStamps: List<TimeStampItem>
  ): SnapshotTimeNodeListData {
    return snapshotListWorkflow.buildSnapshotTimeNodeListData(timeStamps)
  }

  fun updateSnapshotSearchKeyword(keyword: String): Boolean {
    return snapshotListWorkflow.updateSnapshotSearchKeyword(keyword)
  }

  suspend fun buildSnapshotListUpdatePlan(
    currentItems: List<SnapshotDiffItem>,
    highlightRefresh: Boolean
  ): BuildSnapshotListUpdatePlanUseCase.Plan {
    return snapshotListWorkflow.buildSnapshotListUpdatePlan(
      currentItems = currentItems,
      highlightRefresh = highlightRefresh
    )
  }

  suspend fun getSystemPropDisplayData(timestamp: Long): List<SnapshotSystemPropDisplayData> {
    return snapshotListWorkflow.getSystemPropDisplayData(timestamp)
  }

  suspend fun clearSnapshotDiffItems() {
    snapshotListWorkflow.clearSnapshotDiffItems()
    emitSnapshotDiffItemsUpdate()
  }

  suspend fun deleteSnapshotTimeStamp(timestamp: Long): List<TimeStampItem> {
    val result = snapshotListWorkflow.deleteSnapshotTimeStamp(timestamp)
    currentTimeStamp = result.selectedTimestamp
    return result.remainingTimeStamps
  }

  fun getFormatDateString(timestamp: Long): String {
    return snapshotListWorkflow.getFormatDateString(timestamp)
  }

  fun consumeTrackItemsChanged(): Boolean {
    return snapshotListWorkflow.consumeTrackItemsChanged()
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
    val count = snapshotListWorkflow.getDashboardCount(timestamp)
    setEffect {
      Effect.DashboardCountChange(count.snapshotCount, count.appCount, isLeft)
    }
  }

  private suspend fun changeDiffItem(item: SnapshotDiffItem) {
    snapshotListWorkflow.applyDiffItemChange(item)
    emitSnapshotDiffItemsUpdate()
  }

  private suspend fun removeDiffItem(packageName: String) {
    snapshotListWorkflow.applyDiffItemRemove(packageName)
    emitSnapshotDiffItemsUpdate()
  }

  private fun changeComparingProgress(progress: Int) {
    _comparingProgress.value = progress
  }

  private fun setSelectedSnapshotTimestamp(timestamp: Long) {
    snapshotListWorkflow.setSelectedSnapshotTimestamp(timestamp)
    currentTimeStamp = timestamp
  }

  private fun setEffect(builder: () -> Effect) {
    val newEffect = builder()
    viewModelScope.launch {
      _effect.emit(newEffect)
    }
  }

  private suspend fun emitSnapshotDiffItemsUpdate() {
    _snapshotDiffItemsUpdates.emit(Unit)
  }

  sealed class Effect {
    data class DashboardCountChange(
      val snapshotCount: Int,
      val appCount: Int,
      val isLeft: Boolean
    ) : Effect()

    data class TimeStampChange(val timestamp: Long) : Effect()
  }
}
