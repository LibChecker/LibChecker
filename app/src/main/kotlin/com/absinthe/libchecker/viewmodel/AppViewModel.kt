package com.absinthe.libchecker.viewmodel

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.bean.AppItem
import com.absinthe.libchecker.bean.LibReference
import com.absinthe.libchecker.bean.LibStringItem
import com.absinthe.libchecker.constant.*
import com.absinthe.libchecker.constant.librarymap.BaseMap
import com.absinthe.libchecker.constant.librarymap.NativeLibMap
import com.absinthe.libchecker.database.AppItemRepository
import com.absinthe.libchecker.database.LCDatabase
import com.absinthe.libchecker.database.LCItem
import com.absinthe.libchecker.database.LCRepository
import com.absinthe.libchecker.ktx.logd
import com.absinthe.libchecker.utils.PackageUtils
import com.blankj.utilcode.util.AppUtils
import com.microsoft.appcenter.analytics.Analytics
import jonathanfinerty.once.Once
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModel(application: Application) : AndroidViewModel(application) {

    val dbItems: LiveData<List<LCItem>>
    val libReference: MutableLiveData<List<LibReference>> = MutableLiveData()
    val clickBottomItemFlag: MutableLiveData<Boolean> = MutableLiveData(false)
    var refreshLock = false
    var isInit = false

    private val repository: LCRepository

    init {
        val lcDao = LCDatabase.getDatabase(application).lcDao()
        repository = LCRepository(lcDao)
        dbItems = repository.allItems
    }

    fun initItems(context: Context) = viewModelScope.launch(Dispatchers.IO) {
        logd("initItems")

        repository.deleteAllItems()

        var appList: List<ApplicationInfo>? = null
        while (appList == null) {
            appList = try {
                PackageUtils.getInstallApplications()
            } catch (e: Exception) {
                null
            }
        }

        val newItems = ArrayList<AppItem>()
        var packageInfo: PackageInfo
        var versionCode: Long
        var abiType: Int
        var isSystemType: Boolean
        var isKotlinType: Boolean

        var appItem: AppItem
        var lcItem: LCItem

        for (info in appList) {
            try {
                packageInfo = PackageUtils.getPackageInfo(info)
                versionCode = PackageUtils.getVersionCode(packageInfo)
                abiType = PackageUtils.getAbi(info.sourceDir, info.nativeLibraryDir)
                isSystemType =
                    (info.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM
                isKotlinType = PackageUtils.isKotlinUsed(packageInfo)

                appItem = AppItem().apply {
                    icon = info.loadIcon(context.packageManager)
                    appName = info.loadLabel(context.packageManager).toString()
                    packageName = info.packageName
                    versionName = PackageUtils.getVersionString(packageInfo)
                    abi = abiType
                    isSystem = isSystemType
                    updateTime = packageInfo.lastUpdateTime
                    isKotlinUsed = isKotlinType
                }
                lcItem = LCItem(
                    info.packageName,
                    info.loadLabel(context.packageManager).toString(),
                    packageInfo.versionName ?: "",
                    versionCode,
                    packageInfo.firstInstallTime,
                    packageInfo.lastUpdateTime,
                    isSystemType,
                    abiType.toShort(),
                    PackageUtils.isSplitsApk(packageInfo),
                    isKotlinType
                )

                GlobalValues.isShowSystemApps.value?.let {
                    if (it || (!it && !lcItem.isSystem)) {
                        newItems.add(appItem)
                    }
                }

                insert(lcItem)
            } catch (e: Exception) {
                e.printStackTrace()
                continue
            } catch (e: VerifyError) {
                continue
            }
        }

        //Sort
        when (GlobalValues.appSortMode.value) {
            Constants.SORT_MODE_DEFAULT -> newItems.sortWith(compareBy({ it.abi }, { it.appName }))
            Constants.SORT_MODE_UPDATE_TIME_DESC -> newItems.sortByDescending { it.updateTime }
        }

        withContext(Dispatchers.Main) {
            GlobalValues.isObservingDBItems.value = true
            AppItemRepository.allItems.value = newItems
        }
    }

    fun addItem() = viewModelScope.launch(Dispatchers.IO) {
        logd("addItems")

        dbItems.value?.let { value ->
            val newItems = ArrayList<AppItem>()
            var appItem: AppItem

            for (item in value) {
                appItem = AppItem().apply {
                    icon = AppUtils.getAppIcon(item.packageName)
                        ?: ColorDrawable(Color.TRANSPARENT)
                    appName = item.label
                    packageName = item.packageName
                    versionName = PackageUtils.getVersionString(item.versionName, item.versionCode)
                    abi = item.abi.toInt()
                    isSystem = item.isSystem
                    updateTime = item.lastUpdatedTime
                    isKotlinUsed = item.isKotlinUsed
                }

                GlobalValues.isShowSystemApps.value?.let {
                    if (it || (!it && !item.isSystem)) {
                        newItems.add(appItem)
                    }
                }
            }

            when (GlobalValues.appSortMode.value) {
                Constants.SORT_MODE_DEFAULT -> newItems.sortWith(
                    compareBy(
                        { it.abi },
                        { it.appName })
                )
                Constants.SORT_MODE_UPDATE_TIME_DESC -> newItems.sortByDescending { it.updateTime }
            }

            withContext(Dispatchers.Main) {
                AppItemRepository.allItems.value = newItems
            }

            refreshLock = false
        }
    }

    fun requestChange(context: Context) = viewModelScope.launch(Dispatchers.IO) {
        logd("requestChange")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestChangeImpl(context)
        } else {
            try {
                requestChangeImpl(context)
            } catch (e: VerifyError) {
                e.printStackTrace()
            }
        }
        withContext(Dispatchers.Main) {
            GlobalValues.shouldRequestChange.value = false
        }
    }

    private fun requestChangeImpl(context: Context) {
        var appList: MutableList<ApplicationInfo>? = null
        while (appList == null) {
            appList = try {
                PackageUtils.getInstallApplications().toMutableList()
            } catch (e: Exception) {
                null
            }
        }

        if (!Once.beenDone(Once.THIS_APP_VERSION, OnceTag.HAS_COLLECT_LIB)) {
            collectPopularLibraries(appList)
            Once.markDone(OnceTag.HAS_COLLECT_LIB)
        }

        dbItems.value?.let { value ->
            var packageInfo: PackageInfo
            var versionCode: Long
            var lcItem: LCItem

            for (dbItem in value) {
                try {
                    appList.find { it.packageName == dbItem.packageName }?.let {
                        packageInfo = PackageUtils.getPackageInfo(it)
                        versionCode = PackageUtils.getVersionCode(packageInfo)

                        if (packageInfo.lastUpdateTime != dbItem.lastUpdatedTime) {
                            lcItem = LCItem(
                                it.packageName,
                                it.loadLabel(context.packageManager).toString(),
                                packageInfo.versionName ?: "null",
                                versionCode,
                                packageInfo.firstInstallTime,
                                packageInfo.lastUpdateTime,
                                (it.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM,
                                PackageUtils.getAbi(it.sourceDir, it.nativeLibraryDir).toShort(),
                                PackageUtils.isSplitsApk(packageInfo),
                                PackageUtils.isKotlinUsed(packageInfo)
                            )
                            update(lcItem)
                        }

                        appList.remove(it)
                    } ?: run {
                        delete(dbItem)
                    }
                } catch (e: Exception) {
                    continue
                }
            }

            for (info in appList) {
                try {
                    packageInfo = PackageUtils.getPackageInfo(info)
                    versionCode = PackageUtils.getVersionCode(packageInfo)

                    lcItem = LCItem(
                        info.packageName,
                        info.loadLabel(context.packageManager).toString(),
                        packageInfo.versionName ?: "null",
                        versionCode,
                        packageInfo.firstInstallTime,
                        packageInfo.lastUpdateTime,
                        (info.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM,
                        PackageUtils.getAbi(info.sourceDir, info.nativeLibraryDir).toShort(),
                        PackageUtils.isSplitsApk(packageInfo),
                        PackageUtils.isKotlinUsed(packageInfo)
                    )

                    insert(lcItem)
                } catch (e: Exception) {
                    continue
                }
            }
        }
    }

    private fun collectPopularLibraries(appList: List<ApplicationInfo>) = viewModelScope.launch(Dispatchers.IO) {
        val map = HashMap<String, Int>()
        var libList: List<LibStringItem>
        var count: Int

        try {
            for (item in appList) {
                libList = PackageUtils.getNativeDirLibs(
                    item.sourceDir,
                    item.nativeLibraryDir
                )

                for (lib in libList) {
                    count = map[lib.name] ?: 0
                    map[lib.name] = count + 1
                }
            }

            for (entry in map) {
                if (entry.value > 3 && !NativeLibMap.getMap().containsKey(entry.key)) {
                    val properties: MutableMap<String, String> = java.util.HashMap()
                    properties["Library name"] = entry.key
                    properties["Library count"] = entry.value.toString()

                    Analytics.trackEvent("Native Library", properties)
                }
            }

            collectComponentPopularLibraries(
                appList,
                SERVICE,
                "Service"
            )
            collectComponentPopularLibraries(
                appList,
                ACTIVITY,
                "Activity"
            )
            collectComponentPopularLibraries(
                appList,
                RECEIVER,
                "Receiver"
            )
            collectComponentPopularLibraries(
                appList,
                PROVIDER,
                "Provider"
            )
        } catch (ignore: Exception) {}
    }

    private fun collectComponentPopularLibraries(
        appList: List<ApplicationInfo>,
        @LibType type: Int,
        label: String
    ) {
        val map = HashMap<String, Int>()
        var compLibList: List<String>
        var count: Int

        for (item in appList) {
            try {
                compLibList = PackageUtils.getComponentList(item.packageName, type, false)

                for (lib in compLibList) {
                    count = map[lib] ?: 0
                    map[lib] = count + 1
                }
            } catch (e: Exception) {
                continue
            }
        }

        val libMap = BaseMap.getMap(type)
        for (entry in map) {
            if (entry.value > 3 && !libMap.getMap()
                    .containsKey(entry.key) && libMap.findRegex(entry.key) == null
            ) {
                val properties: MutableMap<String, String> = java.util.HashMap()
                properties["Library name"] = entry.key
                properties["Library count"] = entry.value.toString()

                Analytics.trackEvent("$label Library", properties)
            }
        }
    }

    data class RefCountType(
        val count: Int,
        @LibType val type: Int
    )

    fun computeLibReference(context: Context, @LibType type: Int) =
        viewModelScope.launch(Dispatchers.IO) {
            var appList: List<ApplicationInfo>? = null
            while (appList == null) {
                appList = try {
                    PackageUtils.getInstallApplications()
                } catch (e: Exception) {
                    null
                }
            }

            val map = HashMap<String, RefCountType>()
            val refList = mutableListOf<LibReference>()
            val showSystem = GlobalValues.isShowSystemApps.value ?: false

            var libList: List<LibStringItem>
            var packageInfo: PackageInfo
            var count: Int

            when (type) {
                ALL -> {
                    for (item in appList) {

                        if (!showSystem && ((item.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM)) {
                            continue
                        }

                        libList = PackageUtils.getNativeDirLibs(
                            item.sourceDir,
                            item.nativeLibraryDir
                        )

                        for (lib in libList) {
                            count = map[lib.name]?.count ?: 0
                            map[lib.name] =
                                RefCountType(count + 1, NATIVE)
                        }

                        try {
                            packageInfo =
                                context.packageManager.getPackageInfo(
                                    item.packageName,
                                    PackageManager.GET_SERVICES
                                )

                            packageInfo.services?.let {
                                for (service in it) {
                                    count = map[service.name]?.count ?: 0
                                    map[service.name] = RefCountType(count + 1, SERVICE)
                                }
                            }

                            packageInfo =
                                context.packageManager.getPackageInfo(
                                    item.packageName,
                                    PackageManager.GET_ACTIVITIES
                                )
                            packageInfo.activities?.let {
                                for (activity in it) {
                                    count = map[activity.name]?.count ?: 0
                                    map[activity.name] = RefCountType(count + 1, ACTIVITY)
                                }
                            }

                            packageInfo =
                                context.packageManager.getPackageInfo(
                                    item.packageName,
                                    PackageManager.GET_RECEIVERS
                                )
                            packageInfo.receivers?.let {
                                for (receiver in it) {
                                    count = map[receiver.name]?.count ?: 0
                                    map[receiver.name] = RefCountType(count + 1, RECEIVER)
                                }
                            }

                            packageInfo =
                                context.packageManager.getPackageInfo(
                                    item.packageName,
                                    PackageManager.GET_PROVIDERS
                                )
                            packageInfo.providers?.let {
                                for (provider in it) {
                                    count = map[provider.name]?.count ?: 0
                                    map[provider.name] = RefCountType(count + 1, PROVIDER)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                NATIVE -> {
                    for (item in appList) {

                        if (!showSystem && ((item.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM)) {
                            continue
                        }

                        libList = PackageUtils.getNativeDirLibs(
                            item.sourceDir,
                            item.nativeLibraryDir
                        )

                        for (lib in libList) {
                            count = map[lib.name]?.count ?: 0
                            map[lib.name] =
                                RefCountType(count + 1, NATIVE)
                        }
                    }
                }
                SERVICE -> {
                    for (item in appList) {

                        if (!showSystem && ((item.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM)) {
                            continue
                        }

                        try {
                            packageInfo =
                                context.packageManager.getPackageInfo(
                                    item.packageName,
                                    PackageManager.GET_SERVICES
                                )

                            packageInfo.services?.let {
                                for (service in it) {
                                    count = map[service.name]?.count ?: 0
                                    map[service.name] = RefCountType(count + 1, SERVICE)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                ACTIVITY -> {
                    for (item in appList) {

                        if (!showSystem && ((item.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM)) {
                            continue
                        }

                        try {
                            packageInfo =
                                context.packageManager.getPackageInfo(
                                    item.packageName,
                                    PackageManager.GET_ACTIVITIES
                                )
                            packageInfo.activities?.let {
                                for (activity in it) {
                                    count = map[activity.name]?.count ?: 0
                                    map[activity.name] = RefCountType(count + 1, ACTIVITY)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                RECEIVER -> {
                    for (item in appList) {

                        if (!showSystem && ((item.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM)) {
                            continue
                        }

                        try {
                            packageInfo =
                                context.packageManager.getPackageInfo(
                                    item.packageName,
                                    PackageManager.GET_RECEIVERS
                                )
                            packageInfo.receivers?.let {
                                for (receiver in it) {
                                    count = map[receiver.name]?.count ?: 0
                                    map[receiver.name] = RefCountType(count + 1, RECEIVER)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                PROVIDER -> {
                    for (item in appList) {

                        if (!showSystem && ((item.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM)) {
                            continue
                        }

                        try {
                            packageInfo =
                                context.packageManager.getPackageInfo(
                                    item.packageName,
                                    PackageManager.GET_PROVIDERS
                                )
                            packageInfo.providers?.let {
                                for (provider in it) {
                                    count = map[provider.name]?.count ?: 0
                                    map[provider.name] = RefCountType(count + 1, PROVIDER)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            for (entry in map) {
                if (entry.value.count >= GlobalValues.libReferenceThreshold.value!! && entry.key.isNotBlank()) {

                    val chip = BaseMap.getMap(entry.value.type).getChip(entry.key)
                    refList.add(
                        LibReference(entry.key, chip, entry.value.count, entry.value.type)
                    )
                }
            }

            refList.sortByDescending { it.referredCount }

            withContext(Dispatchers.Main) {
                libReference.value = refList
            }
        }

    fun refreshRef() {
        libReference.value?.let { ref ->
            libReference.value =
                ref.filter { it.referredCount >= GlobalValues.libReferenceThreshold.value!! }
        }
    }

    private fun insert(item: LCItem) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(item)
    }

    private fun update(item: LCItem) = viewModelScope.launch(Dispatchers.IO) {
        repository.update(item)
    }

    private fun delete(item: LCItem) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(item)
    }
}