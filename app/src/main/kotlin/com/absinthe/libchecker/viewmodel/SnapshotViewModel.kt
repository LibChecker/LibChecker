package com.absinthe.libchecker.viewmodel

import android.app.Application
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.LibCheckerApp
import com.absinthe.libchecker.SystemServices
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.app.Global
import com.absinthe.libchecker.bean.ADDED
import com.absinthe.libchecker.bean.CHANGED
import com.absinthe.libchecker.bean.LibStringItem
import com.absinthe.libchecker.bean.MOVED
import com.absinthe.libchecker.bean.REMOVED
import com.absinthe.libchecker.bean.SnapshotDetailItem
import com.absinthe.libchecker.bean.SnapshotDiffItem
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.database.AppItemRepository
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.database.entity.SnapshotDiffStoringItem
import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.protocol.Snapshot
import com.absinthe.libchecker.protocol.SnapshotList
import com.absinthe.libchecker.recyclerview.adapter.snapshot.ARROW
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.sizeToString
import com.absinthe.libchecker.utils.fromJson
import com.absinthe.libchecker.utils.toJson
import com.absinthe.libraries.utils.manager.TimeRecorder
import com.google.protobuf.InvalidProtocolBufferException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val CURRENT_SNAPSHOT = -1L

class SnapshotViewModel(application: Application) : AndroidViewModel(application) {

  val repository = Repositories.lcRepository
  val allSnapshots = repository.allSnapshotItemsFlow
  val timestamp: MutableLiveData<Long> = MutableLiveData(GlobalValues.snapshotTimestamp)
  val snapshotDiffItems: MutableLiveData<List<SnapshotDiffItem>> = MutableLiveData()
  val snapshotDetailItems: MutableLiveData<List<SnapshotDetailItem>> = MutableLiveData()
  val snapshotAppsCount: MutableLiveData<Int> = MutableLiveData()
  val comparingProgressLiveData = MutableLiveData(0)

