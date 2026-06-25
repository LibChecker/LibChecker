package com.absinthe.libchecker.features.snapshot

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.domain.app.AppListRepository
import com.absinthe.libchecker.domain.snapshot.ArchiveSnapshotItem
import com.absinthe.libchecker.domain.snapshot.BuildArchiveSnapshotItemUseCase
import com.absinthe.libchecker.domain.snapshot.BuildSnapshotCapturePlanUseCase
import com.absinthe.libchecker.domain.snapshot.BuildSnapshotComparisonPlanUseCase
import com.absinthe.libchecker.domain.snapshot.BuildSnapshotDetailItemsUseCase
import com.absinthe.libchecker.domain.snapshot.BuildSnapshotPairDiffUseCase
import com.absinthe.libchecker.domain.snapshot.CompareSnapshotDiffsUseCase
import com.absinthe.libchecker.domain.snapshot.CompareSnapshotItemWithInstalledAppUseCase
import com.absinthe.libchecker.domain.snapshot.GetApexPackageNamesUseCase
import com.absinthe.libchecker.domain.snapshot.GetSnapshotDashboardCountUseCase
import com.absinthe.libchecker.domain.snapshot.GetSnapshotPackageIconSourcesUseCase
import com.absinthe.libchecker.domain.snapshot.GetSnapshotSystemPropDiffsUseCase
import com.absinthe.libchecker.domain.snapshot.SnapshotCapturePlan
import com.absinthe.libchecker.domain.snapshot.SnapshotComparisonPlan
import com.absinthe.libchecker.domain.snapshot.SnapshotLibraryUseCase
import com.absinthe.libchecker.domain.snapshot.SnapshotRepository
import com.absinthe.libchecker.domain.snapshot.SnapshotSelectionUseCase
import com.absinthe.libchecker.domain.snapshot.SnapshotSystemPropDiff
import com.absinthe.libchecker.domain.snapshot.SnapshotTrackChangeRepository
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDetailItem
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libraries.utils.manager.TimeRecorder
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
  private val snapshotLibrary: SnapshotLibraryUseCase,
  private val buildArchiveSnapshotItemUseCase: BuildArchiveSnapshotItemUseCase,
  private val buildSnapshotCapturePlanUseCase: BuildSnapshotCapturePlanUseCase,
  private val buildSnapshotPairDiffUseCase: BuildSnapshotPairDiffUseCase,
  private val buildSnapshotComparisonPlanUseCase: BuildSnapshotComparisonPlanUseCase,
  private val getSnapshotPackageIconSourcesUseCase: GetSnapshotPackageIconSourcesUseCase,
  private val getSnapshotSystemPropDiffsUseCase: GetSnapshotSystemPropDiffsUseCase,
  private val getApexPackageNamesUseCase: GetApexPackageNamesUseCase,
  private val snapshotSelectionUseCase: SnapshotSelectionUseCase,
  private val snapshotTrackChangeRepository: SnapshotTrackChangeRepository
) : ViewModel() {

  val allSnapshots = repository.currentSnapshotCount
  val snapshotDiffItemsFlow: MutableSharedFlow<List<SnapshotDiffItem>> = MutableSharedFlow()
  val snapshotDetailItemsFlow: MutableSharedFlow<List<SnapshotDetailItem>> = MutableSharedFlow()

  private val _effect: MutableSharedFlow<Effect> = MutableSharedFlow()
  val effect = _effect.asSharedFlow()

  private var compareDiffJob: Job? = null

  val selectedSnapshotTimestamp: Long
    get() = snapshotSelectionUseCase.getCurrentTimestamp()

  var currentTimeStamp: Long = selectedSnapshotTimestamp
    private set

  fun isComparingActive(): Boolean = compareDiffJob == null || compareDiffJob?.isActive == true

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
        snapshotDiffItemsFlow.emit(diffItems)
      }
      timer.end()
      Timber.d("compareDiff: $timer")
    }.also {
      it.start()
    }
  }

  suspend fun compareDiffWithSnapshotList(
    preTimeStamp: Long = -1L,
    preList: List<SnapshotItem>,
    currList: List<SnapshotItem>
  ) {
    if (preList.isEmpty()) {
      return
    }

    if (currList.isEmpty()) {
      return
    }

    val diffList = compareSnapshotDiffs.compareLists(
      previousTimestamp = preTimeStamp,
      previousItems = preList,
      currentItems = currList
    ) ?: return
    snapshotDiffItemsFlow.emit(diffList)
  }

  suspend fun buildArchiveSnapshotItem(
    uri: Uri,
    destinationFile: File,
    iconSize: Int
  ): ArchiveSnapshotItem {
    return buildArchiveSnapshotItemUseCase(uri, destinationFile, iconSize)
  }

  fun buildSnapshotPairDiff(left: SnapshotItem, right: SnapshotItem): SnapshotDiffItem {
    return buildSnapshotPairDiffUseCase(left, right)
  }

  fun buildSnapshotCapturePlan(): SnapshotCapturePlan {
    return buildSnapshotCapturePlanUseCase(selectedSnapshotTimestamp)
  }

  suspend fun buildSnapshotComparisonPlan(
    leftTimeStamp: Long,
    leftArchive: ArchiveSnapshotItem?,
    rightTimeStamp: Long,
    rightArchive: ArchiveSnapshotItem?
  ): SnapshotComparisonPlan? {
    return buildSnapshotComparisonPlanUseCase(
      leftTimeStamp = leftTimeStamp,
      leftArchive = leftArchive,
      rightTimeStamp = rightTimeStamp,
      rightArchive = rightArchive
    )
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

  fun computeDiffDetail(entity: SnapshotDiffItem) = viewModelScope.launch {
    snapshotDetailItemsFlow.emit(buildSnapshotDetailItems(entity))
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

  suspend fun getApexPackageNames(): Set<String> = getApexPackageNamesUseCase()

  suspend fun getSystemPropDiffs(timestamp: Long): List<SnapshotSystemPropDiff> {
    return getSnapshotSystemPropDiffsUseCase(timestamp)
  }

  suspend fun deleteSnapshotsAndTimeStamp(timestamp: Long) {
    snapshotLibrary.deleteTimeStamp(timestamp)
  }

  suspend fun retainLatestSnapshotsAndGetTimeStamps(count: Int): List<TimeStampItem> {
    return snapshotLibrary.retainLatestSnapshotsAndGetTimeStamps(count)
  }

  fun getFormatDateString(timestamp: Long): String {
    val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd, HH:mm:ss", Locale.getDefault())
    val date = Date(timestamp)
    return simpleDateFormat.format(date)
  }

  fun chooseComparedApk(isLeftPart: Boolean) {
    setEffect {
      Effect.ChooseComparedApk(isLeftPart)
    }
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

  fun selectLatestSnapshotTimestamp(timeStamps: List<TimeStampItem>) {
    snapshotSelectionUseCase.selectLatestOrNone(timeStamps)
    currentTimeStamp = selectedSnapshotTimestamp
  }

  fun getDashboardCount(timestamp: Long, isLeft: Boolean) = viewModelScope.launch(Dispatchers.IO) {
    Timber.d("getDashboardCount: $timestamp, $isLeft")
    val count = getSnapshotDashboardCount(timestamp)
    setEffect {
      Effect.DashboardCountChange(count.snapshotCount, count.appCount, isLeft)
    }
  }

  private fun changeDiffItem(item: SnapshotDiffItem) {
    setEffect {
      Effect.DiffItemChange(item)
    }
  }

  private fun removeDiffItem(packageName: String) {
    setEffect {
      Effect.DiffItemRemove(packageName)
    }
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

  sealed class Effect {
    data class ChooseComparedApk(val isLeftPart: Boolean) : Effect()
    data class DashboardCountChange(
      val snapshotCount: Int,
      val appCount: Int,
      val isLeft: Boolean
    ) : Effect()

    data class DiffItemChange(val item: SnapshotDiffItem) : Effect()
    data class DiffItemRemove(val packageName: String) : Effect()
    data class TimeStampChange(val timestamp: Long) : Effect()
    data class ComparingProgressChange(val progress: Int) : Effect()
  }
}
