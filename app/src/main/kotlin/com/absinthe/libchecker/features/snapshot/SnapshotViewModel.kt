package com.absinthe.libchecker.features.snapshot

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.data.app.LocalAppDataSource
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.database.entity.SnapshotDiffStoringItem
import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.database.entity.TrackItem
import com.absinthe.libchecker.features.snapshot.detail.bean.ADDED
import com.absinthe.libchecker.features.snapshot.detail.bean.CHANGED
import com.absinthe.libchecker.features.snapshot.detail.bean.MOVED
import com.absinthe.libchecker.features.snapshot.detail.bean.REMOVED
import com.absinthe.libchecker.features.snapshot.detail.bean.SnapshotDetailItem
import com.absinthe.libchecker.features.snapshot.detail.bean.SnapshotDiffItem
import com.absinthe.libchecker.features.snapshot.ui.adapter.ARROW
import com.absinthe.libchecker.features.statistics.bean.LibStringItem
import com.absinthe.libchecker.protocol.Snapshot
import com.absinthe.libchecker.ui.base.BaseAlertDialogBuilder
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.getAppName
import com.absinthe.libchecker.utils.extensions.getCompileSdkVersion
import com.absinthe.libchecker.utils.extensions.getPackageSize
import com.absinthe.libchecker.utils.extensions.getPermissionsList
import com.absinthe.libchecker.utils.extensions.getVersionCode
import com.absinthe.libchecker.utils.extensions.sizeToString
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

class SnapshotViewModel : ViewModel() {

  val repository = Repositories.lcRepository
  val allSnapshots = repository.allSnapshotItemsFlow
  val snapshotDiffItemsFlow: MutableSharedFlow<List<SnapshotDiffItem>> = MutableSharedFlow()
  val snapshotDetailItemsFlow: MutableSharedFlow<List<SnapshotDetailItem>> = MutableSharedFlow()

  private val _effect: MutableSharedFlow<Effect> = MutableSharedFlow()
  val effect = _effect.asSharedFlow()

  private var compareDiffJob: Job? = null

  var currentTimeStamp: Long = GlobalValues.snapshotTimestamp
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

      if (shouldClearDiff) {
        repository.deleteAllSnapshotDiffItems()
      }