  fun computeSnapshotAppCount(timeStamp: Long) = viewModelScope.launch(Dispatchers.IO) {
    snapshotAppsCount.postValue(repository.getSnapshots(timeStamp).size)
  }

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
      Global.applicationListJob?.join()
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
    }
    compareDiffJob!!.start()
  }

  private suspend fun compareDiffWithApplicationList(preTimeStamp: Long) {
    val preList = repository.getSnapshots(preTimeStamp)
    val diffList = mutableListOf<SnapshotDiffItem>()

    if (preList.isNullOrEmpty() || preTimeStamp == 0L) {
      snapshotDiffItems.postValue(diffList)
      return
    }

    val context: Context = getApplication<LibCheckerApp>()
    val packageManager = context.packageManager
    val appMap = AppItemRepository.getApplicationInfoMap().toMutableMap()
    val size = appMap.size

    var count = 0
    var packageInfo: PackageInfo
    var versionCode: Long
    var compareDiffNode: CompareDiffNode
    var snapshotDiffItem: SnapshotDiffItem
    var snapshotDiffStoringItem: SnapshotDiffStoringItem?
    var snapshotDiffContent: String

    val allTrackItems = repository.getTrackItems()

    suspend fun compare(dbItem: SnapshotItem, packageInfo: PackageInfo, versionCode: Long) {
      if (versionCode != dbItem.versionCode ||
        packageInfo.lastUpdateTime != dbItem.lastUpdatedTime ||
        (dbItem.packageSize != 0L && PackageUtils.getPackageSize(
          packageInfo,
          true
        ) != dbItem.packageSize) ||
        allTrackItems.any { trackItem -> trackItem.packageName == dbItem.packageName }
      ) {
        snapshotDiffItem = SnapshotDiffItem(
          packageName = packageInfo.packageName,
          updateTime = packageInfo.lastUpdateTime,
          labelDiff = SnapshotDiffItem.DiffNode(
            dbItem.label,
            packageInfo.applicationInfo.loadLabel(packageManager).toString()
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
            PackageUtils.getAbi(packageInfo.applicationInfo)
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
            PackageUtils.getPermissionsList(packageInfo.packageName).toJson().orEmpty()
          ),
          metadataDiff = SnapshotDiffItem.DiffNode(
            dbItem.metadata,
            PackageUtils.getMetaDataItems(packageInfo).toJson().orEmpty()
          ),
          packageSizeDiff = SnapshotDiffItem.DiffNode(
            dbItem.packageSize,
            PackageUtils.getPackageSize(packageInfo, true)
          ),
          isTrackItem = allTrackItems.any { trackItem -> trackItem.packageName == packageInfo.packageName }
        )
        compareDiffNode = compareDiffIndicator(snapshotDiffItem)
        snapshotDiffItem.added = compareDiffNode.added
        snapshotDiffItem.removed = compareDiffNode.removed
        snapshotDiffItem.changed = compareDiffNode.changed
        snapshotDiffItem.moved = compareDiffNode.moved

        diffList.add(snapshotDiffItem)

        snapshotDiffContent = snapshotDiffItem.toJson().orEmpty()
        repository.insertSnapshotDiffItems(
          SnapshotDiffStoringItem(
            packageName = packageInfo.packageName,
            lastUpdatedTime = packageInfo.lastUpdateTime,
            diffContent = snapshotDiffContent
          )
        )
      }
    }

    for (dbItem in preList) {
      appMap[dbItem.packageName]?.let {
        try {
          packageInfo = PackageUtils.getPackageInfo(it, PackageManager.GET_META_DATA)
          versionCode = PackageUtils.getVersionCode(packageInfo)
          snapshotDiffStoringItem = repository.getSnapshotDiff(dbItem.packageName)

          if (snapshotDiffStoringItem?.lastUpdatedTime != packageInfo.lastUpdateTime) {
            compare(dbItem, packageInfo, versionCode)
          } else {
            try {
              snapshotDiffStoringItem?.diffContent?.fromJson<SnapshotDiffItem>()?.let { item ->
                diffList.add(item)
              }
            } catch (e: IOException) {
              Timber.e(e, "diffContent parsing failed")
              compare(dbItem, packageInfo, versionCode)
            }
          }
        } catch (e: Exception) {
          Timber.e(e)
        } finally {
          appMap.remove(it.packageName)
          count++
          comparingProgressLiveData.postValue(count * 100 / size)
        }
      } ?: run {
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
    }

    appMap.forEach { entry ->
      val info = entry.value
      try {
        packageInfo = PackageUtils.getPackageInfo(info, PackageManager.GET_META_DATA)
        versionCode = PackageUtils.getVersionCode(packageInfo)

        diffList.add(
          SnapshotDiffItem(
            packageInfo.packageName,
            packageInfo.lastUpdateTime,
            SnapshotDiffItem.DiffNode(info.loadLabel(packageManager).toString()),
            SnapshotDiffItem.DiffNode(packageInfo.versionName),
            SnapshotDiffItem.DiffNode(versionCode),
            SnapshotDiffItem.DiffNode(PackageUtils.getAbi(info).toShort()),
            SnapshotDiffItem.DiffNode(info.targetSdkVersion.toShort()),
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
              PackageUtils.getPermissionsList(packageInfo.packageName).toJson().orEmpty()
            ),
            SnapshotDiffItem.DiffNode(
              PackageUtils.getMetaDataItems(packageInfo).toJson().orEmpty()
            ),
            SnapshotDiffItem.DiffNode(
              PackageUtils.getPackageSize(packageInfo, true)
            ),
            newInstalled = true,
            isTrackItem = allTrackItems.any { trackItem -> trackItem.packageName == packageInfo.packageName }
          )
        )
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

  private suspend fun compareDiffWithSnapshotList(preTimeStamp: Long, currTimeStamp: Long) {
    val preList = repository.getSnapshots(preTimeStamp)
    if (preList.isNullOrEmpty()) {
      return
    }

    val currList = repository.getSnapshots(currTimeStamp).toMutableList()
    if (currList.isNullOrEmpty()) {
      return
    }

    val currMap = mutableMapOf<String, SnapshotItem>()
    val diffList = mutableListOf<SnapshotDiffItem>()

    var compareDiffNode: CompareDiffNode
    var snapshotDiffItem: SnapshotDiffItem

    val allTrackItems = repository.getTrackItems()

    currList.forEach { currMap[it.packageName] = it }
    
    for (preItem in preList) {
      currMap[preItem.packageName]?.let {
        if (it.versionCode > preItem.versionCode || it.lastUpdatedTime > preItem.lastUpdatedTime) {
          snapshotDiffItem = SnapshotDiffItem(
            packageName = it.packageName,
            updateTime = it.lastUpdatedTime,
            labelDiff = SnapshotDiffItem.DiffNode(preItem.label, it.label),
            versionNameDiff = SnapshotDiffItem.DiffNode(
              preItem.versionName,
              it.versionName
            ),
            versionCodeDiff = SnapshotDiffItem.DiffNode(
              preItem.versionCode,
              it.versionCode
            ),
            abiDiff = SnapshotDiffItem.DiffNode(preItem.abi, it.abi),
            targetApiDiff = SnapshotDiffItem.DiffNode(preItem.targetApi, it.targetApi),
            nativeLibsDiff = SnapshotDiffItem.DiffNode(
              preItem.nativeLibs,
              it.nativeLibs
            ),
            servicesDiff = SnapshotDiffItem.DiffNode(preItem.services, it.services),
            activitiesDiff = SnapshotDiffItem.DiffNode(
              preItem.activities,
              it.activities
            ),
            receiversDiff = SnapshotDiffItem.DiffNode(preItem.receivers, it.receivers),
            providersDiff = SnapshotDiffItem.DiffNode(preItem.providers, it.providers),
            permissionsDiff = SnapshotDiffItem.DiffNode(
              preItem.permissions,
              it.permissions
            ),
            metadataDiff = SnapshotDiffItem.DiffNode(
              preItem.metadata,
              it.metadata
            ),
            packageSizeDiff = SnapshotDiffItem.DiffNode(
              preItem.packageSize,
              it.packageSize
            ),
            isTrackItem = allTrackItems.any { trackItem -> trackItem.packageName == it.packageName }
          )
          compareDiffNode = compareDiffIndicator(snapshotDiffItem)
          snapshotDiffItem.added = compareDiffNode.added
          snapshotDiffItem.removed = compareDiffNode.removed
          snapshotDiffItem.changed = compareDiffNode.changed
          snapshotDiffItem.moved = compareDiffNode.moved

          diffList.add(snapshotDiffItem)
        }
        currMap.remove(it.packageName)
      } ?: run {
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
    }

    currMap.forEach {
      val info = it.value
      diffList.add(
        SnapshotDiffItem(
          info.packageName,
          info.lastUpdatedTime,
          SnapshotDiffItem.DiffNode(info.label),
          SnapshotDiffItem.DiffNode(info.versionName),
          SnapshotDiffItem.DiffNode(info.versionCode),
          SnapshotDiffItem.DiffNode(info.abi),
          SnapshotDiffItem.DiffNode(info.targetApi),
          SnapshotDiffItem.DiffNode(info.nativeLibs),
          SnapshotDiffItem.DiffNode(info.services),
          SnapshotDiffItem.DiffNode(info.activities),
          SnapshotDiffItem.DiffNode(info.receivers),
          SnapshotDiffItem.DiffNode(info.providers),
          SnapshotDiffItem.DiffNode(info.permissions),
          SnapshotDiffItem.DiffNode(info.metadata),
          SnapshotDiffItem.DiffNode(info.packageSize),
          newInstalled = true,
          isTrackItem = allTrackItems.any { trackItem -> trackItem.packageName == info.packageName }
        )
      )
    }

    snapshotDiffItems.postValue(diffList)
    if (diffList.isNotEmpty()) {
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
        PackageManager.GET_META_DATA
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
            PackageUtils.getAbi(packageInfo.applicationInfo).toShort()
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
            PackageUtils.getPermissionsList(packageInfo.packageName).toJson().orEmpty()
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
          SnapshotDiffItem.DiffNode(PackageUtils.getAbi(packageInfo.applicationInfo).toShort()),
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
            PackageUtils.getPermissionsList(packageInfo.packageName).toJson().orEmpty()
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
          if (entity.nativeLibsDiff.new != null) {
            entity.nativeLibsDiff.new.fromJson<List<LibStringItem>>(
              List::class.java,
              LibStringItem::class.java
            )
          } else {
            null
          }
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
          ) ?: emptyList(),
          if (entity.permissionsDiff.new != null) {
            entity.permissionsDiff.new.fromJson<List<String>>(
              List::class.java,
              String::class.java
            )
          } else {
            null
          }
        )
      )

      list.addAll(
        getMetadataDiffList(
          entity.metadataDiff.old.fromJson<List<LibStringItem>>(
            List::class.java,
            LibStringItem::class.java
          ) ?: emptyList(),
          if (entity.metadataDiff.new != null) {
            entity.metadataDiff.new.fromJson<List<LibStringItem>>(
              List::class.java,
              LibStringItem::class.java
            )
          } else {
            null
          }
        )
      )

      snapshotDetailItems.postValue(list)
    }

  private fun addComponentDiffInfoFromJson(
    list: MutableList<SnapshotDetailItem>,
    diffNode: SnapshotDiffItem.DiffNode<String>,
    @LibType libType: Int
  ) {
    list.addAll(
      getComponentsDiffList(
        diffNode.old.fromJson<List<String>>(
          List::class.java,
          String::class.java
        ) ?: emptyList(),
        if (diffNode.new != null) {
          diffNode.new.fromJson<List<String>>(
            List::class.java,
            String::class.java
          )
        } else {
          null
        },
        libType
      )
    )
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
    val sameList = mutableListOf<LibStringItem>()

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
        sameList.add(item)
      }
    }

    for (item in sameList) {
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
    oldList: List<String>,
    newList: List<String>?,
    @LibType type: Int
  ): List<SnapshotDetailItem> {
    val list = mutableListOf<SnapshotDetailItem>()

    if (newList == null) {
      return list
    }

    val tempOldList = oldList.toMutableList()
    val tempNewList = newList.toMutableList()
    val sameList = mutableListOf<String>()

    for (item in tempNewList) {
      oldList.find { it == item }?.let {
        sameList.add(item)
      }
    }

    for (item in sameList) {
      tempOldList.remove(item)
      tempNewList.remove(item)
    }

    var simpleName: String
    val deletedOldList = mutableListOf<String>()
    val deletedNewList = mutableListOf<String>()

    for (item in tempNewList) {
      simpleName = item.substringAfterLast(".")
      tempOldList.find { it.substringAfterLast(".") == simpleName }?.let {
        list.add(
          SnapshotDetailItem(item, "$it\n$ARROW\n$item", "", MOVED, type)
        )
        deletedOldList.add(it)
        deletedNewList.add(item)
      }
    }
    tempOldList.removeAll(deletedOldList)
    tempNewList.removeAll(deletedNewList)

    for (item in tempOldList) {
      list.add(
        SnapshotDetailItem(item, item, "", REMOVED, type)
      )
    }
    for (item in tempNewList) {
      list.add(
        SnapshotDetailItem(item, item, "", ADDED, type)
      )
    }

    return list
  }

  private fun getPermissionsDiffList(
    oldList: List<String>,
    newList: List<String>?
  ): List<SnapshotDetailItem> {
    val list = mutableListOf<SnapshotDetailItem>()

    if (newList == null) {
      return list
    }

    val tempOldList = oldList.toMutableList()
    val tempNewList = newList.toMutableList()
    val sameList = mutableListOf<String>()

    for (item in tempNewList) {
      oldList.find { it == item }?.let {
        sameList.add(item)
      }
    }

    for (item in sameList) {
      tempOldList.removeAll(sameList)
      tempNewList.removeAll(sameList)
    }

    for (item in tempOldList) {
      list.add(
        SnapshotDetailItem(item, item, "", REMOVED, PERMISSION)
      )
    }
    for (item in tempNewList) {
      list.add(
        SnapshotDetailItem(item, item, "", ADDED, PERMISSION)
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
    val sameList = mutableListOf<LibStringItem>()

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
        sameList.add(item)
      }
    }

    for (item in sameList) {
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
    val nativeCompareNode = compareNativeDiff(
      item.nativeLibsDiff.old.fromJson<List<LibStringItem>>(
        List::class.java,
        LibStringItem::class.java
      ) ?: emptyList(),
      if (item.nativeLibsDiff.new != null) {
        item.nativeLibsDiff.new.fromJson<List<LibStringItem>>(
          List::class.java,
          LibStringItem::class.java
        )
      } else {
        null
      }
    )
    val servicesCompareNode = compareComponentsDiff(item.servicesDiff)
    val activitiesCompareNode = compareComponentsDiff(item.activitiesDiff)
    val receiversCompareNode = compareComponentsDiff(item.receiversDiff)
    val providersCompareNode = compareComponentsDiff(item.providersDiff)
    val permissionsCompareNode = comparePermissionsDiff(
      item.permissionsDiff.old.fromJson<List<String>>(
        List::class.java,
        String::class.java
      ) ?: emptyList(),
      if (item.permissionsDiff.new != null) {
        item.permissionsDiff.new.fromJson<List<String>>(
          List::class.java,
          String::class.java
        )
      } else {
        null
      }
    )
    val metadataCompareNode = compareMetadataDiff(
      item.metadataDiff.old.fromJson<List<LibStringItem>>(
        List::class.java,
        LibStringItem::class.java
      ) ?: emptyList(),
      if (item.metadataDiff.new != null) {
        item.metadataDiff.new.fromJson<List<LibStringItem>>(
          List::class.java,
          LibStringItem::class.java
        )
      } else {
        null
      }
    )

    val totalNode = CompareDiffNode()
    totalNode.added =
      nativeCompareNode.added or servicesCompareNode.added or activitiesCompareNode.added or receiversCompareNode.added or providersCompareNode.added or permissionsCompareNode.added or metadataCompareNode.added
    totalNode.removed =
      nativeCompareNode.removed or servicesCompareNode.removed or activitiesCompareNode.removed or receiversCompareNode.removed or providersCompareNode.removed or permissionsCompareNode.removed or metadataCompareNode.removed
    totalNode.changed = nativeCompareNode.changed or metadataCompareNode.changed
    totalNode.moved =
      servicesCompareNode.moved or activitiesCompareNode.moved or receiversCompareNode.moved or providersCompareNode.moved

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

    val oldList = diffNode.old.fromJson<List<String>>(
      List::class.java,
      String::class.java
    ) ?: emptyList()
    val newList = diffNode.new.fromJson<List<String>>(
      List::class.java,
      String::class.java
    ) ?: emptyList()
    val tempOldList = oldList.toMutableList()
    val tempNewList = newList.toMutableList()
    val sameList = mutableListOf<String>()
    val node = CompareDiffNode()

    for (item in tempNewList) {
      oldList.find { it == item }?.let {
        sameList.add(item)
      }
    }

    for (item in sameList) {
      tempOldList.remove(item)
      tempNewList.remove(item)
    }

    var simpleName: String
    val deletedOldList = mutableListOf<String>()
    val deletedNewList = mutableListOf<String>()

    for (item in tempNewList) {
      simpleName = item.substringAfterLast(".")
      tempOldList.find { it.substringAfterLast(".") == simpleName }?.let {
        node.moved = true
        deletedOldList.add(it)
        deletedNewList.add(item)
      }
    }
    tempOldList.removeAll(deletedOldList)
    tempNewList.removeAll(deletedNewList)

    if (tempOldList.isNotEmpty()) {
      node.removed = true
    }
    if (tempNewList.isNotEmpty()) {
      node.added = true
    }
    return node
  }

  private fun comparePermissionsDiff(
    oldList: List<String>,
    newList: List<String>?
  ): CompareDiffNode {
    if (newList == null) {
      return CompareDiffNode(removed = true)
    }

    val tempOldList = oldList.toMutableList()
    val tempNewList = newList.toMutableList()
    val sameList = mutableListOf<String>()
    val node = CompareDiffNode()

    for (item in tempNewList) {
      oldList.find { it == item }?.let {
        sameList.add(item)
      }
    }

    for (item in sameList) {
      tempOldList.remove(item)
      tempNewList.remove(item)
    }

    if (tempOldList.isNotEmpty()) {
      node.removed = true
    }
    if (tempNewList.isNotEmpty()) {
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
      }

      snapshotList.add(snapshotBuilder.build())
    }

    builder.addAllSnapshots(snapshotList)
    val str = builder.build().toByteArray()

    os.write(str)
    os.close()

    withContext(Dispatchers.Main) {
      resultAction()
    }
  }

  fun restore(inputStream: InputStream, resultAction: (success: Boolean) -> Unit) {
    viewModelScope.launch(Dispatchers.IO) {
      inputStream.use {
        val list: SnapshotList = try {
          SnapshotList.parseFrom(inputStream)
        } catch (e: InvalidProtocolBufferException) {
          withContext(Dispatchers.Main) {
            resultAction(false)
          }
          SnapshotList.newBuilder().build()
        }
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
        finalList.clear()
        count = 0
        timeStampSet.forEach { insertTimeStamp(it) }
        timeStampSet.maxOrNull()?.let { GlobalValues.snapshotTimestamp = it }
        withContext(Dispatchers.Main) {
          resultAction(true)
        }
      }
    }
  }

  fun getFormatDateString(timestamp: Long): String {
    val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd, HH:mm:ss", Locale.getDefault())
    val date = Date(timestamp)
    return simpleDateFormat.format(date)
  }
}
