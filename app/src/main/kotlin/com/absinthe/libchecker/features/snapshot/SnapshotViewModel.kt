package com.absinthe.libchecker.features.snapshot

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.domain.app.AppListRepository
import com.absinthe.libchecker.domain.snapshot.ArchiveSnapshotItem
import com.absinthe.libchecker.domain.snapshot.BuildArchiveSnapshotItemUseCase
import com.absinthe.libchecker.domain.snapshot.BuildSnapshotComparisonListsUseCase
import com.absinthe.libchecker.domain.snapshot.BuildSnapshotDetailItemsUseCase
import com.absinthe.libchecker.domain.snapshot.BuildSnapshotPairDiffUseCase
import com.absinthe.libchecker.domain.snapshot.CompareSnapshotItemWithInstalledAppUseCase
import com.absinthe.libchecker.domain.snapshot.CompareSnapshotItemsUseCase
import com.absinthe.libchecker.domain.snapshot.CompareSnapshotListsUseCase
import com.absinthe.libchecker.domain.snapshot.CompareSnapshotWithInstalledAppsUseCase
import com.absinthe.libchecker.domain.snapshot.GetSnapshotDashboardCountUseCase
import com.absinthe.libchecker.domain.snapshot.SnapshotArchiveUseCase
import com.absinthe.libchecker.domain.snapshot.SnapshotComparisonLists
import com.absinthe.libchecker.domain.snapshot.SnapshotLibraryUseCase
import com.absinthe.libchecker.domain.snapshot.SnapshotRepository
import com.absinthe.libchecker.domain.snapshot.UpdateSnapshotTopAppsUseCase
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDetailItem
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.ui.base.BaseAlertDialogBuilder
import com.absinthe.libraries.utils.manager.TimeRecorder
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

const val CURRENT_SNAPSHOT = -1L

