package com.absinthe.libchecker.viewmodel

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
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
import com.absinthe.libchecker.app.SystemServices
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.data.app.LocalAppDataSource
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.database.entity.SnapshotDiffStoringItem
import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.model.ADDED
import com.absinthe.libchecker.model.CHANGED
import com.absinthe.libchecker.model.LibStringItem
import com.absinthe.libchecker.model.MOVED
import com.absinthe.libchecker.model.REMOVED
import com.absinthe.libchecker.model.SnapshotDetailItem
import com.absinthe.libchecker.model.SnapshotDiffItem
import com.absinthe.libchecker.protocol.Snapshot
import com.absinthe.libchecker.protocol.SnapshotList
import com.absinthe.libchecker.recyclerview.adapter.snapshot.ARROW
import com.absinthe.libchecker.ui.base.BaseAlertDialogBuilder
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.PackageUtils.getPermissionsList
import com.absinthe.libchecker.utils.extensions.sizeToString
import com.absinthe.libchecker.utils.fromJson
import com.absinthe.libchecker.utils.toJson
import com.absinthe.libraries.utils.manager.TimeRecorder
import com.google.protobuf.InvalidProtocolBufferException
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.buffer
import okio.sink
import timber.log.Timber

const val CURRENT_SNAPSHOT = -1L

class SnapshotViewModel(application: Application) : AndroidViewModel(application) {

  val repository = Repositories.lcRepository
  val allSnapshots = repository.allSnapshotItemsFlow
  val timestamp: MutableLiveData<Long> = MutableLiveData(GlobalValues.snapshotTimestamp)
  val snapshotDiffItems: MutableLiveData<List<SnapshotDiffItem>> = MutableLiveData()
  val snapshotDetailItems: MutableLiveData<List<SnapshotDetailItem>> = MutableLiveData()
  val comparingProgressLiveData = MutableLiveData(0)

  private val _effect: MutableSharedFlow<Effect> = MutableSharedFlow()
  val effect = _effect.asSharedFlow()

  private var compareDiffJob: Job? = null

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

