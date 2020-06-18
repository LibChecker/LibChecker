package com.absinthe.libchecker.viewmodel

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.bean.SnapshotDiffItem
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.database.LCDatabase
import com.absinthe.libchecker.database.LCRepository
import com.absinthe.libchecker.database.SnapshotItem
import com.absinthe.libchecker.ui.main.LibReferenceActivity
import com.absinthe.libchecker.utils.PackageUtils
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SnapshotViewModel(application: Application) : AndroidViewModel(application) {

    val timestamp: MutableLiveData<Long> = MutableLiveData(GlobalValues.snapshotTimestamp)
    val snapshotItems: LiveData<List<SnapshotItem>>
    val snapshotDiffItems: MutableLiveData<List<SnapshotDiffItem>> = MutableLiveData()

    private val repository: LCRepository

    init {
        val lcDao = LCDatabase.getDatabase(application).lcDao()
        repository = LCRepository(lcDao)
        snapshotItems = lcDao.getSnapshots()
    }

    fun computeSnapshots(context: Context) = viewModelScope.launch(Dispatchers.IO) {
        deleteAllSnapshots()

        val dbList = mutableListOf<SnapshotItem>()
        val appList = context.packageManager
            .getInstalledApplications(PackageManager.GET_SHARED_LIBRARY_FILES)
        var packageInfo: PackageInfo
        val gson = Gson()

        for (info in appList) {
            packageInfo = PackageUtils.getPackageInfo(info)

            dbList.add(
                SnapshotItem(
                    packageInfo.packageName,//Package name
                    info.loadLabel(context.packageManager).toString(),//App name
                    packageInfo.versionName,//Version name
                    PackageUtils.getVersionCode(packageInfo),//Version code
                    packageInfo.firstInstallTime,//Install time
                    packageInfo.lastUpdateTime,// Update time
                    (info.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM,//Is system app
                    PackageUtils.getAbi(info.sourceDir, info.nativeLibraryDir).toShort(),//Abi type
                    info.targetSdkVersion.toShort(),//Target API
                    gson.toJson(
                        PackageUtils.getNativeDirLibs(
                            info.sourceDir,
                            info.nativeLibraryDir
                        )
                    ),//Native libs
                    gson.toJson(PackageUtils.getComponentList(
                        packageInfo.packageName,
                        LibReferenceActivity.Type.TYPE_SERVICE
                    )),
                    gson.toJson(PackageUtils.getComponentList(
                        packageInfo.packageName,
                        LibReferenceActivity.Type.TYPE_ACTIVITY
                    )),
                    gson.toJson(PackageUtils.getComponentList(
                        packageInfo.packageName,
                        LibReferenceActivity.Type.TYPE_BROADCAST_RECEIVER
                    )),
                    gson.toJson(PackageUtils.getComponentList(
                        packageInfo.packageName,
                        LibReferenceActivity.Type.TYPE_CONTENT_PROVIDER
                    ))
                )
            )
        }

        insertSnapshots(dbList)
        withContext(Dispatchers.Main) {
            val ts = System.currentTimeMillis()
            GlobalValues.snapshotTimestamp = ts
            timestamp.value = ts
        }
    }

    fun computeDiff(context: Context) = viewModelScope.launch(Dispatchers.IO) {
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
                                    SnapshotDiffItem.DiffNode(dbItem.label, it.loadLabel(packageManager).toString()),
                                    SnapshotDiffItem.DiffNode(dbItem.versionName, packageInfo.versionName),
                                    SnapshotDiffItem.DiffNode(dbItem.versionCode, versionCode),
                                    SnapshotDiffItem.DiffNode(dbItem.abi, PackageUtils.getAbi(it.sourceDir, it.nativeLibraryDir).toShort()),
                                    SnapshotDiffItem.DiffNode(dbItem.targetApi, it.targetSdkVersion.toShort()),
                                    SnapshotDiffItem.DiffNode(dbItem.nativeLibs, gson.toJson(
                                        PackageUtils.getNativeDirLibs(it.sourceDir, it.nativeLibraryDir)
                                    )),
                                    SnapshotDiffItem.DiffNode(dbItem.services, gson.toJson(PackageUtils.getComponentList(
                                        packageInfo.packageName,
                                        LibReferenceActivity.Type.TYPE_SERVICE
                                    ))),
                                    SnapshotDiffItem.DiffNode(dbItem.activities, gson.toJson(PackageUtils.getComponentList(
                                        packageInfo.packageName,
                                        LibReferenceActivity.Type.TYPE_ACTIVITY
                                    ))),
                                    SnapshotDiffItem.DiffNode(dbItem.receivers, gson.toJson(PackageUtils.getComponentList(
                                        packageInfo.packageName,
                                        LibReferenceActivity.Type.TYPE_BROADCAST_RECEIVER
                                    ))),
                                    SnapshotDiffItem.DiffNode(dbItem.providers, gson.toJson(PackageUtils.getComponentList(
                                        packageInfo.packageName,
                                        LibReferenceActivity.Type.TYPE_CONTENT_PROVIDER
                                    )))
                                )
                            )
                        }

                        appList.remove(it)
                    } ?: run {
                        diffList.add(
                            SnapshotDiffItem(
                                dbItem.packageName,
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
                } catch (e: PackageManager.NameNotFoundException) {
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
                            SnapshotDiffItem.DiffNode(info.loadLabel(packageManager).toString()),
                            SnapshotDiffItem.DiffNode(packageInfo.versionName),
                            SnapshotDiffItem.DiffNode(versionCode),
                            SnapshotDiffItem.DiffNode(PackageUtils.getAbi(info.sourceDir, info.nativeLibraryDir).toShort()),
                            SnapshotDiffItem.DiffNode(info.targetSdkVersion.toShort()),
                            SnapshotDiffItem.DiffNode(gson.toJson(
                                PackageUtils.getNativeDirLibs(info.sourceDir, info.nativeLibraryDir)
                            )),
                            SnapshotDiffItem.DiffNode(gson.toJson(PackageUtils.getComponentList(
                                packageInfo.packageName,
                                LibReferenceActivity.Type.TYPE_SERVICE
                            ))),
                            SnapshotDiffItem.DiffNode(gson.toJson(PackageUtils.getComponentList(
                                packageInfo.packageName,
                                LibReferenceActivity.Type.TYPE_ACTIVITY
                            ))),
                            SnapshotDiffItem.DiffNode(gson.toJson(PackageUtils.getComponentList(
                                packageInfo.packageName,
                                LibReferenceActivity.Type.TYPE_BROADCAST_RECEIVER
                            ))),
                            SnapshotDiffItem.DiffNode(gson.toJson(PackageUtils.getComponentList(
                                packageInfo.packageName,
                                LibReferenceActivity.Type.TYPE_CONTENT_PROVIDER
                            ))),
                            newInstalled = true
                        )
                    )
                } catch (e: PackageManager.NameNotFoundException) {
                    e.printStackTrace()
                    continue
                }
            }
        }

        withContext(Dispatchers.Main) {
            snapshotDiffItems.value = diffList
        }
    }

    fun insertSnapshots(items: List<SnapshotItem>) = viewModelScope.launch(Dispatchers.IO) {
        repository.insertSnapshots(items)
    }

    fun deleteAllSnapshots() = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteAllSnapshots()
    }
}