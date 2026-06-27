package com.absinthe.libchecker.features.album.comparison

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.domain.snapshot.ArchiveSnapshotItem
import com.absinthe.libchecker.domain.snapshot.BuildSnapshotComparisonPlanUseCase
import com.absinthe.libchecker.domain.snapshot.BuildSnapshotPairDiffUseCase
import com.absinthe.libchecker.domain.snapshot.CompareSnapshotDiffsUseCase
import com.absinthe.libchecker.domain.snapshot.FormatSnapshotTimestampUseCase
import com.absinthe.libchecker.domain.snapshot.GetSnapshotDashboardCountUseCase
import com.absinthe.libchecker.domain.snapshot.SnapshotComparisonPlan
import com.absinthe.libchecker.domain.snapshot.SnapshotLibraryUseCase
import com.absinthe.libchecker.domain.snapshot.comparison.PrepareSnapshotComparisonArchivesUseCase
import com.absinthe.libchecker.domain.snapshot.comparison.SnapshotComparisonInputs
import com.absinthe.libchecker.domain.snapshot.comparison.SnapshotComparisonSide
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libraries.utils.manager.TimeRecorder
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class SnapshotComparisonViewModel(
  private val compareSnapshotDiffs: CompareSnapshotDiffsUseCase,
  private val getSnapshotDashboardCount: GetSnapshotDashboardCountUseCase,
  private val snapshotLibrary: SnapshotLibraryUseCase,
  private val buildSnapshotPairDiffUseCase: BuildSnapshotPairDiffUseCase,
  private val buildSnapshotComparisonPlanUseCase: BuildSnapshotComparisonPlanUseCase,
  private val formatSnapshotTimestampUseCase: FormatSnapshotTimestampUseCase,
  private val prepareSnapshotComparisonArchivesUseCase: PrepareSnapshotComparisonArchivesUseCase
) : ViewModel() {

  val snapshotDiffItemsFlow: MutableSharedFlow<List<SnapshotDiffItem>> = MutableSharedFlow()
  private var inputs: SnapshotComparisonInputs = SnapshotComparisonInputs()

  private val _effect: MutableSharedFlow<Effect> = MutableSharedFlow()
  val effect = _effect.asSharedFlow()
  private var compareDiffJob: Job? = null

  fun compareDiff(
    previousTimestamp: Long,
    currentTimestamp: Long
  ) {
    if (compareDiffJob?.isActive == true) {
      compareDiffJob?.cancel()
    }
    compareDiffJob = viewModelScope.launch(Dispatchers.IO) {
      val timer = TimeRecorder().apply { start() }
      val diffItems = compareSnapshotDiffs(
        previousTimestamp = previousTimestamp,
        currentTimestamp = currentTimestamp,
        shouldClearDiff = false,
        onProgress = {}
      )
      if (diffItems != null) {
        snapshotDiffItemsFlow.emit(diffItems)
      }
      timer.end()
      Timber.d("compareSelectedSnapshotDiff: $timer")
    }.also {
      it.start()
    }
  }

  suspend fun compareDiffWithSnapshotList(
    previousItems: List<SnapshotItem>,
    currentItems: List<SnapshotItem>
  ) {
    if (previousItems.isEmpty() || currentItems.isEmpty()) {
      return
    }

    val diffItems = compareSnapshotDiffs.compareLists(
      previousItems = previousItems,
      currentItems = currentItems
    ) ?: return
    snapshotDiffItemsFlow.emit(diffItems)
  }

  private suspend fun prepareSnapshotComparisonArchives(
    inputs: SnapshotComparisonInputs,
    cacheDir: File,
    iconSize: Int
  ): PrepareSnapshotComparisonArchivesUseCase.Result {
    return prepareSnapshotComparisonArchivesUseCase(
      PrepareSnapshotComparisonArchivesUseCase.Request(
        inputs = inputs,
        cacheDir = cacheDir,
        iconSize = iconSize
      )
    )
  }

  fun canCompare(): Boolean {
    return inputs.canCompare
  }

  fun needsArchivePreparation(): Boolean {
    return inputs.hasArchiveInput
  }

  suspend fun buildCompareAction(
    cacheDir: File,
    iconSize: Int
  ): CompareAction {
    val currentInputs = inputs
    val archiveResult = if (currentInputs.hasArchiveInput) {
      prepareSnapshotComparisonArchives(
        inputs = currentInputs,
        cacheDir = cacheDir,
        iconSize = iconSize
      )
    } else {
      null
    }

    val plan = buildSnapshotComparisonPlan(
      inputs = currentInputs,
      leftArchive = archiveResult?.leftArchive,
      rightArchive = archiveResult?.rightArchive
    )
    val hasNotEnoughStorageSpace = archiveResult?.hasNotEnoughStorageSpace == true

    return if (plan == null) {
      CompareAction.Invalid(hasNotEnoughStorageSpace)
    } else {
      CompareAction.Ready(plan, hasNotEnoughStorageSpace)
    }
  }

  fun clearSnapshotComparisonArchiveCache(cacheDir: File?) {
    if (!inputs.hasArchiveInput || cacheDir == null) {
      return
    }
    prepareSnapshotComparisonArchivesUseCase.clearCache(cacheDir)
  }

  fun buildSnapshotPairDiff(left: SnapshotItem, right: SnapshotItem): SnapshotDiffItem {
    return buildSnapshotPairDiffUseCase(left, right)
  }

  internal fun buildDashboardSideState(side: SnapshotComparisonSide): ComparisonDashboardStatePlanner.SideState {
    val input = when (side) {
      SnapshotComparisonSide.LEFT -> inputs.left
      SnapshotComparisonSide.RIGHT -> inputs.right
    }
    return ComparisonDashboardStatePlanner.planSideState(input, ::getFormatDateString)
  }

  private suspend fun buildSnapshotComparisonPlan(
    inputs: SnapshotComparisonInputs,
    leftArchive: ArchiveSnapshotItem?,
    rightArchive: ArchiveSnapshotItem?
  ): SnapshotComparisonPlan? {
    return buildSnapshotComparisonPlanUseCase(
      leftTimeStamp = inputs.left.timestamp,
      leftArchive = leftArchive,
      rightTimeStamp = inputs.right.timestamp,
      rightArchive = rightArchive
    )
  }

  internal fun selectSnapshot(side: SnapshotComparisonSide, timestamp: Long) {
    inputs = inputs.selectSnapshot(side, timestamp)
  }

  internal fun selectArchive(side: SnapshotComparisonSide, uri: Uri) {
    inputs = inputs.selectArchive(side, uri)
  }

  fun getTimeStamps(): List<TimeStampItem> {
    return snapshotLibrary.getTimeStamps()
  }

  fun getFormatDateString(timestamp: Long): String {
    return formatSnapshotTimestampUseCase(timestamp)
  }

  fun getDashboardCount(timestamp: Long, isLeft: Boolean) = viewModelScope.launch(Dispatchers.IO) {
    Timber.d("getComparisonDashboardCount: $timestamp, $isLeft")
    val count = getSnapshotDashboardCount(timestamp)
    setEffect {
      Effect.DashboardCountChange(count.snapshotCount, isLeft)
    }
  }

  private fun setEffect(builder: () -> Effect) {
    val newEffect = builder()
    viewModelScope.launch {
      _effect.emit(newEffect)
    }
  }

  sealed interface CompareAction {
    val hasNotEnoughStorageSpace: Boolean

    data class Invalid(
      override val hasNotEnoughStorageSpace: Boolean
    ) : CompareAction

    data class Ready(
      val plan: SnapshotComparisonPlan,
      override val hasNotEnoughStorageSpace: Boolean
    ) : CompareAction
  }

  sealed class Effect {
    data class DashboardCountChange(
      val snapshotCount: Int,
      val isLeft: Boolean
    ) : Effect()
  }
}