  private suspend fun compareDiffWithApplicationList(preTimeStamp: Long) {
    val preMap = repository.getSnapshots(preTimeStamp).associateBy { it.packageName }

    if (preMap.isEmpty() || preTimeStamp == 0L) {
      snapshotDiffItems.postValue(emptyList())
      return
    }

    val currMap = LocalAppDataSource.getCachedApplicationMap().toMutableMap()
    val prePackageSet = preMap.map { it.key }.toSet()
    val currPackageSet = currMap.map { it.key }.toSet()
    val removedPackageSet = prePackageSet - currPackageSet
    val addedPackageSet = currPackageSet - prePackageSet
    val commonPackageSet = prePackageSet intersect currPackageSet

    val diffList = mutableListOf<SnapshotDiffItem>()
    val allTrackItems = repository.getTrackItems()
    val packageManager = SystemServices.packageManager
    val size = currMap.size

    var count = 0
    var dbItem: SnapshotItem
    var pi: PackageInfo
    var ai: ApplicationInfo
    var versionCode: Long
    var snapshotDiffStoringItem: SnapshotDiffStoringItem?
    var snapshotDiffContent: String

    removedPackageSet.forEach {
      dbItem = preMap[it]!!
      diffList.add(
        SnapshotDiffItem(
          dbItem.packageName,
          dbItem.lastUpdatedTime,
          SnapshotDiffItem.DiffNode(dbItem.label),
          SnapshotDiffItem.DiffNode(dbItem.versionName),
          SnapshotDiffItem.DiffNode(dbItem.versionCode),
          SnapshotDiffItem.DiffNode(dbItem.abi),
          SnapshotDiffItem.DiffNode(dbItem.targetApi),
          SnapshotDiffItem.DiffNode(dbItem.nativeLibs),
          SnapshotDiffItem.DiffNode(dbItem.services),
          SnapshotDiffItem.DiffNode(dbItem.activities),
          SnapshotDiffItem.DiffNode(dbItem.receivers),
          SnapshotDiffItem.DiffNode(dbItem.providers),
          SnapshotDiffItem.DiffNode(dbItem.permissions),
          SnapshotDiffItem.DiffNode(dbItem.metadata),
          SnapshotDiffItem.DiffNode(dbItem.packageSize),
          deleted = true,
          isTrackItem = allTrackItems.any { trackItem -> trackItem.packageName == dbItem.packageName }
        )
      )
    }

    addedPackageSet.forEach {
      try {
        pi = currMap[it]!!
        ai = pi.applicationInfo
        versionCode = PackageUtils.getVersionCode(pi)

        diffList.add(
          SnapshotDiffItem(
            pi.packageName,
            pi.lastUpdateTime,
            SnapshotDiffItem.DiffNode(ai.loadLabel(packageManager).toString()),
            SnapshotDiffItem.DiffNode(pi.versionName),
            SnapshotDiffItem.DiffNode(versionCode),
            SnapshotDiffItem.DiffNode(PackageUtils.getAbi(pi).toShort()),
            SnapshotDiffItem.DiffNode(ai.targetSdkVersion.toShort()),
            SnapshotDiffItem.DiffNode(
              PackageUtils.getNativeDirLibs(pi).toJson().orEmpty()
            ),
            SnapshotDiffItem.DiffNode(
              PackageUtils.getComponentStringList(
                pi.packageName,
                SERVICE,
                false
              ).toJson().orEmpty()
            ),
            SnapshotDiffItem.DiffNode(
              PackageUtils.getComponentStringList(
                pi.packageName,
                ACTIVITY,
                false
              ).toJson().orEmpty()
            ),
            SnapshotDiffItem.DiffNode(
              PackageUtils.getComponentStringList(
                pi.packageName,
                RECEIVER,
                false
              ).toJson().orEmpty()
            ),
            SnapshotDiffItem.DiffNode(
              PackageUtils.getComponentStringList(
                pi.packageName,
                PROVIDER,
                false
              ).toJson().orEmpty()
            ),
            SnapshotDiffItem.DiffNode(
              pi.getPermissionsList().toJson().orEmpty()
            ),
            SnapshotDiffItem.DiffNode(
              PackageUtils.getMetaDataItems(pi).toJson().orEmpty()
            ),
            SnapshotDiffItem.DiffNode(
              PackageUtils.getPackageSize(pi, true)
            ),
            newInstalled = true,
            isTrackItem = allTrackItems.any { trackItem -> trackItem.packageName == pi.packageName }
          )
        )
      } catch (e: Exception) {
        Timber.e(e)
      } finally {
        count++
        comparingProgressLiveData.postValue(count * 100 / size)
      }
    }

    commonPackageSet.forEach {
      try {
        dbItem = preMap[it]!!
        pi = currMap[it]!!
        versionCode = PackageUtils.getVersionCode(pi)
        snapshotDiffStoringItem = repository.getSnapshotDiff(dbItem.packageName)

        if (snapshotDiffStoringItem?.lastUpdatedTime != pi.lastUpdateTime) {
          val inTrackedList =
            allTrackItems.any { trackItem -> trackItem.packageName == dbItem.packageName }
          getDiffItemByComparingDBWithLocal(dbItem, pi, versionCode, inTrackedList)?.let { item ->
            diffList.add(item)

            snapshotDiffContent = item.toJson().orEmpty()
            repository.insertSnapshotDiffItems(
              SnapshotDiffStoringItem(
                packageName = pi.packageName,
                lastUpdatedTime = pi.lastUpdateTime,
                diffContent = snapshotDiffContent
              )
            )
          }
        } else {
          try {
            snapshotDiffStoringItem?.diffContent?.fromJson<SnapshotDiffItem>()?.let { item ->
              diffList.add(item)
            }
          } catch (e: IOException) {
            Timber.e(e, "diffContent parsing failed")

            val inTrackedList =
              allTrackItems.any { trackItem -> trackItem.packageName == dbItem.packageName }
            getDiffItemByComparingDBWithLocal(dbItem, pi, versionCode, inTrackedList)?.let { item ->
              diffList.add(item)

              snapshotDiffContent = item.toJson().orEmpty()
              repository.insertSnapshotDiffItems(
                SnapshotDiffStoringItem(
                  packageName = pi.packageName,
                  lastUpdatedTime = pi.lastUpdateTime,
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
        comparingProgressLiveData.postValue(count * 100 / size)
      }
    }

    snapshotDiffItems.postValue(diffList)
    if (diffList.isNotEmpty()) {
      updateTopApps(preTimeStamp, diffList.subList(0, (diffList.size - 1).coerceAtMost(5)))
    }
  }

  private fun getDiffItemByComparingDBWithLocal(
    dbItem: SnapshotItem,
    packageInfo: PackageInfo,
    versionCode: Long,
    inTrackedList: Boolean
  ): SnapshotDiffItem? {
    if (versionCode == dbItem.versionCode &&
      packageInfo.lastUpdateTime == dbItem.lastUpdatedTime &&
      PackageUtils.getPackageSize(packageInfo, true) == dbItem.packageSize &&
      inTrackedList.not()
    ) {
      return null
    }
    val sdi = SnapshotDiffItem(
      packageName = packageInfo.packageName,
      updateTime = packageInfo.lastUpdateTime,
      labelDiff = SnapshotDiffItem.DiffNode(
        dbItem.label,
        packageInfo.applicationInfo.loadLabel(SystemServices.packageManager).toString()
      ),
      versionNameDiff = SnapshotDiffItem.DiffNode(
        dbItem.versionName,
        packageInfo.versionName
      ),
      versionCodeDiff = SnapshotDiffItem.DiffNode(
        dbItem.versionCode,
        versionCode
      ),
      abiDiff = SnapshotDiffItem.DiffNode(
        dbItem.abi,
        PackageUtils.getAbi(packageInfo)
          .toShort()
      ),
      targetApiDiff = SnapshotDiffItem.DiffNode(
        dbItem.targetApi,
        packageInfo.applicationInfo.targetSdkVersion.toShort()
      ),
      nativeLibsDiff = SnapshotDiffItem.DiffNode(
        dbItem.nativeLibs,
        PackageUtils.getNativeDirLibs(packageInfo).toJson().orEmpty()
      ),
      servicesDiff = SnapshotDiffItem.DiffNode(
        dbItem.services,
        PackageUtils.getComponentStringList(
          packageInfo.packageName,
          SERVICE,
          false
        ).toJson().orEmpty()
      ),
      activitiesDiff = SnapshotDiffItem.DiffNode(
        dbItem.activities,
        PackageUtils.getComponentStringList(
          packageInfo.packageName,
          ACTIVITY,
          false
        ).toJson().orEmpty()
      ),
      receiversDiff = SnapshotDiffItem.DiffNode(
        dbItem.receivers,
        PackageUtils.getComponentStringList(
          packageInfo.packageName,
          RECEIVER,
          false
        ).toJson().orEmpty()
      ),
      providersDiff = SnapshotDiffItem.DiffNode(
        dbItem.providers,
        PackageUtils.getComponentStringList(
          packageInfo.packageName,
          PROVIDER,
          false
        ).toJson().orEmpty()
      ),
      permissionsDiff = SnapshotDiffItem.DiffNode(
        dbItem.permissions,
        packageInfo.getPermissionsList().toJson().orEmpty()
      ),
      metadataDiff = SnapshotDiffItem.DiffNode(
        dbItem.metadata,
        PackageUtils.getMetaDataItems(packageInfo).toJson().orEmpty()
      ),
      packageSizeDiff = SnapshotDiffItem.DiffNode(
        dbItem.packageSize,
        PackageUtils.getPackageSize(packageInfo, true)
      ),
      isTrackItem = inTrackedList
    )
    val diffIndicator = compareDiffIndicator(sdi)
    sdi.apply {
      added = diffIndicator.added
      removed = diffIndicator.removed
      changed = diffIndicator.changed
      moved = diffIndicator.moved
    }

    return sdi
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

    compareDiffWithSnapshotList(preTimeStamp, preMap, currMap)
  }

  suspend fun compareDiffWithSnapshotList(
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

  private suspend fun compareDiffWithSnapshotList(
    preTimeStamp: Long = -1L,
    preMap: Map<String, SnapshotItem>,
    currMap: Map<String, SnapshotItem>
  ) {
    if (preMap.isEmpty()) {
      return
    }

    if (currMap.isEmpty()) {
      return
    }

    val prePackageSet = preMap.map { it.key }.toSet()
    val currPackageSet = currMap.map { it.key }.toSet()
    val removedPackageSet = prePackageSet - currPackageSet
    val addedPackageSet = currPackageSet - prePackageSet
    val commonPackageSet = prePackageSet intersect currPackageSet
    val diffList = mutableListOf<SnapshotDiffItem>()

    var compareDiffNode: CompareDiffNode
    var snapshotDiffItem: SnapshotDiffItem
    var preItem: SnapshotItem
    var currItem: SnapshotItem

    val allTrackItems = repository.getTrackItems()

    removedPackageSet.forEach {
      preItem = preMap[it]!!
      diffList.add(
        SnapshotDiffItem(
          preItem.packageName,
          preItem.lastUpdatedTime,
          SnapshotDiffItem.DiffNode(preItem.label),
          SnapshotDiffItem.DiffNode(preItem.versionName),
          SnapshotDiffItem.DiffNode(preItem.versionCode),
          SnapshotDiffItem.DiffNode(preItem.abi),
          SnapshotDiffItem.DiffNode(preItem.targetApi),
          SnapshotDiffItem.DiffNode(preItem.nativeLibs),
          SnapshotDiffItem.DiffNode(preItem.services),
          SnapshotDiffItem.DiffNode(preItem.activities),
          SnapshotDiffItem.DiffNode(preItem.receivers),
          SnapshotDiffItem.DiffNode(preItem.providers),
          SnapshotDiffItem.DiffNode(preItem.permissions),
          SnapshotDiffItem.DiffNode(preItem.metadata),
          SnapshotDiffItem.DiffNode(preItem.packageSize),
          deleted = true,
          isTrackItem = allTrackItems.any { trackItem -> trackItem.packageName == preItem.packageName }
        )
      )
    }

    addedPackageSet.forEach {
      currItem = currMap[it]!!
      diffList.add(
        SnapshotDiffItem(
          currItem.packageName,
          currItem.lastUpdatedTime,
          SnapshotDiffItem.DiffNode(currItem.label),
          SnapshotDiffItem.DiffNode(currItem.versionName),
          SnapshotDiffItem.DiffNode(currItem.versionCode),
          SnapshotDiffItem.DiffNode(currItem.abi),
          SnapshotDiffItem.DiffNode(currItem.targetApi),
          SnapshotDiffItem.DiffNode(currItem.nativeLibs),
          SnapshotDiffItem.DiffNode(currItem.services),
          SnapshotDiffItem.DiffNode(currItem.activities),
          SnapshotDiffItem.DiffNode(currItem.receivers),
          SnapshotDiffItem.DiffNode(currItem.providers),
          SnapshotDiffItem.DiffNode(currItem.permissions),
          SnapshotDiffItem.DiffNode(currItem.metadata),
          SnapshotDiffItem.DiffNode(currItem.packageSize),
          newInstalled = true,
          isTrackItem = allTrackItems.any { trackItem -> trackItem.packageName == currItem.packageName }
        )
      )
    }

    commonPackageSet.forEach {
      preItem = preMap[it]!!
      currItem = currMap[it]!!
      if (currItem.versionCode != preItem.versionCode || currItem.lastUpdatedTime != preItem.lastUpdatedTime) {
        snapshotDiffItem = SnapshotDiffItem(
          packageName = currItem.packageName,
          updateTime = currItem.lastUpdatedTime,
          labelDiff = SnapshotDiffItem.DiffNode(preItem.label, currItem.label),
          versionNameDiff = SnapshotDiffItem.DiffNode(
            preItem.versionName,
            currItem.versionName
          ),
          versionCodeDiff = SnapshotDiffItem.DiffNode(
            preItem.versionCode,
            currItem.versionCode
          ),
          abiDiff = SnapshotDiffItem.DiffNode(preItem.abi, currItem.abi),
          targetApiDiff = SnapshotDiffItem.DiffNode(preItem.targetApi, currItem.targetApi),
          nativeLibsDiff = SnapshotDiffItem.DiffNode(
            preItem.nativeLibs,
            currItem.nativeLibs
          ),
          servicesDiff = SnapshotDiffItem.DiffNode(preItem.services, currItem.services),
          activitiesDiff = SnapshotDiffItem.DiffNode(
            preItem.activities,
            currItem.activities
          ),
          receiversDiff = SnapshotDiffItem.DiffNode(preItem.receivers, currItem.receivers),
          providersDiff = SnapshotDiffItem.DiffNode(preItem.providers, currItem.providers),
          permissionsDiff = SnapshotDiffItem.DiffNode(
            preItem.permissions,
            currItem.permissions
          ),
          metadataDiff = SnapshotDiffItem.DiffNode(
            preItem.metadata,
            currItem.metadata
          ),
          packageSizeDiff = SnapshotDiffItem.DiffNode(
            preItem.packageSize,
            currItem.packageSize
          ),
          isTrackItem = allTrackItems.any { trackItem -> trackItem.packageName == currItem.packageName }
        )
        compareDiffNode = compareDiffIndicator(snapshotDiffItem)
        snapshotDiffItem.added = compareDiffNode.added
        snapshotDiffItem.removed = compareDiffNode.removed
        snapshotDiffItem.changed = compareDiffNode.changed
        snapshotDiffItem.moved = compareDiffNode.moved

        diffList.add(snapshotDiffItem)
      }
    }

    snapshotDiffItems.postValue(diffList)
    if (diffList.isNotEmpty() && preTimeStamp != -1L) {
      updateTopApps(preTimeStamp, diffList.subList(0, (diffList.size - 1).coerceAtMost(5)))
    }
  }

  suspend fun compareItemDiff(
    timeStamp: Long = GlobalValues.snapshotTimestamp,
    packageName: String
  ) {
    val info = runCatching {
      PackageUtils.getPackageInfo(
        packageName,
        PackageManager.GET_META_DATA or PackageManager.GET_PERMISSIONS
      )
    }.getOrNull()
    val allTrackItems = repository.getTrackItems()
    var diffItem: SnapshotDiffItem? = null

    repository.getSnapshot(timeStamp, packageName)?.let {
      info?.let { packageInfo ->
        diffItem = SnapshotDiffItem(
          packageInfo.packageName,
          packageInfo.lastUpdateTime,
          SnapshotDiffItem.DiffNode(
            it.label,
            packageInfo.applicationInfo.loadLabel(SystemServices.packageManager).toString()
          ),
          SnapshotDiffItem.DiffNode(it.versionName, packageInfo.versionName),
          SnapshotDiffItem.DiffNode(it.versionCode, PackageUtils.getVersionCode(packageInfo)),
          SnapshotDiffItem.DiffNode(
            it.abi,
            PackageUtils.getAbi(packageInfo).toShort()
          ),
          SnapshotDiffItem.DiffNode(
            it.targetApi,
            packageInfo.applicationInfo.targetSdkVersion.toShort()
          ),
          SnapshotDiffItem.DiffNode(
            it.nativeLibs,
            PackageUtils.getNativeDirLibs(packageInfo).toJson().orEmpty()
          ),
          SnapshotDiffItem.DiffNode(
            it.services,
            PackageUtils.getComponentStringList(
              packageInfo.packageName,
              SERVICE,
              false
            ).toJson().orEmpty()
          ),
          SnapshotDiffItem.DiffNode(
            it.activities,
            PackageUtils.getComponentStringList(
              packageInfo.packageName,
              ACTIVITY,
              false
            ).toJson().orEmpty()
          ),
          SnapshotDiffItem.DiffNode(
            it.receivers,
            PackageUtils.getComponentStringList(
              packageInfo.packageName,
              RECEIVER,
              false
            ).toJson().orEmpty()
          ),
          SnapshotDiffItem.DiffNode(
            it.providers,
            PackageUtils.getComponentStringList(
              packageInfo.packageName,
              PROVIDER,
              false
            ).toJson().orEmpty()
          ),
          SnapshotDiffItem.DiffNode(
            it.permissions,
            packageInfo.getPermissionsList().toJson().orEmpty()
          ),
          SnapshotDiffItem.DiffNode(
            it.metadata,
            PackageUtils.getMetaDataItems(packageInfo).toJson().orEmpty()
          ),
          SnapshotDiffItem.DiffNode(
            it.packageSize,
            PackageUtils.getPackageSize(packageInfo, true)
          ),
          isTrackItem = allTrackItems.any { trackItem -> trackItem.packageName == it.packageName }
        ).also { diffItem ->
          val compareDiffNode = compareDiffIndicator(diffItem)
          diffItem.added = compareDiffNode.added
          diffItem.removed = compareDiffNode.removed
          diffItem.changed = compareDiffNode.changed
          diffItem.moved = compareDiffNode.moved
        }
      } ?: run {
        diffItem = SnapshotDiffItem(
          it.packageName,
          it.lastUpdatedTime,
          SnapshotDiffItem.DiffNode(it.label),
          SnapshotDiffItem.DiffNode(it.versionName),
          SnapshotDiffItem.DiffNode(it.versionCode),
          SnapshotDiffItem.DiffNode(it.abi),
          SnapshotDiffItem.DiffNode(it.targetApi),
          SnapshotDiffItem.DiffNode(it.nativeLibs),
          SnapshotDiffItem.DiffNode(it.services),
          SnapshotDiffItem.DiffNode(it.activities),
          SnapshotDiffItem.DiffNode(it.receivers),
          SnapshotDiffItem.DiffNode(it.providers),
          SnapshotDiffItem.DiffNode(it.permissions),
          SnapshotDiffItem.DiffNode(it.metadata),
          SnapshotDiffItem.DiffNode(it.packageSize),
          deleted = true,
          isTrackItem = allTrackItems.any { trackItem -> trackItem.packageName == it.packageName }
        )
      }
    } ?: run {
      info?.let { packageInfo ->
        diffItem = SnapshotDiffItem(
          packageInfo.packageName,
          packageInfo.lastUpdateTime,
          SnapshotDiffItem.DiffNode(
            packageInfo.applicationInfo.loadLabel(SystemServices.packageManager).toString()
          ),
          SnapshotDiffItem.DiffNode(packageInfo.versionName),
          SnapshotDiffItem.DiffNode(PackageUtils.getVersionCode(packageInfo)),
          SnapshotDiffItem.DiffNode(PackageUtils.getAbi(packageInfo).toShort()),
          SnapshotDiffItem.DiffNode(packageInfo.applicationInfo.targetSdkVersion.toShort()),
          SnapshotDiffItem.DiffNode(
            PackageUtils.getNativeDirLibs(packageInfo).toJson().orEmpty()
          ),
          SnapshotDiffItem.DiffNode(
            PackageUtils.getComponentStringList(
              packageInfo.packageName,
              SERVICE,
              false
            ).toJson().orEmpty()
          ),
          SnapshotDiffItem.DiffNode(
            PackageUtils.getComponentStringList(
              packageInfo.packageName,
              ACTIVITY,
              false
            ).toJson().orEmpty()
          ),
          SnapshotDiffItem.DiffNode(
            PackageUtils.getComponentStringList(
              packageInfo.packageName,
              RECEIVER,
              false
            ).toJson().orEmpty()
          ),
          SnapshotDiffItem.DiffNode(
            PackageUtils.getComponentStringList(
              packageInfo.packageName,
              PROVIDER,
              false
            ).toJson().orEmpty()
          ),
          SnapshotDiffItem.DiffNode(
            packageInfo.getPermissionsList().toJson().orEmpty()
          ),
          SnapshotDiffItem.DiffNode(
            PackageUtils.getMetaDataItems(packageInfo).toJson().orEmpty()
          ),
          SnapshotDiffItem.DiffNode(
            PackageUtils.getPackageSize(packageInfo, true)
          ),
          newInstalled = true,
          isTrackItem = allTrackItems.any { trackItem -> trackItem.packageName == info.packageName }
        )
      }
    }

    diffItem?.let { diff ->
      val diffList = snapshotDiffItems.value?.toMutableList() ?: mutableListOf()
      diffList.removeAll { it.packageName == diff.packageName }
      diffList.add(diff)
      snapshotDiffItems.postValue(diffList)
    }
  }

  private suspend fun updateTopApps(timestamp: Long, list: List<SnapshotDiffItem>) {
    val appsList = list.asSequence()
      .map { it.packageName }
      .filter { PackageUtils.isAppInstalled(it) }
      .toList()
    repository.updateTimeStampItem(TimeStampItem(timestamp, appsList.toJson()))
  }

  fun computeDiffDetail(context: Context, entity: SnapshotDiffItem) =
    viewModelScope.launch(Dispatchers.IO) {
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

      snapshotDetailItems.postValue(list)
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
    repository.insert(TimeStampItem(timestamp, null))
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
          PackageUtils.sizeToString(context, item, false),
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
          PackageUtils.sizeToString(context, item, false),
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
    val builder: SnapshotList.Builder = SnapshotList.newBuilder()
    val backupList = repository.getSnapshots()

    val snapshotList = mutableListOf<Snapshot>()
    val snapshotBuilder: Snapshot.Builder = Snapshot.newBuilder()

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
      }

      snapshotList.add(snapshotBuilder.build())
    }

    builder.addAllSnapshots(snapshotList)
    os.sink().buffer().use {
      it.write(builder.build().toByteArray())
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
        val list: SnapshotList = try {
          SnapshotList.parseFrom(stream)
        } catch (e: InvalidProtocolBufferException) {
          withContext(Dispatchers.Main) {
            resultAction(false)
          }
          SnapshotList.newBuilder().build()
        }
        val total = list.snapshotsList.groupingBy { it.timeStamp }.eachCount()
        val message = buildString {
          total.forEach {
            append(
              String.format(context.getString(R.string.album_restore_detail), getFormatDateString(it.key), it.value)
            )
          }
        }
        withContext(Dispatchers.Main) {
          BaseAlertDialogBuilder(context)
            .setTitle(R.string.album_restore)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
              restoreImpl(list, resultAction)
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
              resultAction(true)
            }
            .setCancelable(true)
            .show()
        }
      }
    }
  }

  private fun restoreImpl(list: SnapshotList, resultAction: (success: Boolean) -> Unit) {
    viewModelScope.launch(Dispatchers.IO) {
      val finalList = mutableListOf<SnapshotItem>()
      val timeStampSet = mutableSetOf<Long>()
      var count = 0

      list.snapshotsList.forEach {
        if (it != null) {
          timeStampSet += it.timeStamp
          finalList += SnapshotItem(
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
            it.packageSize
          )
          count++
        }

        if (count == 50) {
          repository.insertSnapshots(finalList)
          finalList.clear()
          count = 0
        }
      }

      repository.insertSnapshots(finalList)
      repository.deleteDuplicateSnapshotItems()
      finalList.clear()
      count = 0
      timeStampSet.forEach { insertTimeStamp(it) }
      timeStampSet.maxOrNull()?.let { GlobalValues.snapshotTimestamp = it }
      withContext(Dispatchers.Main) {
        resultAction(true)
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

  fun getDashboardCount(timestamp: Long, isLeft: Boolean) = viewModelScope.launch(Dispatchers.IO) {
    Timber.d("getDashboardCount: $timestamp, $isLeft")
    val snapshotCount = repository.getSnapshots(timestamp).size
    val appCount = LocalAppDataSource.getCachedApplicationMap().size
    setEffect {
      Effect.DashboardCountChange(snapshotCount, appCount, isLeft)
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
    data class DashboardCountChange(val snapshotCount: Int, val appCount: Int, val isLeft: Boolean) : Effect()
  }
}
