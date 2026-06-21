package com.absinthe.libchecker.features.snapshot

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.data.app.LocalAppDataSource
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.database.entity.SnapshotDiffStoringItem
import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.domain.app.AppListRepository
import com.absinthe.libchecker.domain.snapshot.BuildSnapshotDetailItemsUseCase
import com.absinthe.libchecker.domain.snapshot.CompareSnapshotItemsUseCase
import com.absinthe.libchecker.domain.snapshot.CompareSnapshotListsUseCase
import com.absinthe.libchecker.domain.snapshot.SnapshotArchiveUseCase
import com.absinthe.libchecker.domain.snapshot.SnapshotItemFactory
import com.absinthe.libchecker.domain.snapshot.SnapshotLibraryUseCase
import com.absinthe.libchecker.domain.snapshot.SnapshotRepository
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDetailItem
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.ui.base.BaseAlertDialogBuilder
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.getPackageSize
import com.absinthe.libchecker.utils.extensions.getVersionCode
import com.absinthe.libchecker.utils.fromJson
import com.absinthe.libchecker.utils.toJson
import com.absinthe.libraries.utils.manager.TimeRecorder
import java.io.IOException
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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber

const val CURRENT_SNAPSHOT = -1L

class SnapshotViewModel(
  private val repository: SnapshotRepository,
  private val appListRepository: AppListRepository,
  private val snapshotItemFactory: SnapshotItemFactory,
  private val compareSnapshotItems: CompareSnapshotItemsUseCase,
  private val compareSnapshotLists: CompareSnapshotListsUseCase,
  private val buildSnapshotDetailItems: BuildSnapshotDetailItemsUseCase,
  private val snapshotArchive: SnapshotArchiveUseCase,
  private val snapshotLibrary: SnapshotLibraryUseCase
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
        compareDiffWithApplicationList(context, preTimeStamp)
      } else {
        compareDiffWithSnapshotList(preTimeStamp, currTimeStamp)
      }
      timer.end()
      Timber.d("compareDiff: $timer")
    }.also {
      it.start()
    }
  }

  private fun compareDiffWithApplicationList(context: Context, preTimeStamp: Long) = runBlocking {
    val preMap = repository.getSnapshots(preTimeStamp).associateBy { it.packageName }

    if (preMap.isEmpty() || preTimeStamp == 0L) {
      snapshotDiffItemsFlow.emit(emptyList())
      return@runBlocking
    }

    val packageManager = context.packageManager
    val currMap = LocalAppDataSource.getApplicationMap(true)

    val diffList = mutableListOf<SnapshotDiffItem>()
    val trackPackageNames = repository.getTrackItems()
      .asSequence()
      .map { it.packageName }
      .toSet()
    val size = currMap.size

    var count = 0
    var snapshotDiffStoringItem: SnapshotDiffStoringItem?
    var snapshotDiffContent: String

    preMap.forEach { (packageName, snapshotItem) ->
      if (!isActive) return@runBlocking
      if (packageName !in currMap) {
        diffList.add(compareSnapshotItems(snapshotItem, null, trackPackageNames)!!)
      }
    }

    currMap.forEach { (packageName, packageInfo) ->
      if (!isActive) return@runBlocking
      if (packageName in preMap) {
        return@forEach
      }
      try {
        val newInfo = snapshotItemFactory.create(packageManager, packageInfo)
        diffList.add(compareSnapshotItems(null, newInfo, trackPackageNames)!!)
      } catch (e: Exception) {
        Timber.e(e)
      } finally {
        count++
        changeComparingProgress(count * 100 / size)
      }
    }

    preMap.forEach { (packageName, snapshotItem) ->
      if (!isActive) return@runBlocking
      val presentItem = currMap[packageName] ?: return@forEach
      try {
        snapshotDiffStoringItem = repository.getSnapshotDiff(snapshotItem.packageName)

        if (snapshotDiffStoringItem?.lastUpdatedTime != presentItem.lastUpdateTime) {
          getDiffItemByComparingDBWithLocal(packageManager, snapshotItem, presentItem, trackPackageNames)?.let { item ->
            diffList.add(item)

            snapshotDiffContent = item.toJson().orEmpty()
            repository.insertSnapshotDiff(
              SnapshotDiffStoringItem(
                packageName = presentItem.packageName,
                lastUpdatedTime = presentItem.lastUpdateTime,
                diffContent = snapshotDiffContent
              )
            )
          }
        } else {
          try {
            snapshotDiffStoringItem.diffContent.fromJson<SnapshotDiffItem>()?.let { item ->
              diffList.add(item)
            }
          } catch (e: IOException) {
            Timber.e(e, "diffContent parsing failed")

            getDiffItemByComparingDBWithLocal(
              packageManager,
              snapshotItem,
              presentItem,
              trackPackageNames
            )?.let { item ->
              diffList.add(item)

              snapshotDiffContent = item.toJson().orEmpty()
              repository.insertSnapshotDiff(
                SnapshotDiffStoringItem(
                  packageName = presentItem.packageName,
                  lastUpdatedTime = presentItem.lastUpdateTime,
                  diffContent = snapshotDiffContent
                )
              )
            }
          }
        }
      } catch (e: Exception) {
        Timber.e(e)
      } finally {
        count++
        changeComparingProgress(count * 100 / size)
      }
    }

    snapshotDiffItemsFlow.emit(diffList)
    if (diffList.isNotEmpty()) {
      updateTopApps(preTimeStamp, diffList.subList(0, (diffList.size - 1).coerceAtMost(5)))
    }
  }

  private fun getDiffItemByComparingDBWithLocal(
    packageManager: PackageManager,
    dbItem: SnapshotItem,
    packageInfo: PackageInfo,
    trackPackageNames: Set<String>
  ): SnapshotDiffItem? {
    if (packageInfo.getVersionCode() == dbItem.versionCode &&
      packageInfo.lastUpdateTime == dbItem.lastUpdatedTime &&
      packageInfo.getPackageSize(true) == dbItem.packageSize &&
      dbItem.packageName !in trackPackageNames
    ) {
      return null
    }
    return compareSnapshotItems(dbItem, snapshotItemFactory.create(packageManager, packageInfo), trackPackageNames)
  }

  private fun compareDiffWithSnapshotList(preTimeStamp: Long, currTimeStamp: Long) = runBlocking {
    val preMap = repository.getSnapshots(preTimeStamp).associateBy { it.packageName }
    if (preMap.isEmpty()) {
      return@runBlocking
    }

    val currMap = repository.getSnapshots(currTimeStamp).associateBy { it.packageName }
    if (currMap.isEmpty()) {
      return@runBlocking
    }

    compareDiffWithSnapshotList(preTimeStamp, preMap.values.toList(), currMap.values.toList())
  }

  fun compareDiffWithSnapshotList(
    preTimeStamp: Long = -1L,
    preList: List<SnapshotItem>,
    currList: List<SnapshotItem>
  ) = runBlocking {
    if (preList.isEmpty()) {
      return@runBlocking
    }

    if (currList.isEmpty()) {
      return@runBlocking
    }

    val trackPackageNames = repository.getTrackItems()
      .asSequence()
      .map { it.packageName }
      .toSet()
    val diffList = compareSnapshotLists(preList, currList, trackPackageNames)

    snapshotDiffItemsFlow.emit(diffList)
    if (diffList.isNotEmpty() && preTimeStamp != -1L) {
      updateTopApps(preTimeStamp, diffList.subList(0, (diffList.size - 1).coerceAtMost(5)))
    }
  }

  suspend fun compareItemDiff(
    packageManager: PackageManager,
    timeStamp: Long = GlobalValues.snapshotTimestamp,
    packageName: String
  ) {
    val presentInfo = runCatching {
      val flags = PackageManager.GET_META_DATA or PackageManager.GET_PERMISSIONS
      PackageUtils.getPackageInfo(packageName, flags)
    }.getOrNull()?.let { snapshotItemFactory.create(packageManager, it) }
    val snapshotInfo = repository.getSnapshot(timeStamp, packageName)
    val trackPackageNames = repository.getTrackItems()
      .asSequence()
      .map { it.packageName }
      .toSet()
    val diffItem = compareSnapshotItems(snapshotInfo, presentInfo, trackPackageNames)

    diffItem?.let {
      changeDiffItem(it)
    } ?: run {
      removeDiffItem(packageName)
    }
  }

  private suspend fun updateTopApps(timestamp: Long, list: List<SnapshotDiffItem>) {
    val systemProps = repository.getTimeStamp(timestamp)?.systemProps
    val appsList = list.asSequence()
      .map { it.packageName }
      .filter { PackageUtils.isAppInstalled(it) }
      .toList()
    repository.updateTimeStamp(TimeStampItem(timestamp, appsList.toJson(), systemProps))
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
    val snapshotCount = repository.getSnapshots(timestamp).size
    val appCount = LocalAppDataSource.getApplicationCount()
    setEffect {
      Effect.DashboardCountChange(snapshotCount, appCount, isLeft)
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
