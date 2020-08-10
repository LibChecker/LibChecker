package com.absinthe.libchecker.viewmodel

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.text.format.Formatter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.LibCheckerApp
import com.absinthe.libchecker.bean.*
import com.absinthe.libchecker.constant.*
import com.absinthe.libchecker.database.LCDatabase
import com.absinthe.libchecker.database.LCRepository
import com.absinthe.libchecker.database.SnapshotItem
import com.absinthe.libchecker.recyclerview.adapter.snapshot.ARROW
import com.absinthe.libchecker.utils.PackageUtils
import com.blankj.utilcode.util.Utils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SnapshotViewModel(application: Application) : AndroidViewModel(application) {

    val timestamp: MutableLiveData<Long> = MutableLiveData(GlobalValues.snapshotTimestamp)
    val snapshotItems: LiveData<List<SnapshotItem>>
    val snapshotDiffItems: MutableLiveData<List<SnapshotDiffItem>> = MutableLiveData()
    val snapshotDetailItems: MutableLiveData<List<SnapshotDetailItem>> = MutableLiveData()

    private val repository: LCRepository

    init {
        val lcDao = LCDatabase.getDatabase(application).lcDao()
        repository = LCRepository(lcDao)
        snapshotItems = repository.allSnapshotItems
    }

    fun insertSnapshots(items: List<SnapshotItem>) = viewModelScope.launch(Dispatchers.IO) {
        repository.insertSnapshots(items)
    }

    fun deleteAllSnapshots() = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteAllSnapshots()
    }

    fun computeSnapshots() = viewModelScope.launch(Dispatchers.IO) {
        deleteAllSnapshots()

        val context: Context = getApplication<LibCheckerApp>()
        var appList: List<ApplicationInfo>? = null
        while (appList == null) {
            appList = try {
                PackageUtils.getInstallApplications()
            } catch (e: Exception) {
                null
            }
        }

        val dbList = mutableListOf<SnapshotItem>()
        val gson = Gson()

        for (info in appList) {
            try {
                PackageUtils.getPackageInfo(info.packageName).let {
                    dbList.add(
                        SnapshotItem(
                            it.packageName,//Package name
                            info.loadLabel(context.packageManager).toString(),//App name
                            it.versionName ?: "null",//Version name
                            PackageUtils.getVersionCode(it),//Version code
                            it.firstInstallTime,//Install time
                            it.lastUpdateTime,// Update time
                            (info.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM,//Is system app
                            PackageUtils.getAbi(info.sourceDir, info.nativeLibraryDir)
                                .toShort(),//Abi type
                            info.targetSdkVersion.toShort(),//Target API
                            gson.toJson(
                                PackageUtils.getNativeDirLibs(info.sourceDir, info.nativeLibraryDir)
                            ),//Native libs
                            gson.toJson(
                                PackageUtils.getComponentList(it.packageName, SERVICE, false)
                            ),
                            gson.toJson(
                                PackageUtils.getComponentList(it.packageName, ACTIVITY, false)
                            ),
                            gson.toJson(
                                PackageUtils.getComponentList(it.packageName, RECEIVER, false)
                            ),
                            gson.toJson(
                                PackageUtils.getComponentList(it.packageName, PROVIDER, false)
                            )
                        )
                    )
                }
            } catch (e: Exception) {
                continue
            }
        }

        insertSnapshots(dbList)
        withContext(Dispatchers.Main) {
            val ts = System.currentTimeMillis()
            GlobalValues.snapshotTimestamp = ts
            timestamp.value = ts
        }
    }

    fun computeDiff() = viewModelScope.launch(Dispatchers.IO) {
        if (snapshotItems.value.isNullOrEmpty()) return@launch

        val context: Context = getApplication<LibCheckerApp>()
        var appList: MutableList<ApplicationInfo>? = null
        while (appList == null) {
            appList = try {
                PackageUtils.getInstallApplications().toMutableList()
            } catch (e: Exception) {
                null
            }
        }

        val diffList = mutableListOf<SnapshotDiffItem>()
        val packageManager = context.packageManager
        val gson = Gson()

        var packageInfo: PackageInfo
        var versionCode: Long

        snapshotItems.value?.let { dbItems ->
            for (dbItem in dbItems) {
                try {
                    appList.find { it.packageName == dbItem.packageName }?.let {
                        packageInfo = PackageUtils.getPackageInfo(it)
                        versionCode = PackageUtils.getVersionCode(packageInfo)

                        if (versionCode > dbItem.versionCode || packageInfo.lastUpdateTime > dbItem.lastUpdatedTime) {
                            diffList.add(
                                SnapshotDiffItem(
                                    packageInfo.packageName,
                                    packageInfo.lastUpdateTime,
                                    SnapshotDiffItem.DiffNode(
                                        dbItem.label,
                                        it.loadLabel(packageManager).toString()
                                    ),
                                    SnapshotDiffItem.DiffNode(
                                        dbItem.versionName,
                                        packageInfo.versionName
                                    ),
                                    SnapshotDiffItem.DiffNode(dbItem.versionCode, versionCode),
                                    SnapshotDiffItem.DiffNode(
                                        dbItem.abi,
                                        PackageUtils.getAbi(it.sourceDir, it.nativeLibraryDir)
                                            .toShort()
                                    ),
                                    SnapshotDiffItem.DiffNode(
                                        dbItem.targetApi,
                                        it.targetSdkVersion.toShort()
                                    ),
                                    SnapshotDiffItem.DiffNode(
                                        dbItem.nativeLibs, gson.toJson(
                                            PackageUtils.getNativeDirLibs(it.sourceDir, it.nativeLibraryDir)
                                        )
                                    ),
                                    SnapshotDiffItem.DiffNode(
                                        dbItem.services, gson.toJson(
                                            PackageUtils.getComponentList(packageInfo.packageName, SERVICE, false)
                                        )
                                    ),
                                    SnapshotDiffItem.DiffNode(
                                        dbItem.activities, gson.toJson(
                                            PackageUtils.getComponentList(packageInfo.packageName, ACTIVITY, false)
                                        )
                                    ),
                                    SnapshotDiffItem.DiffNode(
                                        dbItem.receivers, gson.toJson(
                                            PackageUtils.getComponentList(packageInfo.packageName, RECEIVER, false)
                                        )
                                    ),
                                    SnapshotDiffItem.DiffNode(
                                        dbItem.providers, gson.toJson(
                                            PackageUtils.getComponentList(packageInfo.packageName, PROVIDER, false)
                                        )
                                    )
                                )
                            )
                        }

                        appList.remove(it)
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
                                deleted = true
                            )
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    continue
                }
            }

            for (info in appList) {
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
                            SnapshotDiffItem.DiffNode(
                                PackageUtils.getAbi(
                                    info.sourceDir,
                                    info.nativeLibraryDir
                                ).toShort()
                            ),
                            SnapshotDiffItem.DiffNode(info.targetSdkVersion.toShort()),
                            SnapshotDiffItem.DiffNode(
                                gson.toJson(
                                    PackageUtils.getNativeDirLibs(info.sourceDir, info.nativeLibraryDir)
                                )
                            ),
                            SnapshotDiffItem.DiffNode(
                                gson.toJson(
                                    PackageUtils.getComponentList(packageInfo.packageName, SERVICE, false)
                                )
                            ),
                            SnapshotDiffItem.DiffNode(
                                gson.toJson(
                                    PackageUtils.getComponentList(packageInfo.packageName, ACTIVITY, false)
                                )
                            ),
                            SnapshotDiffItem.DiffNode(
                                gson.toJson(
                                    PackageUtils.getComponentList(packageInfo.packageName, RECEIVER, false)
                                )
                            ),
                            SnapshotDiffItem.DiffNode(
                                gson.toJson(
                                    PackageUtils.getComponentList(packageInfo.packageName, PROVIDER, false)
                                )
                            ),
                            newInstalled = true
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    continue
                }
            }
        }

        withContext(Dispatchers.Main) {
            snapshotDiffItems.value = diffList
        }
    }

    fun computeDiffDetail(entity: SnapshotDiffItem) = viewModelScope.launch(Dispatchers.IO) {
        val list = mutableListOf<SnapshotDetailItem>()
        val gson = Gson()

        list.addAll(
            getNativeDiffList(
                gson.fromJson(
                    entity.nativeLibsDiff.old,
                    object : TypeToken<List<LibStringItem>>() {}.type
                ),
                gson.fromJson(
                    entity.nativeLibsDiff.new,
                    object : TypeToken<List<LibStringItem>>() {}.type
                )
            )
        )
        list.addAll(
            getComponentsDiffList(
                gson.fromJson(
                    entity.servicesDiff.old,
                    object : TypeToken<List<String>>() {}.type
                ),
                gson.fromJson(
                    entity.servicesDiff.new,
                    object : TypeToken<List<String>>() {}.type
                ),
                SERVICE
            )
        )
        list.addAll(
            getComponentsDiffList(
                gson.fromJson(
                    entity.activitiesDiff.old,
                    object : TypeToken<List<String>>() {}.type
                ),
                gson.fromJson(
                    entity.activitiesDiff.new,
                    object : TypeToken<List<String>>() {}.type
                ),
                ACTIVITY
            )
        )
        list.addAll(
            getComponentsDiffList(
                gson.fromJson(
                    entity.receiversDiff.old,
                    object : TypeToken<List<String>>() {}.type
                ),
                gson.fromJson(
                    entity.receiversDiff.new,
                    object : TypeToken<List<String>>() {}.type
                ),
                RECEIVER
            )
        )
        list.addAll(
            getComponentsDiffList(
                gson.fromJson(
                    entity.providersDiff.old,
                    object : TypeToken<List<String>>() {}.type
                ),
                gson.fromJson(
                    entity.providersDiff.new,
                    object : TypeToken<List<String>>() {}.type
                ),
                PROVIDER
            )
        )

        withContext(Dispatchers.Main) {
            snapshotDetailItems.value = list
        }
    }

    private fun getNativeDiffList(
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
                SnapshotDetailItem(item.name, item.name, PackageUtils.sizeToString(item.size), REMOVED, NATIVE)
            )
        }
        for (item in tempNewList) {
            list.add(
                SnapshotDetailItem(item.name, item.name, PackageUtils.sizeToString(item.size), ADDED, NATIVE)
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
                    SnapshotDetailItem(
                        item,
                        "$it\n$ARROW\n$item",
                        "",
                        MOVED,
                        type
                    )
                )
                deletedOldList.add(it)
                deletedNewList.add(item)
            }
        }
        tempOldList.removeAll(deletedOldList)
        tempNewList.removeAll(deletedNewList)

        for (item in tempOldList) {
            list.add(
                SnapshotDetailItem(
                    item,
                    item,
                    "",
                    REMOVED,
                    type
                )
            )
        }
        for (item in tempNewList) {
            list.add(
                SnapshotDetailItem(
                    item,
                    item,
                    "",
                    ADDED,
                    type
                )
            )
        }

        return list
    }

    private fun sizeToString(size: Long): String {
        return "${Formatter.formatFileSize(Utils.getApp(), size)} ($size Bytes)"
    }
}