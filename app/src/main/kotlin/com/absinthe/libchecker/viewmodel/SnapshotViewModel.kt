package com.absinthe.libchecker.viewmodel

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.text.format.Formatter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.bean.*
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.database.LCDatabase
import com.absinthe.libchecker.database.LCRepository
import com.absinthe.libchecker.database.SnapshotItem
import com.absinthe.libchecker.recyclerview.adapter.ARROW
import com.absinthe.libchecker.ui.main.LibReferenceActivity
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

    fun computeSnapshots(context: Context) = viewModelScope.launch(Dispatchers.IO) {
        deleteAllSnapshots()

        val dbList = mutableListOf<SnapshotItem>()
        val appList = context.packageManager
            .getInstalledApplications(PackageManager.GET_SHARED_LIBRARY_FILES)
        var packageInfo: PackageInfo
        val gson = Gson()

        for (info in appList) {
            try {
                packageInfo = PackageUtils.getPackageInfo(info)

                dbList.add(
                    SnapshotItem(
                        packageInfo.packageName,//Package name
                        info.loadLabel(context.packageManager).toString(),//App name
                        packageInfo.versionName ?: "null",//Version name
                        PackageUtils.getVersionCode(packageInfo),//Version code
                        packageInfo.firstInstallTime,//Install time
                        packageInfo.lastUpdateTime,// Update time
                        (info.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM,//Is system app
                        PackageUtils.getAbi(info.sourceDir, info.nativeLibraryDir)
                            .toShort(),//Abi type
                        info.targetSdkVersion.toShort(),//Target API
                        gson.toJson(
                            PackageUtils.getNativeDirLibs(
                                info.sourceDir,
                                info.nativeLibraryDir
                            )
                        ),//Native libs
                        gson.toJson(
                            PackageUtils.getComponentList(
                                packageInfo.packageName,
                                LibReferenceActivity.Type.TYPE_SERVICE
                            )
                        ),
                        gson.toJson(
                            PackageUtils.getComponentList(
                                packageInfo.packageName,
                                LibReferenceActivity.Type.TYPE_ACTIVITY
                            )
                        ),
                        gson.toJson(
                            PackageUtils.getComponentList(
                                packageInfo.packageName,
                                LibReferenceActivity.Type.TYPE_BROADCAST_RECEIVER
                            )
                        ),
                        gson.toJson(
                            PackageUtils.getComponentList(
                                packageInfo.packageName,
                                LibReferenceActivity.Type.TYPE_CONTENT_PROVIDER
                            )
                        )
                    )
                )
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

    fun computeDiff(context: Context) = viewModelScope.launch(Dispatchers.IO) {
        if (snapshotItems.value != null && snapshotItems.value!!.isEmpty()) return@launch

        val diffList = mutableListOf<SnapshotDiffItem>()
        val packageManager = context.packageManager
        val appList = packageManager
            .getInstalledApplications(PackageManager.GET_SHARED_LIBRARY_FILES)
        val gson = Gson()

        var packageInfo: PackageInfo
        var versionCode: Long

        snapshotItems.value?.let { dbItems ->
            for (dbItem in dbItems) {
                try {
                    appList.find { it.packageName == dbItem.packageName }?.let {
                        packageInfo = PackageUtils.getPackageInfo(it)
                        versionCode = PackageUtils.getVersionCode(packageInfo)

                        if (packageInfo.lastUpdateTime > dbItem.lastUpdatedTime) {
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
                                            PackageUtils.getNativeDirLibs(
                                                it.sourceDir,
                                                it.nativeLibraryDir
                                            )
                                        )
                                    ),
                                    SnapshotDiffItem.DiffNode(
                                        dbItem.services, gson.toJson(
                                            PackageUtils.getComponentList(
                                                packageInfo.packageName,
                                                LibReferenceActivity.Type.TYPE_SERVICE
                                            )
                                        )
                                    ),
                                    SnapshotDiffItem.DiffNode(
                                        dbItem.activities, gson.toJson(
                                            PackageUtils.getComponentList(
                                                packageInfo.packageName,
                                                LibReferenceActivity.Type.TYPE_ACTIVITY
                                            )
                                        )
                                    ),
                                    SnapshotDiffItem.DiffNode(
                                        dbItem.receivers, gson.toJson(
                                            PackageUtils.getComponentList(
                                                packageInfo.packageName,
                                                LibReferenceActivity.Type.TYPE_BROADCAST_RECEIVER
                                            )
                                        )
                                    ),
                                    SnapshotDiffItem.DiffNode(
                                        dbItem.providers, gson.toJson(
                                            PackageUtils.getComponentList(
                                                packageInfo.packageName,
                                                LibReferenceActivity.Type.TYPE_CONTENT_PROVIDER
                                            )
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
                                    PackageUtils.getNativeDirLibs(
                                        info.sourceDir,
                                        info.nativeLibraryDir
                                    )
                                )
                            ),
                            SnapshotDiffItem.DiffNode(
                                gson.toJson(
                                    PackageUtils.getComponentList(
                                        packageInfo.packageName,
                                        LibReferenceActivity.Type.TYPE_SERVICE
                                    )
                                )
                            ),
                            SnapshotDiffItem.DiffNode(
                                gson.toJson(
                                    PackageUtils.getComponentList(
                                        packageInfo.packageName,
                                        LibReferenceActivity.Type.TYPE_ACTIVITY
                                    )
                                )
                            ),
                            SnapshotDiffItem.DiffNode(
                                gson.toJson(
                                    PackageUtils.getComponentList(
                                        packageInfo.packageName,
                                        LibReferenceActivity.Type.TYPE_BROADCAST_RECEIVER
                                    )
                                )
                            ),
                            SnapshotDiffItem.DiffNode(
                                gson.toJson(
                                    PackageUtils.getComponentList(
                                        packageInfo.packageName,
                                        LibReferenceActivity.Type.TYPE_CONTENT_PROVIDER
                                    )
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
                )
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
                )
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
                )
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
                )
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
                        SnapshotDetailItem(
                            it.name,
                            "${sizeToString(it.size)} $ARROW ${sizeToString(item.size)}",
                            CHANGED,
                            TYPE_NATIVE
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
                    PackageUtils.sizeToString(item.size),
                    REMOVED,
                    TYPE_NATIVE
                )
            )
        }
        for (item in tempNewList) {
            list.add(
                SnapshotDetailItem(
                    item.name,
                    PackageUtils.sizeToString(item.size),
                    ADDED,
                    TYPE_NATIVE
                )
            )
        }

        return list
    }

    private fun getComponentsDiffList(
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
            tempOldList.remove(item)
            tempNewList.remove(item)
        }

        for (item in tempOldList) {
            list.add(
                SnapshotDetailItem(
                    item,
                    "",
                    REMOVED,
                    TYPE_COMPONENT
                )
            )
        }
        for (item in tempNewList) {
            list.add(
                SnapshotDetailItem(
                    item,
                    "",
                    ADDED,
                    TYPE_COMPONENT
                )
            )
        }

        return list
    }

    private fun sizeToString(size: Long): String {
        return "${Formatter.formatFileSize(Utils.getApp(), size)} ($size Bytes)"
    }
}