      if (currTimeStamp == CURRENT_SNAPSHOT) {
        compareDiffWithApplicationList(preTimeStamp)
      } else {
        compareDiffWithSnapshotList(preTimeStamp, currTimeStamp)
      }
      timer.end()
      Timber.d("compareDiff: $timer")
    }.also {
      it.start()
    }
  }

  private fun compareDiffWithApplicationList(preTimeStamp: Long) = runBlocking {
    val preMap = repository.getSnapshots(preTimeStamp).associateBy { it.packageName }

    if (preMap.isEmpty() || preTimeStamp == 0L) {
      snapshotDiffItemsFlow.emit(emptyList())
      return@runBlocking
    }

    val currMap = LocalAppDataSource.getApplicationMap().toMutableMap()
    val prePackageSet = preMap.map { it.key }.toSet()
    val currPackageSet = currMap.map { it.key }.toSet()
    val removedPackageSet = prePackageSet - currPackageSet
    val addedPackageSet = currPackageSet - prePackageSet
    val commonPackageSet = prePackageSet intersect currPackageSet

    val diffList = mutableListOf<SnapshotDiffItem>()
    val allTrackItems = repository.getTrackItems()
    val size = currMap.size

    var count = 0
    var snapshotDiffStoringItem: SnapshotDiffStoringItem?
    var snapshotDiffContent: String

    removedPackageSet.forEach {
      if (!isActive) return@runBlocking
      diffList.add(generateSnapshotDiffItem(preMap[it], null, allTrackItems)!!)
    }

    addedPackageSet.forEach {
      if (!isActive) return@runBlocking
      try {
        val newInfo = convertToSnapshotItem(currMap[it]!!)
        diffList.add(generateSnapshotDiffItem(null, newInfo, allTrackItems)!!)
      } catch (e: Exception) {
        Timber.e(e)
      } finally {
        count++
        changeComparingProgress(count * 100 / size)
      }
    }

    commonPackageSet.forEach {
      if (!isActive) return@runBlocking
      try {
        val snapshotItem = preMap[it]!!
        val presentItem = currMap[it]!!
        snapshotDiffStoringItem = repository.getSnapshotDiff(snapshotItem.packageName)

        if (snapshotDiffStoringItem?.lastUpdatedTime != presentItem.lastUpdateTime) {
          getDiffItemByComparingDBWithLocal(snapshotItem, presentItem, allTrackItems)?.let { item ->
            diffList.add(item)

            snapshotDiffContent = item.toJson().orEmpty()
            repository.insertSnapshotDiffItems(
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
              snapshotItem,
              presentItem,
              allTrackItems
            )?.let { item ->
              diffList.add(item)

              snapshotDiffContent = item.toJson().orEmpty()
              repository.insertSnapshotDiffItems(
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
    dbItem: SnapshotItem,
    packageInfo: PackageInfo,
    trackItems: List<TrackItem>
  ): SnapshotDiffItem? {
    if (packageInfo.getVersionCode() == dbItem.versionCode &&
      packageInfo.lastUpdateTime == dbItem.lastUpdatedTime &&
      packageInfo.getPackageSize(true) == dbItem.packageSize &&
      trackItems.any { trackItem -> trackItem.packageName == dbItem.packageName }.not()
    ) {
      return null
    }
    return generateSnapshotDiffItem(dbItem, convertToSnapshotItem(packageInfo), trackItems)
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

    compareDiffWithSnapshotList(preTimeStamp, preMap, currMap)
  }

  fun compareDiffWithSnapshotList(
    preTimeStamp: Long = -1L,
    preList: List<SnapshotItem>,
    currList: List<SnapshotItem>
  ) {
    val preMap = preList.associateBy { it.packageName }
    if (preMap.isEmpty()) {
      return
    }

    val currMap = currList.associateBy { it.packageName }
    if (currMap.isEmpty()) {
      return
    }

    compareDiffWithSnapshotList(preTimeStamp, preMap, currMap)
  }

  private fun compareDiffWithSnapshotList(
    preTimeStamp: Long = -1L,
    preMap: Map<String, SnapshotItem>,
    currMap: Map<String, SnapshotItem>
  ) = runBlocking {
    if (preMap.isEmpty()) {
      return@runBlocking
    }

    if (currMap.isEmpty()) {
      return@runBlocking
    }

    val prePackageSet = preMap.map { it.key }.toSet()
    val currPackageSet = currMap.map { it.key }.toSet()
    val removedPackageSet = prePackageSet - currPackageSet
    val addedPackageSet = currPackageSet - prePackageSet
    val commonPackageSet = prePackageSet intersect currPackageSet
    val diffList = mutableListOf<SnapshotDiffItem>()

    var preItem: SnapshotItem
    var currItem: SnapshotItem

    val allTrackItems = repository.getTrackItems()

    removedPackageSet.forEach {
      if (!isActive) return@runBlocking
      preItem = preMap[it]!!
      diffList.add(generateSnapshotDiffItem(preItem, null, allTrackItems)!!)
    }

    addedPackageSet.forEach {
      if (!isActive) return@runBlocking
      currItem = currMap[it]!!
      diffList.add(generateSnapshotDiffItem(null, currItem, allTrackItems)!!)
    }

    commonPackageSet.forEach {
      if (!isActive) return@runBlocking
      preItem = preMap[it]!!
      currItem = currMap[it]!!
      if (currItem.versionCode != preItem.versionCode || currItem.lastUpdatedTime != preItem.lastUpdatedTime) {
        diffList.add(generateSnapshotDiffItem(preItem, currItem, allTrackItems)!!)
      }
    }

    snapshotDiffItemsFlow.emit(diffList)
    if (diffList.isNotEmpty() && preTimeStamp != -1L) {
      updateTopApps(preTimeStamp, diffList.subList(0, (diffList.size - 1).coerceAtMost(5)))
    }
  }

  suspend fun compareItemDiff(
    timeStamp: Long = GlobalValues.snapshotTimestamp,
    packageName: String
  ) {
    val presentInfo = runCatching {
      val flags = PackageManager.GET_META_DATA or PackageManager.GET_PERMISSIONS
      PackageUtils.getPackageInfo(packageName, flags)
    }.getOrNull()?.let { convertToSnapshotItem(it) }
    val snapshotInfo = repository.getSnapshot(timeStamp, packageName)
    val allTrackItems = repository.getTrackItems()
    val diffItem = generateSnapshotDiffItem(snapshotInfo, presentInfo, allTrackItems)

    diffItem?.let {
      changeDiffItem(it)
    } ?: run {
      removeDiffItem(packageName)
    }
  }

  private fun convertToSnapshotItem(packageInfo: PackageInfo): SnapshotItem {
    val flaggedPi = PackageUtils.getPackageInfo(
      packageInfo.packageName,
      PackageManager.GET_SERVICES or PackageManager.GET_RECEIVERS or PackageManager.GET_PROVIDERS or PackageManager.GET_PERMISSIONS or PackageManager.GET_META_DATA
    )
    return SnapshotItem(
      id = null,
      packageName = packageInfo.packageName,
      timeStamp = 0,
      installedTime = packageInfo.firstInstallTime,
      lastUpdatedTime = packageInfo.lastUpdateTime,
      label = packageInfo.getAppName().toString(),
      versionName = packageInfo.versionName.toString(),
      versionCode = packageInfo.getVersionCode(),
      abi = PackageUtils.getAbi(packageInfo).toShort(),
      targetApi = packageInfo.applicationInfo?.targetSdkVersion?.toShort() ?: 0,
      compileSdk = packageInfo.getCompileSdkVersion().toShort(),
      minSdk = packageInfo.applicationInfo?.minSdkVersion?.toShort() ?: 0,
      nativeLibs = PackageUtils.getNativeDirLibs(packageInfo).toJson().orEmpty(),
      services = PackageUtils.getComponentStringList(
        flaggedPi,
        SERVICE,
        false
      ).toJson().orEmpty(),
      activities = PackageUtils.getComponentStringList(
        packageInfo.packageName,
        ACTIVITY,
        false
      ).toJson().orEmpty(),
      receivers = PackageUtils.getComponentStringList(
        flaggedPi,
        RECEIVER,
        false
      ).toJson().orEmpty(),
      providers = PackageUtils.getComponentStringList(
        flaggedPi,
        PROVIDER,
        false
      ).toJson().orEmpty(),
      permissions = flaggedPi.getPermissionsList().toJson().orEmpty(),
      metadata = PackageUtils.getMetaDataItems(flaggedPi).toJson().orEmpty(),
      packageSize = packageInfo.getPackageSize(true),
      isSystem = (packageInfo.applicationInfo!!.flags and ApplicationInfo.FLAG_SYSTEM) > 0
    )
  }

  private fun generateSnapshotDiffItem(
    oldInfo: SnapshotItem?,
    newInfo: SnapshotItem?,
    trackItems: List<TrackItem>
  ): SnapshotDiffItem? {
    if (oldInfo == null && newInfo == null) {
      return null
    } else if (newInfo == null || oldInfo == null) {
      val targetInfo = newInfo ?: oldInfo!!
      val newInstalled = newInfo != null
      return SnapshotDiffItem(
        targetInfo.packageName,
        targetInfo.lastUpdatedTime,
        SnapshotDiffItem.DiffNode(targetInfo.label),
        SnapshotDiffItem.DiffNode(targetInfo.versionName),
        SnapshotDiffItem.DiffNode(targetInfo.versionCode),
        SnapshotDiffItem.DiffNode(targetInfo.abi),
        SnapshotDiffItem.DiffNode(targetInfo.targetApi),
        SnapshotDiffItem.DiffNode(targetInfo.compileSdk),
        SnapshotDiffItem.DiffNode(targetInfo.minSdk),
        SnapshotDiffItem.DiffNode(targetInfo.nativeLibs),
        SnapshotDiffItem.DiffNode(targetInfo.services),
        SnapshotDiffItem.DiffNode(targetInfo.activities),
        SnapshotDiffItem.DiffNode(targetInfo.receivers),
        SnapshotDiffItem.DiffNode(targetInfo.providers),
        SnapshotDiffItem.DiffNode(targetInfo.permissions),
        SnapshotDiffItem.DiffNode(targetInfo.metadata),
        SnapshotDiffItem.DiffNode(targetInfo.packageSize),
        newInstalled = newInstalled,
        deleted = !newInstalled,
        isTrackItem = trackItems.any { trackItem -> trackItem.packageName == targetInfo.packageName }
      )
    } else {
      return SnapshotDiffItem(
        packageName = newInfo.packageName,
        updateTime = newInfo.lastUpdatedTime,
        labelDiff = SnapshotDiffItem.DiffNode(oldInfo.label, newInfo.label),
        versionNameDiff = SnapshotDiffItem.DiffNode(oldInfo.versionName, newInfo.versionName),
        versionCodeDiff = SnapshotDiffItem.DiffNode(oldInfo.versionCode, newInfo.versionCode),
        abiDiff = SnapshotDiffItem.DiffNode(oldInfo.abi, newInfo.abi),
        targetApiDiff = SnapshotDiffItem.DiffNode(oldInfo.targetApi, newInfo.targetApi),
        compileSdkDiff = SnapshotDiffItem.DiffNode(oldInfo.compileSdk, newInfo.compileSdk),
        minSdkDiff = SnapshotDiffItem.DiffNode(oldInfo.minSdk, newInfo.minSdk),
        nativeLibsDiff = SnapshotDiffItem.DiffNode(oldInfo.nativeLibs, newInfo.nativeLibs),
        servicesDiff = SnapshotDiffItem.DiffNode(oldInfo.services, newInfo.services),
        activitiesDiff = SnapshotDiffItem.DiffNode(oldInfo.activities, newInfo.activities),
        receiversDiff = SnapshotDiffItem.DiffNode(oldInfo.receivers, newInfo.receivers),
        providersDiff = SnapshotDiffItem.DiffNode(oldInfo.providers, newInfo.providers),
        permissionsDiff = SnapshotDiffItem.DiffNode(oldInfo.permissions, newInfo.permissions),
        metadataDiff = SnapshotDiffItem.DiffNode(oldInfo.metadata, newInfo.metadata),
        packageSizeDiff = SnapshotDiffItem.DiffNode(oldInfo.packageSize, newInfo.packageSize),
        isTrackItem = trackItems.any { trackItem -> trackItem.packageName == newInfo.packageName }
      ).apply {
        val diffIndicator = compareDiffIndicator(this)
        added = diffIndicator.added
        removed = diffIndicator.removed
        changed = diffIndicator.changed
        moved = diffIndicator.moved
      }
    }
  }

  private suspend fun updateTopApps(timestamp: Long, list: List<SnapshotDiffItem>) {
    val systemProps = repository.getTimeStamp(timestamp)?.systemProps
    val appsList = list.asSequence()
      .map { it.packageName }
      .filter { PackageUtils.isAppInstalled(it) }
      .toList()
    repository.updateTimeStampItem(TimeStampItem(timestamp, appsList.toJson(), systemProps))
  }

  fun computeDiffDetail(context: Context, entity: SnapshotDiffItem) = viewModelScope.launch(Dispatchers.IO) {
    val list = mutableListOf<SnapshotDetailItem>()

    list.addAll(
      getNativeDiffList(
        context,
        entity.nativeLibsDiff.old.fromJson<List<LibStringItem>>(
          List::class.java,
          LibStringItem::class.java
        ) ?: emptyList(),
        entity.nativeLibsDiff.new?.fromJson<List<LibStringItem>>(
          List::class.java,
          LibStringItem::class.java
        )
      )
    )
    addComponentDiffInfoFromJson(list, entity.servicesDiff, SERVICE)
    addComponentDiffInfoFromJson(list, entity.activitiesDiff, ACTIVITY)
    addComponentDiffInfoFromJson(list, entity.receiversDiff, RECEIVER)
    addComponentDiffInfoFromJson(list, entity.providersDiff, PROVIDER)

    list.addAll(
      getPermissionsDiffList(
        entity.permissionsDiff.old.fromJson<List<String>>(
          List::class.java,
          String::class.java
        ).orEmpty().toSet(),
        entity.permissionsDiff.new?.fromJson<List<String>>(
          List::class.java,
          String::class.java
        )?.toSet()
      )
    )

    list.addAll(
      getMetadataDiffList(
        entity.metadataDiff.old.fromJson<List<LibStringItem>>(
          List::class.java,
          LibStringItem::class.java
        ) ?: emptyList(),
        entity.metadataDiff.new?.fromJson<List<LibStringItem>>(
          List::class.java,
          LibStringItem::class.java
        )
      )
    )

    snapshotDetailItemsFlow.emit(list)
  }

  private fun addComponentDiffInfoFromJson(
    list: MutableList<SnapshotDetailItem>,
    diffNode: SnapshotDiffItem.DiffNode<String>,
    @LibType libType: Int
  ) {
    val old =
      diffNode.old.fromJson<List<String>>(List::class.java, String::class.java).orEmpty().toSet()
    val new =
      diffNode.new?.fromJson<List<String>>(List::class.java, String::class.java)?.toSet()
    list.addAll(getComponentsDiffList(old, new, libType))
  }

  private fun insertTimeStamp(timestamp: Long) = viewModelScope.launch(Dispatchers.IO) {
    if (timestamp == 0L) {
      return@launch
    }
    repository.insert(TimeStampItem(timestamp, null, null))
  }

  private fun getNativeDiffList(
    context: Context,
    oldList: List<LibStringItem>,
    newList: List<LibStringItem>?
  ): List<SnapshotDetailItem> {
    val list = mutableListOf<SnapshotDetailItem>()
    if (newList == null) {
      return list
    }

    val tempOldList = oldList.toMutableList()
    val tempNewList = newList.toMutableList()
    val intersectList = mutableListOf<LibStringItem>()

    for (item in tempNewList) {
      oldList.find { it.name == item.name }?.let {
        if (it.size != item.size) {
          val extra =
            "${it.size.sizeToString(context)} $ARROW ${item.size.sizeToString(context)}"
          list.add(
            SnapshotDetailItem(
              it.name,
              it.name,
              extra,
              CHANGED,
              NATIVE
            )
          )
        }
        intersectList.add(item)
      }
    }

    for (item in intersectList) {
      tempOldList.remove(tempOldList.find { it.name == item.name })
      tempNewList.remove(tempNewList.find { it.name == item.name })
    }

    for (item in tempOldList) {
      list.add(
        SnapshotDetailItem(
          item.name,
          item.name,
          PackageUtils.sizeToString(context, item),
          REMOVED,
          NATIVE
        )
      )
    }
    for (item in tempNewList) {
      list.add(
        SnapshotDetailItem(
          item.name,
          item.name,
          PackageUtils.sizeToString(context, item),
          ADDED,
          NATIVE
        )
      )
    }

    return list
  }

  private fun getComponentsDiffList(
    oldSet: Set<String>,
    newSet: Set<String>?,
    @LibType type: Int
  ): List<SnapshotDetailItem> {
    val list = mutableListOf<SnapshotDetailItem>()

    if (newSet == null) {
      return emptyList()
    }

    val removeList = (oldSet - newSet).toMutableSet()
    val addList = (newSet - oldSet).toMutableSet()

    val pendingRemovedOldSet = mutableSetOf<String>()
    val pendingRemovedNewSet = mutableSetOf<String>()

    for (item in addList) {
      removeList.find { it.substringAfterLast(".") == item.substringAfterLast(".") }?.let {
        list.add(
          SnapshotDetailItem(item, String.format("%s\n$ARROW\n%s", it, item), "", MOVED, type)
        )
        pendingRemovedOldSet.add(it)
        pendingRemovedNewSet.add(item)
      }
    }
    removeList.removeAll(pendingRemovedOldSet)
    addList.removeAll(pendingRemovedNewSet)

    removeList.forEach {
      list.add(
        SnapshotDetailItem(it, it, "", REMOVED, type)
      )
    }
    addList.forEach {
      list.add(
        SnapshotDetailItem(it, it, "", ADDED, type)
      )
    }

    return list
  }

  private fun getPermissionsDiffList(
    oldSet: Set<String>,
    newSet: Set<String>?
  ): List<SnapshotDetailItem> {
    val list = mutableListOf<SnapshotDetailItem>()

    if (newSet == null) {
      return emptyList()
    }

    val removeList = oldSet - newSet
    val addList = newSet - oldSet

    removeList.forEach {
      list.add(
        SnapshotDetailItem(it, it, "", REMOVED, PERMISSION)
      )
    }
    addList.forEach {
      list.add(
        SnapshotDetailItem(it, it, "", ADDED, PERMISSION)
      )
    }

    return list
  }

  private fun getMetadataDiffList(
    oldList: List<LibStringItem>,
    newList: List<LibStringItem>?
  ): List<SnapshotDetailItem> {
    val list = mutableListOf<SnapshotDetailItem>()

    if (newList == null) {
      return list
    }

    val tempOldList = oldList.toMutableList()
    val tempNewList = newList.toMutableList()
    val intersectList = mutableListOf<LibStringItem>()

    for (item in tempNewList) {
      oldList.find { it.name == item.name }?.let {
        if (it.source != item.source) {
          val extra =
            "${it.source.orEmpty()} $ARROW ${item.source.orEmpty()}"
          list.add(
            SnapshotDetailItem(
              it.name,
              it.name,
              extra,
              CHANGED,
              METADATA
            )
          )
        }
        intersectList.add(item)
      }
    }

    for (item in intersectList) {
      tempOldList.remove(tempOldList.find { it.name == item.name })
      tempNewList.remove(tempNewList.find { it.name == item.name })
    }

    for (item in tempOldList) {
      list.add(
        SnapshotDetailItem(item.name, item.name, item.source.orEmpty(), REMOVED, METADATA)
      )
    }
    for (item in tempNewList) {
      list.add(
        SnapshotDetailItem(item.name, item.name, item.source.orEmpty(), ADDED, METADATA)
      )
    }

    return list
  }

  data class CompareDiffNode(
    var added: Boolean = false,
    var removed: Boolean = false,
    var changed: Boolean = false,
    var moved: Boolean = false
  )

  private fun compareDiffIndicator(item: SnapshotDiffItem): CompareDiffNode {
    val native = compareNativeDiff(
      item.nativeLibsDiff.old.fromJson<List<LibStringItem>>(
        List::class.java,
        LibStringItem::class.java
      ) ?: emptyList(),
      item.nativeLibsDiff.new?.fromJson<List<LibStringItem>>(
        List::class.java,
        LibStringItem::class.java
      )
    )
    val services = compareComponentsDiff(item.servicesDiff)
    val activities = compareComponentsDiff(item.activitiesDiff)
    val receivers = compareComponentsDiff(item.receiversDiff)
    val providers = compareComponentsDiff(item.providersDiff)
    val permissions = comparePermissionsDiff(
      item.permissionsDiff.old.fromJson<List<String>>(
        List::class.java,
        String::class.java
      ).orEmpty().toSet(),
      item.permissionsDiff.new?.fromJson<List<String>>(
        List::class.java,
        String::class.java
      )?.toSet()
    )
    val metadata = compareMetadataDiff(
      item.metadataDiff.old.fromJson<List<LibStringItem>>(
        List::class.java,
        LibStringItem::class.java
      ) ?: emptyList(),
      item.metadataDiff.new?.fromJson<List<LibStringItem>>(
        List::class.java,
        LibStringItem::class.java
      )
    )

    val totalNode = CompareDiffNode().apply {
      added =
        native.added or services.added or activities.added or receivers.added or providers.added or permissions.added or metadata.added
      removed =
        native.removed or services.removed or activities.removed or receivers.removed or providers.removed or permissions.removed or metadata.removed
      changed =
        native.changed or metadata.changed
      moved =
        services.moved or activities.moved or receivers.moved or providers.moved
    }

    return totalNode
  }

  private fun compareNativeDiff(
    oldList: List<LibStringItem>,
    newList: List<LibStringItem>?
  ): CompareDiffNode {
    if (newList == null) {
      return CompareDiffNode(removed = true)
    }

    val tempOldList = oldList.toMutableList()
    val tempNewList = newList.toMutableList()
    val node = CompareDiffNode()

    val iterator = tempNewList.iterator()
    var nextItem: LibStringItem

    while (iterator.hasNext()) {
      nextItem = iterator.next()
      oldList.find { it.name == nextItem.name }?.let {
        if (it.size != nextItem.size) {
          node.changed = true
        }
        iterator.remove()
        tempOldList.remove(tempOldList.find { item -> item.name == nextItem.name })
      }
    }

    if (tempOldList.isNotEmpty()) {
      node.removed = true
    }
    if (tempNewList.isNotEmpty()) {
      node.added = true
    }
    return node
  }

  private fun compareComponentsDiff(diffNode: SnapshotDiffItem.DiffNode<String>): CompareDiffNode {
    if (diffNode.new == null) {
      return CompareDiffNode(removed = true)
    }

    val oldSet = diffNode.old.fromJson<List<String>>(
      List::class.java,
      String::class.java
    ).orEmpty().toSet()
    val newSet = diffNode.new.fromJson<List<String>>(
      List::class.java,
      String::class.java
    ).orEmpty().toSet()

    val removeList = (oldSet - newSet).toMutableSet()
    val addList = (newSet - oldSet).toMutableSet()
    val node = CompareDiffNode()
    val pendingRemovedOldSet = mutableSetOf<String>()
    val pendingRemovedNewSet = mutableSetOf<String>()

    for (item in addList) {
      removeList.find { it.substringAfterLast(".") == item.substringAfterLast(".") }?.let {
        node.moved = true
        pendingRemovedOldSet += it
        pendingRemovedNewSet += item
      }
    }
    removeList.removeAll(pendingRemovedOldSet)
    addList.removeAll(pendingRemovedNewSet)

    if (removeList.isNotEmpty()) {
      node.removed = true
    }
    if (addList.isNotEmpty()) {
      node.added = true
    }
    return node
  }

  private fun comparePermissionsDiff(
    oldSet: Set<String>,
    newSet: Set<String>?
  ): CompareDiffNode {
    if (newSet == null) {
      return CompareDiffNode(removed = true)
    }

    val removeList = oldSet - newSet
    val addList = newSet - oldSet
    val node = CompareDiffNode()

    if (removeList.isNotEmpty()) {
      node.removed = true
    }
    if (addList.isNotEmpty()) {
      node.added = true
    }
    return node
  }

  private fun compareMetadataDiff(
    oldList: List<LibStringItem>,
    newList: List<LibStringItem>?
  ): CompareDiffNode {
    if (newList == null) {
      return CompareDiffNode(removed = true)
    }

    val tempOldList = oldList.toMutableList()
    val tempNewList = newList.toMutableList()
    val node = CompareDiffNode()

    val iterator = tempNewList.iterator()
    var nextItem: LibStringItem

    while (iterator.hasNext()) {
      nextItem = iterator.next()
      oldList.find { it.name == nextItem.name }?.let {
        if (it.source != nextItem.source) {
          node.changed = true
        }
        iterator.remove()
        tempOldList.remove(tempOldList.find { item -> item.name == nextItem.name })
      }
    }

    if (tempOldList.isNotEmpty()) {
      node.removed = true
    }
    if (tempNewList.isNotEmpty()) {
      node.added = true
    }
    return node
  }

  fun backup(os: OutputStream, resultAction: () -> Unit) = viewModelScope.launch(Dispatchers.IO) {
    val backupList = repository.getSnapshots()
    val snapshotBuilder: Snapshot.Builder = Snapshot.newBuilder()

    os.use {
      backupList.forEach {
        snapshotBuilder.apply {
          packageName = it.packageName
          timeStamp = it.timeStamp
          label = it.label
          versionName = it.versionName
          versionCode = it.versionCode
          installedTime = it.installedTime
          lastUpdatedTime = it.lastUpdatedTime
          isSystem = it.isSystem
          abi = it.abi.toInt()
          targetApi = it.targetApi.toInt()
          nativeLibs = it.nativeLibs
          services = it.services
          activities = it.activities
          receivers = it.receivers
          providers = it.providers
          permissions = it.permissions
          metadata = it.metadata
          packageSize = it.packageSize
          compileSdk = it.compileSdk.toInt()
          minSdk = it.minSdk.toInt()
        }

        snapshotBuilder.build().writeDelimitedTo(os)
      }
    }

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
      inputStream.use { stream ->
        val list = mutableListOf<Snapshot>()
        val timeStampMap = mutableMapOf<Long, Int>()

        runCatching {
          while (true) {
            val snapshot = Snapshot.parseDelimitedFrom(stream) ?: break
            list.add(snapshot)
            timeStampMap[snapshot.timeStamp] = timeStampMap.getOrDefault(snapshot.timeStamp, 0) + 1
            if (list.size == 200) {
              restoreImpl(list)
              list.clear()
            }
          }
          restoreImpl(list)
          list.clear()
        }.onFailure {
          Timber.e("restore with new format failed: $it")
          withContext(Dispatchers.Main) {
            resultAction(false)
          }
          return@launch
          // runCatching {
          //   val list: SnapshotList = SnapshotList.parseFrom(stream)
          //   Timber.d("restore with old format: ${list.snapshotsList.size}")
          //   list.snapshotsList.forEach {
          //     timeStampMap[it.timeStamp] = timeStampMap.getOrDefault(it.timeStamp, 0) + 1
          //   }
          //   restoreImpl(list.snapshotsList)
          // }.onFailure {
          //   Timber.e("restore with old format failed: $it")
          //   withContext(Dispatchers.Main) {
          //     resultAction(false)
          //   }
          //   return@launch
          // }
        }

        repository.deleteDuplicateSnapshotItems()
        timeStampMap.forEach { insertTimeStamp(it.key) }
        timeStampMap.keys.maxOrNull()?.let { GlobalValues.snapshotTimestamp = it }
        withContext(Dispatchers.Main) {
          resultAction(true)
        }

        val message = buildString {
          timeStampMap.forEach {
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

  private suspend fun restoreImpl(list: List<Snapshot>) {
    list.map {
      SnapshotItem(
        null,
        it.packageName,
        it.timeStamp,
        it.label,
        it.versionName,
        it.versionCode,
        it.installedTime,
        it.lastUpdatedTime,
        it.isSystem,
        it.abi.toShort(),
        it.targetApi.toShort(),
        it.nativeLibs,
        it.services,
        it.activities,
        it.receivers,
        it.providers,
        it.permissions,
        it.metadata,
        it.packageSize,
        it.compileSdk.toShort(),
        it.minSdk.toShort()
      )
    }.let {
      repository.insertSnapshots(it)
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
    val appCount = LocalAppDataSource.getApplicationMap().size
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