class SnapshotViewModel(
  private val repository: SnapshotRepository,
  private val appListRepository: AppListRepository,
  private val compareSnapshotItems: CompareSnapshotItemsUseCase,
  private val compareSnapshotLists: CompareSnapshotListsUseCase,
  private val compareSnapshotWithInstalledApps: CompareSnapshotWithInstalledAppsUseCase,
  private val compareSnapshotItemWithInstalledApp: CompareSnapshotItemWithInstalledAppUseCase,
  private val getSnapshotDashboardCount: GetSnapshotDashboardCountUseCase,
  private val updateSnapshotTopApps: UpdateSnapshotTopAppsUseCase,
  private val buildSnapshotDetailItems: BuildSnapshotDetailItemsUseCase,
  private val snapshotArchive: SnapshotArchiveUseCase,
  private val snapshotLibrary: SnapshotLibraryUseCase,
  private val buildArchiveSnapshotItemUseCase: BuildArchiveSnapshotItemUseCase,
  private val buildSnapshotPairDiffUseCase: BuildSnapshotPairDiffUseCase,
  private val buildSnapshotComparisonListsUseCase: BuildSnapshotComparisonListsUseCase
) : ViewModel() {

  val allSnapshots = repository.currentSnapshotCount
  val snapshotDiffItemsFlow: MutableSharedFlow<List<SnapshotDiffItem>> = MutableSharedFlow()
  val snapshotDetailItemsFlow: MutableSharedFlow<List<SnapshotDetailItem>> = MutableSharedFlow()

  private val _effect: MutableSharedFlow<Effect> = MutableSharedFlow()
  val effect = _effect.asSharedFlow()

  private var compareDiffJob: Job? = null

  var currentTimeStamp: Long = GlobalValues.snapshotTimestamp
    private set

  fun isComparingActive(): Boolean = compareDiffJob == null || compareDiffJob?.isActive == true

  fun compareDiff(
    context: Context,
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

      if (shouldClearDiff) {
        repository.deleteAllSnapshotDiffItems()
      }

      if (currTimeStamp == CURRENT_SNAPSHOT) {
        compareDiffWithInstalledApps(context, preTimeStamp)
      } else {
        compareDiffWithSnapshotList(preTimeStamp, currTimeStamp)
      }
      timer.end()
      Timber.d("compareDiff: $timer")
    }.also {
      it.start()
    }
  }

  private suspend fun compareDiffWithInstalledApps(context: Context, preTimeStamp: Long) {
    val diffList = compareSnapshotWithInstalledApps(
      packageManager = context.packageManager,
      timestamp = preTimeStamp,
      onProgress = ::changeComparingProgress
    ) ?: return
    snapshotDiffItemsFlow.emit(diffList)
    if (diffList.isNotEmpty()) {
      updateSnapshotTopApps(preTimeStamp, diffList.subList(0, (diffList.size - 1).coerceAtMost(5)))
    }
  }

  private suspend fun compareDiffWithSnapshotList(preTimeStamp: Long, currTimeStamp: Long) {
    val preMap = repository.getSnapshots(preTimeStamp).associateBy { it.packageName }
    if (preMap.isEmpty()) {
      return
    }

    val currMap = repository.getSnapshots(currTimeStamp).associateBy { it.packageName }
    if (currMap.isEmpty()) {
      return
    }

    compareDiffWithSnapshotList(preTimeStamp, preMap.values.toList(), currMap.values.toList())
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

    val trackPackageNames = repository.getTrackItems()
      .asSequence()
      .map { it.packageName }
      .toSet()
    val diffList = compareSnapshotLists(preList, currList, trackPackageNames)

    snapshotDiffItemsFlow.emit(diffList)
    if (diffList.isNotEmpty() && preTimeStamp != -1L) {
      updateSnapshotTopApps(preTimeStamp, diffList.subList(0, (diffList.size - 1).coerceAtMost(5)))
    }
  }

  suspend fun buildArchiveSnapshotItem(
    uri: Uri,
    destinationFile: File,
    iconSize: Int
  ): ArchiveSnapshotItem {
    return withContext(Dispatchers.IO) {
      buildArchiveSnapshotItemUseCase(uri, destinationFile, iconSize)
    }
  }

  fun buildSnapshotPairDiff(left: SnapshotItem, right: SnapshotItem): SnapshotDiffItem {
    return buildSnapshotPairDiffUseCase(left, right)
  }

  suspend fun buildSnapshotComparisonLists(
    leftTimeStamp: Long,
    leftPackage: SnapshotItem?,
    rightTimeStamp: Long,
    rightPackage: SnapshotItem?
  ): SnapshotComparisonLists? {
    return withContext(Dispatchers.IO) {
      buildSnapshotComparisonListsUseCase(
        leftTimeStamp = leftTimeStamp,
        leftPackage = leftPackage,
        rightTimeStamp = rightTimeStamp,
        rightPackage = rightPackage
      )
    }
  }

  suspend fun compareItemDiff(
    packageManager: PackageManager,
    timeStamp: Long = GlobalValues.snapshotTimestamp,
    packageName: String
  ) {
    val diffItem = compareSnapshotItemWithInstalledApp(packageManager, timeStamp, packageName)

    diffItem?.let {
      changeDiffItem(it)
    } ?: run {
      removeDiffItem(packageName)
    }
  }

  fun computeDiffDetail(context: Context, entity: SnapshotDiffItem) = viewModelScope.launch(Dispatchers.IO) {
    snapshotDetailItemsFlow.emit(buildSnapshotDetailItems(context, entity))
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

  suspend fun getTimeStampSystemProps(timestamp: Long): String? {
    return snapshotLibrary.getSystemProps(timestamp)
  }

  suspend fun deleteSnapshotsAndTimeStamp(timestamp: Long) {
    snapshotLibrary.deleteTimeStamp(timestamp)
  }

  suspend fun retainLatestSnapshotsAndGetTimeStamps(count: Int): List<TimeStampItem> {
    return snapshotLibrary.retainLatestSnapshotsAndGetTimeStamps(count)
  }

  fun backup(os: OutputStream, resultAction: () -> Unit) = viewModelScope.launch(Dispatchers.IO) {
    snapshotArchive.backup(os)
    withContext(Dispatchers.Main) {
      resultAction()
    }
  }

  fun restore(
    context: Context,
    inputStream: InputStream,
    resultAction: (success: Boolean) -> Unit
  ) {
    viewModelScope.launch(Dispatchers.IO) {
      runCatching {
        snapshotArchive.restore(inputStream)
      }.onFailure {
        Timber.e("restore with new format failed: $it")
        withContext(Dispatchers.Main) {
          resultAction(false)
        }
        return@launch
      }.onSuccess { result ->
        result.latestTimeStamp?.let { GlobalValues.snapshotTimestamp = it }
        withContext(Dispatchers.Main) {
          resultAction(true)
        }

        val message = buildString {
          result.timeStampCounts.forEach {
            append(
              context.getString(
                R.string.album_restore_detail,
                getFormatDateString(it.key),
                it.value.toString()
              )
            )
          }
        }

        withContext(Dispatchers.Main) {
          BaseAlertDialogBuilder(context)
            .setTitle(R.string.album_restore)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
              resultAction(true)
            }
            .setCancelable(true)
            .show()
        }
      }
    }
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

  fun changeTimeStamp(timestamp: Long) {
    setEffect {
      Effect.TimeStampChange(timestamp)
    }
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
