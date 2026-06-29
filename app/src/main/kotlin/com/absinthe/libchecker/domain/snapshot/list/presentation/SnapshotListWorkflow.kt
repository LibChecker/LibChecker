package com.absinthe.libchecker.domain.snapshot.list.presentation

import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.domain.app.AppListRepository
import com.absinthe.libchecker.domain.snapshot.SnapshotRepository
import com.absinthe.libchecker.domain.snapshot.SnapshotSettingsRepository
import com.absinthe.libchecker.domain.snapshot.comparison.usecase.CompareSnapshotDiffsUseCase
import com.absinthe.libchecker.domain.snapshot.comparison.usecase.CompareSnapshotItemWithInstalledAppUseCase
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailSection
import com.absinthe.libchecker.domain.snapshot.detail.usecase.SnapshotDetailSectionBuilder
import com.absinthe.libchecker.domain.snapshot.display.FormatSnapshotTimestampUseCase
import com.absinthe.libchecker.domain.snapshot.display.SnapshotDashboardCount
import com.absinthe.libchecker.domain.snapshot.display.SnapshotDashboardCounter
import com.absinthe.libchecker.domain.snapshot.library.SnapshotLibrary
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotCapturePlan
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotSystemPropDisplayData
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotTimeNodeListData
import com.absinthe.libchecker.domain.snapshot.list.usecase.BuildSnapshotCapturePlanUseCase
import com.absinthe.libchecker.domain.snapshot.list.usecase.BuildSnapshotListUpdatePlanUseCase
import com.absinthe.libchecker.domain.snapshot.list.usecase.BuildSnapshotSystemPropDisplayDataUseCase
import com.absinthe.libchecker.domain.snapshot.list.usecase.BuildSnapshotTimeNodeListDataUseCase
import com.absinthe.libchecker.domain.snapshot.list.usecase.DeleteSnapshotTimeStampUseCase
import com.absinthe.libchecker.domain.snapshot.list.usecase.GetSnapshotPackageIconSourcesUseCase
import com.absinthe.libchecker.domain.snapshot.list.usecase.UpdateSnapshotDiffItemsUseCase
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.domain.snapshot.model.SnapshotPackageIconSource
import com.absinthe.libchecker.domain.snapshot.selection.SnapshotSelection
import com.absinthe.libchecker.domain.snapshot.timenode.usecase.UpdateSnapshotAutoRemoveThresholdUseCase
import com.absinthe.libchecker.domain.snapshot.track.repository.SnapshotTrackChangeRepository
import com.absinthe.libraries.utils.manager.TimeRecorder
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

