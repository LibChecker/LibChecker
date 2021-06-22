package com.absinthe.libchecker.viewmodel

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.text.format.Formatter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.LibCheckerApp
import com.absinthe.libchecker.annotation.*
import com.absinthe.libchecker.bean.*
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.database.AppItemRepository
import com.absinthe.libchecker.database.entity.SnapshotDiffStoringItem
import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.protocol.Snapshot
import com.absinthe.libchecker.protocol.SnapshotList
import com.absinthe.libchecker.recyclerview.adapter.snapshot.ARROW
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libraries.utils.manager.TimeRecorder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.protobuf.InvalidProtocolBufferException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

const val CURRENT_SNAPSHOT = -1L

class SnapshotViewModel(application: Application) : AndroidViewModel(application) {

    val repository = LibCheckerApp.repository
    val allSnapshots = repository.allSnapshotItems
    val timestamp: MutableLiveData<Long> = MutableLiveData(GlobalValues.snapshotTimestamp)
    val snapshotDiffItems: MutableLiveData<List<SnapshotDiffItem>> = MutableLiveData()
    val snapshotDetailItems: MutableLiveData<List<SnapshotDetailItem>> = MutableLiveData()
    val snapshotAppsCount: MutableLiveData<Int> = MutableLiveData()
    val comparingProgressLiveData = MutableLiveData(0)

    private val gson by lazy { Gson() }

    fun computeSnapshotAppCount(timeStamp: Long) = viewModelScope.launch(Dispatchers.IO) {
        snapshotAppsCount.postValue(repository.getSnapshots(timeStamp).size)
    }

    private var compareDiffJob: Job? = null

