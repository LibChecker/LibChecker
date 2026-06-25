package com.absinthe.libchecker.features.album.comparison

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.domain.snapshot.ArchiveSnapshotItem
import com.absinthe.libchecker.domain.snapshot.BuildArchiveSnapshotItemUseCase
import com.absinthe.libchecker.domain.snapshot.BuildSnapshotComparisonPlanUseCase
import com.absinthe.libchecker.domain.snapshot.BuildSnapshotPairDiffUseCase
import com.absinthe.libchecker.domain.snapshot.CompareSnapshotDiffsUseCase
import com.absinthe.libchecker.domain.snapshot.GetSnapshotDashboardCountUseCase
import com.absinthe.libchecker.domain.snapshot.SnapshotComparisonPlan
import com.absinthe.libchecker.domain.snapshot.SnapshotLibraryUseCase
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

class SnapshotComparisonViewModel(
  private val compareSnapshotDiffs: CompareSnapshotDiffsUseCase,
  private val getSnapshotDashboardCount: GetSnapshotDashboardCountUseCase,
  private val snapshotLibrary: SnapshotLibraryUseCase,
  private val buildArchiveSnapshotItemUseCase: BuildArchiveSnapshotItemUseCase,
  private val buildSnapshotPairDiffUseCase: BuildSnapshotPairDiffUseCase,
  private val buildSnapshotComparisonPlanUseCase: BuildSnapshotComparisonPlanUseCase
) : ViewModel() {

  val snapshotDiffItemsFlow: MutableSharedFlow<List<SnapshotDiffItem>> = MutableSharedFlow()
  internal var inputs: SnapshotComparisonInputs = SnapshotComparisonInputs()
    private set

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

  internal suspend fun buildSnapshotComparisonPlan(
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
    val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd, HH:mm:ss", Locale.getDefault())
    val date = Date(timestamp)
    return simpleDateFormat.format(date)
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

  sealed class Effect {
    data class DashboardCountChange(
      val snapshotCount: Int,
      val isLeft: Boolean
    ) : Effect()
  }
}