class SnapshotListWorkflow(
  repository: SnapshotRepository,
  private val appListRepository: AppListRepository,
  private val compareSnapshotDiffs: CompareSnapshotDiffsUseCase,
  private val compareSnapshotItemWithInstalledApp: CompareSnapshotItemWithInstalledAppUseCase,
  private val snapshotDashboardCounter: SnapshotDashboardCounter,
  private val snapshotDetailSectionBuilder: SnapshotDetailSectionBuilder,
  private val snapshotLibrary: SnapshotLibrary,
  private val buildSnapshotCapturePlanUseCase: BuildSnapshotCapturePlanUseCase,
  private val getSnapshotPackageIconSourcesUseCase: GetSnapshotPackageIconSourcesUseCase,
  private val buildSnapshotListUpdatePlanUseCase: BuildSnapshotListUpdatePlanUseCase,
  private val buildSnapshotSystemPropDisplayDataUseCase: BuildSnapshotSystemPropDisplayDataUseCase,
  private val buildSnapshotTimeNodeListDataUseCase: BuildSnapshotTimeNodeListDataUseCase,
  private val deleteSnapshotTimeStampUseCase: DeleteSnapshotTimeStampUseCase,
  private val formatSnapshotTimestampUseCase: FormatSnapshotTimestampUseCase,
  private val snapshotSelection: SnapshotSelection,
  private val snapshotSettingsRepository: SnapshotSettingsRepository,
  private val updateSnapshotAutoRemoveThresholdUseCase: UpdateSnapshotAutoRemoveThresholdUseCase,
  private val updateSnapshotDiffItemsUseCase: UpdateSnapshotDiffItemsUseCase,
  private val snapshotTrackChangeRepository: SnapshotTrackChangeRepository
) {

  val currentSnapshotCount: Flow<Int> = repository.currentSnapshotCount

  private var snapshotDiffItems: List<SnapshotDiffItem> = emptyList()
  private val pendingParticleRemovePackageNames = linkedSetOf<String>()
  private var snapshotSearchKeyword: String = ""

  val selectedSnapshotTimestamp: Long
    get() = snapshotSelection.getCurrentTimestamp()

  suspend fun compareDiff(
    previousTimestamp: Long,
    currentTimestamp: Long?,
    shouldClearDiff: Boolean,
    onProgress: (Int) -> Unit
  ): List<SnapshotDiffItem>? {
    val timer = TimeRecorder().apply { start() }
    val diffItems = compareSnapshotDiffs(
      previousTimestamp = previousTimestamp,
      currentTimestamp = currentTimestamp,
      shouldClearDiff = shouldClearDiff,
      onProgress = onProgress
    )
    timer.end()
    Timber.d("compareDiff: $timer")
    if (diffItems != null) {
      snapshotDiffItems = diffItems
    }
    return diffItems
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
  ): SnapshotDiffItem? {
    return compareSnapshotItemWithInstalledApp(timeStamp, packageName)
  }

  suspend fun buildSnapshotDetailSections(entity: SnapshotDiffItem): List<SnapshotDetailSection> {
    return snapshotDetailSectionBuilder(entity)
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

  suspend fun getSnapshotPackageIconSources(
    packageNames: Collection<String>
  ): Map<String, SnapshotPackageIconSource> {
    return getSnapshotPackageIconSourcesUseCase(packageNames)
  }

  suspend fun buildSnapshotTimeNodeListData(
    timeStamps: List<TimeStampItem>
  ): SnapshotTimeNodeListData {
    return buildSnapshotTimeNodeListDataUseCase(timeStamps)
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

  fun clearSnapshotDiffItems() {
    snapshotDiffItems = emptyList()
  }

  suspend fun deleteSnapshotTimeStamp(timestamp: Long): DeleteSnapshotTimeStampUseCase.Result {
    return deleteSnapshotTimeStampUseCase(timestamp)
  }

  fun getFormatDateString(timestamp: Long): String {
    return formatSnapshotTimestampUseCase(timestamp)
  }

  fun consumeTrackItemsChanged(): Boolean {
    return snapshotTrackChangeRepository.consumeChanged()
  }

  fun setSelectedSnapshotTimestamp(timestamp: Long) {
    snapshotSelection.setCurrentTimestamp(timestamp)
  }

  suspend fun getDashboardCount(timestamp: Long): SnapshotDashboardCount {
    Timber.d("getDashboardCount: $timestamp")
    return snapshotDashboardCounter(timestamp)
  }

  fun applyDiffItemChange(
    item: SnapshotDiffItem
  ): List<SnapshotDiffItem> {
    val result = updateSnapshotDiffItemsUseCase.applyChange(snapshotDiffItems, item)
    trackPendingRemovals(result)
    snapshotDiffItems = result.items
    return result.items
  }

  fun applyDiffItemRemove(
    packageName: String
  ): List<SnapshotDiffItem> {
    val result = updateSnapshotDiffItemsUseCase.applyRemove(snapshotDiffItems, packageName)
    trackPendingRemovals(result)
    snapshotDiffItems = result.items
    return result.items
  }

  private fun trackPendingRemovals(result: UpdateSnapshotDiffItemsUseCase.Result) {
    pendingParticleRemovePackageNames += result.pendingRemovePackageNames
  }
}