    fun compareDiff(preTimeStamp: Long, currTimeStamp: Long = CURRENT_SNAPSHOT, shouldClearDiff: Boolean = false) {
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
        }
        compareDiffJob!!.start()
    }

    private suspend fun compareDiffWithApplicationList(preTimeStamp: Long) {
        val preList = repository.getSnapshots(preTimeStamp)
        val diffList = mutableListOf<SnapshotDiffItem>()

        if (preList.isNullOrEmpty()) {
            snapshotDiffItems.postValue(diffList)
            return
        }

        val context: Context = getApplication<LibCheckerApp>()
        val packageManager = context.packageManager
        val appList: MutableList<ApplicationInfo> = AppItemRepository.allApplicationInfoItems.value?.toMutableList() ?: mutableListOf()
        val appMap = mutableMapOf<String, ApplicationInfo>()
        val size = appList.size
        var count = 0

        var packageInfo: PackageInfo
        var versionCode: Long
        var compareDiffNode: CompareDiffNode
        var snapshotDiffItem: SnapshotDiffItem
        var snapshotDiffStoringItem: SnapshotDiffStoringItem?
        var snapshotDiffContent: String

        val allTrackItems = repository.getTrackItems()
        appList.forEach { appMap[it.packageName] = it }
        appList.clear()

        suspend fun compare(dbItem: SnapshotItem, packageInfo: PackageInfo, versionCode: Long) {
            if (versionCode > dbItem.versionCode || packageInfo.lastUpdateTime > dbItem.lastUpdatedTime ||
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
                        gson.toJson(PackageUtils.getNativeDirLibs(packageInfo))
                    ),
                    servicesDiff = SnapshotDiffItem.DiffNode(
                        dbItem.services,
                        gson.toJson(
                            PackageUtils.getComponentStringList(
                                packageInfo.packageName,
                                SERVICE,
                                false
                            )
                        )
                    ),
                    activitiesDiff = SnapshotDiffItem.DiffNode(
                        dbItem.activities,
                        gson.toJson(
                            PackageUtils.getComponentStringList(
                                packageInfo.packageName,
                                ACTIVITY,
                                false
                            )
                        )
                    ),
                    receiversDiff = SnapshotDiffItem.DiffNode(
                        dbItem.receivers,
                        gson.toJson(
                            PackageUtils.getComponentStringList(
                                packageInfo.packageName,
                                RECEIVER,
                                false
                            )
                        )
                    ),
                    providersDiff = SnapshotDiffItem.DiffNode(
                        dbItem.providers,
                        gson.toJson(
                            PackageUtils.getComponentStringList(
                                packageInfo.packageName,
                                PROVIDER,
                                false
                            )
                        )
                    ),
                    permissionsDiff = SnapshotDiffItem.DiffNode(
                        dbItem.permissions,
                        gson.toJson(PackageUtils.getPermissionsList(packageInfo.packageName))
                    ),
                    isTrackItem = allTrackItems.any { trackItem -> trackItem.packageName == packageInfo.packageName }
                )
                compareDiffNode = compareNativeAndComponentDiff(snapshotDiffItem)
                snapshotDiffItem.added = compareDiffNode.added
                snapshotDiffItem.removed = compareDiffNode.removed
                snapshotDiffItem.changed = compareDiffNode.changed
                snapshotDiffItem.moved = compareDiffNode.moved

                diffList.add(snapshotDiffItem)

                snapshotDiffContent = gson.toJson(snapshotDiffItem)
                repository.insertSnapshotDiffItems(SnapshotDiffStoringItem(
                    packageName = packageInfo.packageName,
                    lastUpdatedTime = packageInfo.lastUpdateTime,
                    diffContent = snapshotDiffContent
                ))
            }
        }

        preList.let { dbItems ->
            for (dbItem in dbItems) {
                appMap[dbItem.packageName]?.let {
                    try {
                        packageInfo = PackageUtils.getPackageInfo(it)
                        versionCode = PackageUtils.getVersionCode(packageInfo)
                        snapshotDiffStoringItem = repository.getSnapshotDiff(dbItem.packageName)

                        if (snapshotDiffStoringItem != null && snapshotDiffStoringItem!!.lastUpdatedTime == packageInfo.lastUpdateTime) {
                            try {
                                snapshotDiffItem = gson.fromJson(snapshotDiffStoringItem!!.diffContent, SnapshotDiffItem::class.java)
                                diffList.add(snapshotDiffItem)
                            } catch (e: Exception) {
                                Timber.e(e, "diffContent parsing failed")
                                compare(dbItem, packageInfo, versionCode)
                            }
                        } else {
                            compare(dbItem, packageInfo, versionCode)
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
                            deleted = true,
                            isTrackItem = allTrackItems.any { trackItem -> trackItem.packageName == dbItem.packageName }
                        )
                    )
                }
            }

            appMap.forEach { entry ->
                val info = entry.value
                try {
                    packageInfo = PackageUtils.getPackageInfo(info)
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
                                gson.toJson(PackageUtils.getNativeDirLibs(packageInfo))
                            ),
                            SnapshotDiffItem.DiffNode(
                                gson.toJson(PackageUtils.getComponentStringList(packageInfo.packageName, SERVICE, false))
                            ),
                            SnapshotDiffItem.DiffNode(
                                gson.toJson(PackageUtils.getComponentStringList(packageInfo.packageName, ACTIVITY, false))
                            ),
                            SnapshotDiffItem.DiffNode(
                                gson.toJson(PackageUtils.getComponentStringList(packageInfo.packageName, RECEIVER, false))
                            ),
                            SnapshotDiffItem.DiffNode(
                                gson.toJson(PackageUtils.getComponentStringList(packageInfo.packageName, PROVIDER, false))
                            ),
                            SnapshotDiffItem.DiffNode(
                                gson.toJson(PackageUtils.getPermissionsList(packageInfo.packageName))
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
        }

        snapshotDiffItems.postValue(diffList)
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
                        versionNameDiff = SnapshotDiffItem.DiffNode(preItem.versionName, it.versionName),
                        versionCodeDiff = SnapshotDiffItem.DiffNode(preItem.versionCode, it.versionCode),
                        abiDiff = SnapshotDiffItem.DiffNode(preItem.abi, it.abi),
                        targetApiDiff = SnapshotDiffItem.DiffNode(preItem.targetApi, it.targetApi),
                        nativeLibsDiff = SnapshotDiffItem.DiffNode(preItem.nativeLibs, it.nativeLibs),
                        servicesDiff = SnapshotDiffItem.DiffNode(preItem.services, it.services),
                        activitiesDiff = SnapshotDiffItem.DiffNode(preItem.activities, it.activities),
                        receiversDiff = SnapshotDiffItem.DiffNode(preItem.receivers, it.receivers),
                        providersDiff = SnapshotDiffItem.DiffNode(preItem.providers, it.providers),
                        permissionsDiff = SnapshotDiffItem.DiffNode(preItem.permissions, it.permissions),
                        isTrackItem = allTrackItems.any { trackItem -> trackItem.packageName == it.packageName }
                    )
                    compareDiffNode = compareNativeAndComponentDiff(snapshotDiffItem)
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
                    newInstalled = true,
                    isTrackItem = allTrackItems.any { trackItem -> trackItem.packageName == info.packageName }
                )
            )
        }

        snapshotDiffItems.postValue(diffList)
    }

    fun computeDiffDetail(entity: SnapshotDiffItem) = viewModelScope.launch {
        val list = mutableListOf<SnapshotDetailItem>()

        list.addAll(
            getNativeDiffList(
                gson.fromJson(entity.nativeLibsDiff.old, object : TypeToken<List<LibStringItem>>() {}.type),
                gson.fromJson(entity.nativeLibsDiff.new, object : TypeToken<List<LibStringItem>>() {}.type)
            )
        )
        list.addAll(
            getComponentsDiffList(
                gson.fromJson(entity.servicesDiff.old, object : TypeToken<List<String>>() {}.type),
                gson.fromJson(entity.servicesDiff.new, object : TypeToken<List<String>>() {}.type),
                SERVICE
            )
        )
        list.addAll(
            getComponentsDiffList(
                gson.fromJson(entity.activitiesDiff.old, object : TypeToken<List<String>>() {}.type),
                gson.fromJson(entity.activitiesDiff.new, object : TypeToken<List<String>>() {}.type),
                ACTIVITY
            )
        )
        list.addAll(
            getComponentsDiffList(
                gson.fromJson(entity.receiversDiff.old, object : TypeToken<List<String>>() {}.type),
                gson.fromJson(entity.receiversDiff.new, object : TypeToken<List<String>>() {}.type),
                RECEIVER
            )
        )
        list.addAll(
            getComponentsDiffList(
                gson.fromJson(entity.providersDiff.old, object : TypeToken<List<String>>() {}.type),
                gson.fromJson(entity.providersDiff.new, object : TypeToken<List<String>>() {}.type),
                PROVIDER
            )
        )
        list.addAll(
            getPermissionsDiffList(
                gson.fromJson(entity.permissionsDiff.old, object : TypeToken<List<String>>() {}.type),
                gson.fromJson(entity.permissionsDiff.new, object : TypeToken<List<String>>() {}.type)
            )
        )

        snapshotDetailItems.postValue(list)
    }

    private fun insertTimeStamp(timestamp: Long) = viewModelScope.launch(Dispatchers.IO) {
        if (timestamp == 0L) {
            return@launch
        }
        repository.insert(TimeStampItem(timestamp))
    }

    private fun getNativeDiffList(oldList: List<LibStringItem>, newList: List<LibStringItem>?): List<SnapshotDetailItem> {
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
                    list.add(
                        SnapshotDetailItem(it.name, it.name, "${sizeToString(it.size)} $ARROW ${sizeToString(item.size)}", CHANGED, NATIVE)
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
                SnapshotDetailItem(item.name, item.name, PackageUtils.sizeToString(item), REMOVED, NATIVE)
            )
        }
        for (item in tempNewList) {
            list.add(
                SnapshotDetailItem(item.name, item.name, PackageUtils.sizeToString(item), ADDED, NATIVE)
            )
        }

        return list
    }

    private fun getComponentsDiffList(oldList: List<String>, newList: List<String>?, @LibType type: Int): List<SnapshotDetailItem> {
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

    private fun getPermissionsDiffList(oldList: List<String>, newList: List<String>?): List<SnapshotDetailItem> {
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

    private fun sizeToString(size: Long): String {
        return "${Formatter.formatFileSize(LibCheckerApp.context, size)} ($size Bytes)"
    }

    data class CompareDiffNode(
        var added: Boolean = false,
        var removed: Boolean = false,
        var changed: Boolean = false,
        var moved: Boolean = false
    )

    private fun compareNativeAndComponentDiff(item: SnapshotDiffItem): CompareDiffNode {
        val nativeCompareNode = compareNativeDiff(
            gson.fromJson(
                item.nativeLibsDiff.old,
                object : TypeToken<List<LibStringItem>>() {}.type
            ),
            gson.fromJson(
                item.nativeLibsDiff.new,
                object : TypeToken<List<LibStringItem>>() {}.type
            )
        )
        val servicesCompareNode = compareComponentsDiff(
            gson.fromJson(
                item.servicesDiff.old,
                object : TypeToken<List<String>>() {}.type
            ),
            gson.fromJson(
                item.servicesDiff.new,
                object : TypeToken<List<String>>() {}.type
            )
        )
        val activitiesCompareNode = compareComponentsDiff(
            gson.fromJson(
                item.activitiesDiff.old,
                object : TypeToken<List<String>>() {}.type
            ),
            gson.fromJson(
                item.activitiesDiff.new,
                object : TypeToken<List<String>>() {}.type
            )
        )
        val receiversCompareNode = compareComponentsDiff(
            gson.fromJson(
                item.receiversDiff.old,
                object : TypeToken<List<String>>() {}.type
            ),
            gson.fromJson(
                item.receiversDiff.new,
                object : TypeToken<List<String>>() {}.type
            )
        )
        val providersCompareNode = compareComponentsDiff(
            gson.fromJson(
                item.providersDiff.old,
                object : TypeToken<List<String>>() {}.type
            ),
            gson.fromJson(
                item.providersDiff.new,
                object : TypeToken<List<String>>() {}.type
            )
        )
        val permissionsCompareNode = comparePermissionsDiff(
            gson.fromJson(
                item.permissionsDiff.old,
                object : TypeToken<List<String>>() {}.type
            ),
            gson.fromJson(
                item.permissionsDiff.new,
                object : TypeToken<List<String>>() {}.type
            )
        )

        val totalNode = CompareDiffNode()
        totalNode.added = nativeCompareNode.added or servicesCompareNode.added or activitiesCompareNode.added or receiversCompareNode.added or providersCompareNode.added or permissionsCompareNode.added
        totalNode.removed = nativeCompareNode.removed or servicesCompareNode.removed or activitiesCompareNode.removed or receiversCompareNode.removed or providersCompareNode.removed or permissionsCompareNode.removed
        totalNode.changed = nativeCompareNode.changed
        totalNode.moved = servicesCompareNode.moved or activitiesCompareNode.moved or receiversCompareNode.moved or providersCompareNode.moved

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

    private fun compareComponentsDiff(
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

    fun backup(os: OutputStream) = viewModelScope.launch(Dispatchers.IO) {
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
            }

            snapshotList.add(snapshotBuilder.build())
        }

        builder.addAllSnapshots(snapshotList)
        val str = builder.build().toByteArray()

        os.write(str)
        os.close()
    }

    fun restore(inputStream: InputStream, failedAction: () -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        inputStream.use {
            val list: SnapshotList = try {
                SnapshotList.parseFrom(inputStream)
            } catch (e: InvalidProtocolBufferException) {
                withContext(Dispatchers.Main) {
                    failedAction()
                }
                SnapshotList.newBuilder().build()
            }
            val finalList = mutableListOf<SnapshotItem>()
            val timeStampSet = mutableSetOf<Long>()
            var count = 0

            list.snapshotsList.forEach {
                if (it != null) {
                    timeStampSet.add(it.timeStamp)
                    finalList.add(
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
                            it.permissions
                        )
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
        }
    }

    fun getFormatDateString(timestamp: Long): String {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd, HH:mm:ss", Locale.getDefault())
        val date = Date(timestamp)
        return simpleDateFormat.format(date)
    }
}