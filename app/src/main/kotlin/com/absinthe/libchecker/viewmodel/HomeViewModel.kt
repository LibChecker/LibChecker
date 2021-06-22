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
import com.absinthe.libchecker.LibCheckerApp
import com.absinthe.libchecker.annotation.*
import com.absinthe.libchecker.bean.LibReference
import com.absinthe.libchecker.bean.LibStringItem
import com.absinthe.libchecker.bean.StatefulComponent
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.LibChip
import com.absinthe.libchecker.constant.OnceTag
import com.absinthe.libchecker.constant.librarymap.IconResMap
import com.absinthe.libchecker.database.AppItemRepository
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.database.entity.RuleEntity
import com.absinthe.libchecker.exception.MiuiOpsException
import com.absinthe.libchecker.extensions.valueUnsafe
import com.absinthe.libchecker.protocol.CloudRulesBundle
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libraries.utils.manager.TimeRecorder
import com.absinthe.libraries.utils.utils.XiaomiUtilities
import com.microsoft.appcenter.analytics.Analytics
import jonathanfinerty.once.Once
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.InputStream
import java.util.regex.Pattern

const val GET_INSTALL_APPS_RETRY_PERIOD = 200L

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    val dbItems: LiveData<List<LCItem>>
    val libReference: MutableLiveData<List<LibReference>> = MutableLiveData()
    val reloadAppsFlag = MutableLiveData(false)
    val initProgressLiveData = MutableLiveData(0)
    val appListStatusLiveData = MutableLiveData(STATUS_NOT_START)
    val packageChangedLiveData = MutableLiveData<String?>()
    var hasRequestedChange = false

    private val repository = LibCheckerApp.repository

    init {
        dbItems = repository.allDatabaseItems
    }

    fun initItems() = viewModelScope.launch(Dispatchers.IO) {
        Timber.d("initItems: START")

        val context: Context = getApplication<LibCheckerApp>()
        val timeRecorder = TimeRecorder()
        timeRecorder.start()

        appListStatusLiveData.postValue(STATUS_START)
        repository.deleteAllItems()
        initProgressLiveData.postValue(0)

        var appList: List<ApplicationInfo>?
        var appNumbers = 0

        do {
            appList = try {
                PackageUtils.getInstallApplications()
            } catch (e: MiuiOpsException) {
                emptyList()
            } catch (e: Exception) {
                Timber.w(e)
                delay(GET_INSTALL_APPS_RETRY_PERIOD)
                null
            }?.apply {
                AppItemRepository.allApplicationInfoItems.postValue(this)
                appNumbers = this.size
            }
        } while (appList == null)

        val lcItems = mutableListOf<LCItem>()
        var packageInfo: PackageInfo
        var versionCode: Long
        var abiType: Int
        var isSystemType: Boolean
        var isKotlinType: Boolean

        var lcItem: LCItem
        var count = 0
        var progressCount = 0

        for (info in appList) {
            try {
                packageInfo = PackageUtils.getPackageInfo(info)
                versionCode = PackageUtils.getVersionCode(packageInfo)
                abiType = PackageUtils.getAbi(info)
                isSystemType = (info.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM
                isKotlinType = PackageUtils.isKotlinUsed(packageInfo)

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
                    isKotlinType,
                    packageInfo.applicationInfo.targetSdkVersion.toShort()
                )

                lcItems.add(lcItem)
                count++
                progressCount++
                initProgressLiveData.postValue(progressCount * 100 / appNumbers)
            } catch (e: Throwable) {
                Timber.e(e, "initItems")
                continue
            }

            if (count == 50) {
                insert(lcItems)
                lcItems.clear()
                count = 0
            }
        }

        insert(lcItems)
        lcItems.clear()
        appListStatusLiveData.postValue(STATUS_END)

        timeRecorder.end()
        Timber.d("initItems: END, $timeRecorder")
        appListStatusLiveData.postValue(STATUS_NOT_START)
    }

    fun requestChange(needRefresh: Boolean = false) = viewModelScope.launch(Dispatchers.IO) {
        if (appListStatusLiveData.value == STATUS_START) {
            Timber.d("Request change appListStatusLiveData not equals STATUS_START")
            return@launch
        }
        if (XiaomiUtilities.isMIUI() && !XiaomiUtilities.isCustomPermissionGranted(XiaomiUtilities.OP_GET_INSTALLED_APPS)) {
            Timber.d("Request change OP_GET_INSTALLED_APPS returns")
            return@launch
        }

        requestChangeImpl(LibCheckerApp.context.packageManager, needRefresh)
    }

    private suspend fun requestChangeImpl(packageManager: PackageManager, needRefresh: Boolean = false) {
        Timber.d("Request change: START")
        val timeRecorder = TimeRecorder()
        var appList: MutableList<ApplicationInfo>? = AppItemRepository.allApplicationInfoItems.value?.toMutableList()

        timeRecorder.start()
        appListStatusLiveData.postValue(STATUS_START)

        if (appList.isNullOrEmpty() || needRefresh) {
            do {
                appList = try {
                    PackageUtils.getInstallApplications().toMutableList()
                } catch (e: MiuiOpsException) {
                    mutableListOf()
                } catch (e: Exception) {
                    Timber.w(e)
                    delay(GET_INSTALL_APPS_RETRY_PERIOD)
                    null
                }
            } while (appList == null)
        }

        dbItems.value?.let { value ->
            var packageInfo: PackageInfo
            var versionCode: Long
            var lcItem: LCItem
            var abi: Int

            for (dbItem in value) {
                try {
                    appList.find { it.packageName == dbItem.packageName }?.let {
                        packageInfo = PackageUtils.getPackageInfo(it)
                        versionCode = PackageUtils.getVersionCode(packageInfo)

                        if (packageInfo.lastUpdateTime != dbItem.lastUpdatedTime
                            || (dbItem.lastUpdatedTime == 0L && versionCode != dbItem.versionCode)) {
                            abi = PackageUtils.getAbi(it)
                            lcItem = LCItem(
                                it.packageName,
                                it.loadLabel(packageManager).toString(),
                                packageInfo.versionName ?: "null",
                                versionCode,
                                packageInfo.firstInstallTime,
                                packageInfo.lastUpdateTime,
                                (it.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM,
                                abi.toShort(),
                                PackageUtils.isSplitsApk(packageInfo),
                                PackageUtils.isKotlinUsed(packageInfo),
                                packageInfo.applicationInfo.targetSdkVersion.toShort()
                            )
                            update(lcItem)
                        }

                        appList.remove(it)
                    } ?: run {
                        delete(dbItem)
                    }
                } catch (e: Exception) {
                    Timber.e(e)
                    continue
                }
            }

            for (info in appList) {
                try {
                    packageInfo = PackageUtils.getPackageInfo(info)
                    versionCode = PackageUtils.getVersionCode(packageInfo)

                    lcItem = LCItem(
                        info.packageName,
                        info.loadLabel(packageManager).toString(),
                        packageInfo.versionName ?: "null",
                        versionCode,
                        packageInfo.firstInstallTime,
                        packageInfo.lastUpdateTime,
                        (info.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM,
                        PackageUtils.getAbi(info).toShort(),
                        PackageUtils.isSplitsApk(packageInfo),
                        PackageUtils.isKotlinUsed(packageInfo),
                        packageInfo.applicationInfo.targetSdkVersion.toShort()
                    )
                    abi = PackageUtils.getAbi(info)
                    lcItem = LCItem(
                        info.packageName,
                        info.loadLabel(packageManager).toString(),
                        packageInfo.versionName ?: "null",
                        versionCode,
                        packageInfo.firstInstallTime,
                        packageInfo.lastUpdateTime,
                        (info.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM,
                        abi.toShort(),
                        PackageUtils.isSplitsApk(packageInfo),
                        PackageUtils.isKotlinUsed(packageInfo),
                        packageInfo.applicationInfo.targetSdkVersion.toShort()
                    )

                    insert(lcItem)
                } catch (e: Exception) {
                    Timber.e(e)
                    continue
                }
            }
            GlobalValues.shouldRequestChange.postValue(false)
            AppItemRepository.shouldRefreshAppList = true
        } ?: run {
            GlobalValues.shouldRequestChange.postValue(true)
        }

        appListStatusLiveData.postValue(STATUS_END)
        timeRecorder.end()
        Timber.d("Request change: END, $timeRecorder")
        appListStatusLiveData.postValue(STATUS_NOT_START)

        if (!Once.beenDone(Once.THIS_APP_VERSION, OnceTag.HAS_COLLECT_LIB)) {
            delay(10000)
            collectPopularLibraries(appList.toList())
            Once.markDone(OnceTag.HAS_COLLECT_LIB)
        }
    }

    private fun collectPopularLibraries(appList: List<ApplicationInfo>) = viewModelScope.launch(Dispatchers.IO) {
        val map = HashMap<String, Int>()
        var libList: List<LibStringItem>
        var count: Int

        try {
            for (item in appList) {
                libList = PackageUtils.getNativeDirLibs(PackageUtils.getPackageInfo(item.packageName))

                for (lib in libList) {
                    count = map[lib.name] ?: 0
                    map[lib.name] = count + 1
                }
            }
            val properties: MutableMap<String, String> = HashMap()

            for (entry in map) {
                if (entry.value > 3 && LCAppUtils.getRuleWithRegex(entry.key, NATIVE) == null) {
                    properties.clear()
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
        } catch (ignore: Exception) {
            Timber.e(ignore, "collectPopularLibraries failed")
        }
    }

    private suspend fun collectComponentPopularLibraries(
        appList: List<ApplicationInfo>,
        @LibType type: Int,
        label: String
    ) {
        val map = HashMap<String, Int>()
        var compLibList: List<StatefulComponent>
        var count: Int

        for (item in appList) {
            try {
                compLibList = PackageUtils.getComponentList(item.packageName, type, false)

                for (lib in compLibList) {
                    count = map[lib.componentName] ?: 0
                    map[lib.componentName] = count + 1
                }
            } catch (e: Exception) {
                Timber.e(e)
                continue
            }
        }

        val properties: MutableMap<String, String> = HashMap()

        for (entry in map) {
            if (entry.value > 3 && LCAppUtils.getRuleWithRegex(entry.key, type) == null) {
                properties.clear()
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

    private var computeLibReferenceJob: Job? = null

    fun computeLibReference(@LibType type: Int) {
        computeLibReferenceJob = viewModelScope.launch(Dispatchers.IO) {
            libReference.postValue(null)
            var appList: List<ApplicationInfo>? = AppItemRepository.allApplicationInfoItems.value

            if (appList.isNullOrEmpty()) {
                do {
                    appList = try {
                        PackageUtils.getInstallApplications()
                    } catch (e: MiuiOpsException) {
                        Timber.e(e)
                        emptyList()
                    } catch (e: Exception) {
                        Timber.e(e)
                        delay(GET_INSTALL_APPS_RETRY_PERIOD)
                        null
                    }
                } while (appList == null)
            }

            val map = HashMap<String, RefCountType>()
            val refList = mutableListOf<LibReference>()
            val showSystem = GlobalValues.isShowSystemApps.value ?: false

            var libList: List<LibStringItem>
            var packageInfo: PackageInfo
            var count: Int
            var onlyShowNotMarked = false

            when (type) {
                ALL, NOT_MARKED -> {
                    if (type == NOT_MARKED) {
                        onlyShowNotMarked = true
                    }
                    for (item in appList) {

                        if (!showSystem && ((item.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM)) {
                            continue
                        }

                        try {
                            packageInfo = PackageUtils.getPackageInfo(item.packageName)
                            libList = PackageUtils.getNativeDirLibs(packageInfo)

                            for (lib in libList) {
                                count = map[lib.name]?.count ?: 0
                                map[lib.name] = RefCountType(count + 1, NATIVE)
                            }
                        } catch (e: Exception) {
                            Timber.e(e)
                        }

                        try {
                            packageInfo = PackageUtils.getPackageInfo(item.packageName, PackageManager.GET_SERVICES)
                            packageInfo.services?.let {
                                for (service in it) {
                                    count = map[service.name]?.count ?: 0
                                    map[service.name] = RefCountType(count + 1, SERVICE)
                                }
                            }

                            packageInfo = PackageUtils.getPackageInfo(item.packageName, PackageManager.GET_ACTIVITIES)
                            packageInfo.activities?.let {
                                for (activity in it) {
                                    count = map[activity.name]?.count ?: 0
                                    map[activity.name] = RefCountType(count + 1, ACTIVITY)
                                }
                            }

                            packageInfo = PackageUtils.getPackageInfo(item.packageName, PackageManager.GET_RECEIVERS)
                            packageInfo.receivers?.let {
                                for (receiver in it) {
                                    count = map[receiver.name]?.count ?: 0
                                    map[receiver.name] = RefCountType(count + 1, RECEIVER)
                                }
                            }

                            packageInfo = PackageUtils.getPackageInfo(item.packageName, PackageManager.GET_PROVIDERS)
                            packageInfo.providers?.let {
                                for (provider in it) {
                                    count = map[provider.name]?.count ?: 0
                                    map[provider.name] = RefCountType(count + 1, PROVIDER)
                                }
                            }
                        } catch (e: Exception) {
                            Timber.e(e)
                        }
                    }
                }
                NATIVE -> {
                    for (item in appList) {

                        if (!showSystem && ((item.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM)) {
                            continue
                        }

                        try {
                            packageInfo = PackageUtils.getPackageInfo(item.packageName)
                            libList = PackageUtils.getNativeDirLibs(packageInfo)

                            for (lib in libList) {
                                count = map[lib.name]?.count ?: 0
                                map[lib.name] = RefCountType(count + 1, NATIVE)
                            }
                        } catch (e: Exception) {
                            Timber.e(e)
                        }
                    }
                }
                SERVICE -> {
                    for (item in appList) {

                        if (!showSystem && ((item.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM)) {
                            continue
                        }

                        try {
                            packageInfo = PackageUtils.getPackageInfo(item.packageName, PackageManager.GET_SERVICES)
                            packageInfo.services?.let {
                                for (service in it) {
                                    count = map[service.name]?.count ?: 0
                                    map[service.name] = RefCountType(count + 1, SERVICE)
                                }
                            }
                        } catch (e: Exception) {
                            Timber.e(e)
                        }
                    }
                }
                ACTIVITY -> {
                    for (item in appList) {

                        if (!showSystem && ((item.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM)) {
                            continue
                        }

                        try {
                            packageInfo = PackageUtils.getPackageInfo(item.packageName, PackageManager.GET_ACTIVITIES)
                            packageInfo.activities?.let {
                                for (activity in it) {
                                    count = map[activity.name]?.count ?: 0
                                    map[activity.name] = RefCountType(count + 1, ACTIVITY)
                                }
                            }
                        } catch (e: Exception) {
                            Timber.e(e)
                        }
                    }
                }
                RECEIVER -> {
                    for (item in appList) {

                        if (!showSystem && ((item.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM)) {
                            continue
                        }

                        try {
                            packageInfo = PackageUtils.getPackageInfo(item.packageName, PackageManager.GET_RECEIVERS)
                            packageInfo.receivers?.let {
                                for (receiver in it) {
                                    count = map[receiver.name]?.count ?: 0
                                    map[receiver.name] = RefCountType(count + 1, RECEIVER)
                                }
                            }
                        } catch (e: Exception) {
                            Timber.e(e)
                        }
                    }
                }
                PROVIDER -> {
                    for (item in appList) {

                        if (!showSystem && ((item.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM)) {
                            continue
                        }

                        try {
                            packageInfo = PackageUtils.getPackageInfo(item.packageName, PackageManager.GET_PROVIDERS)
                            packageInfo.providers?.let {
                                for (provider in it) {
                                    count = map[provider.name]?.count ?: 0
                                    map[provider.name] = RefCountType(count + 1, PROVIDER)
                                }
                            }
                        } catch (e: Exception) {
                            Timber.e(e)
                        }
                    }
                }
                DEX -> {
                    for (item in appList) {

                        if (!showSystem && ((item.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM)) {
                            continue
                        }

                        libList = PackageUtils.getDexList(item.packageName)

                        for (lib in libList) {
                            count = map[lib.name]?.count ?: 0
                            map[lib.name] = RefCountType(count + 1, DEX)
                        }
                    }
                }
            }

            var chip: LibChip?
            var rule: RuleEntity?
            for (entry in map) {
                if (entry.value.count >= GlobalValues.libReferenceThreshold.valueUnsafe && entry.key.isNotBlank()) {
                    rule = LCAppUtils.getRuleWithRegex(entry.key, entry.value.type)
                    chip = null
                    rule?.let {
                        chip = LibChip(iconRes = IconResMap.getIconRes(it.iconIndex), name = it.label, regexName = it.regexName)
                    }
                    if (!onlyShowNotMarked) {
                        refList.add(LibReference(entry.key, chip, entry.value.count, entry.value.type))
                    } else {
                        if (rule == null) {
                            refList.add(LibReference(entry.key, chip, entry.value.count, entry.value.type))
                        }
                    }
                }
            }

            refList.sortByDescending { it.referredCount }
            libReference.postValue(refList)
        }
    }

    fun cancelComputingLibReference() {
        computeLibReferenceJob?.cancel()
        computeLibReferenceJob = null
    }

    fun refreshRef() {
        libReference.value?.let { ref ->
            libReference.value = ref.filter { it.referredCount >= GlobalValues.libReferenceThreshold.valueUnsafe }
        }
    }

    private suspend fun insert(item: LCItem) = repository.insert(item)

    private suspend fun insert(list: List<LCItem>) = repository.insert(list)

    private suspend fun update(item: LCItem) = repository.update(item)

    private suspend fun delete(item: LCItem) = repository.delete(item)

    private fun deleteAllRules() = viewModelScope.launch(Dispatchers.IO) { repository.getAllRules() }

    fun insertPreinstallRules(context: Context) = viewModelScope.launch(Dispatchers.IO) {
        deleteAllRules()
        insertPreinstallRules(context, 1)
        insertPreinstallRules(context, 2)
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun insertPreinstallRules(context: Context, bundleCount: Int) {
        withContext(Dispatchers.IO) {
            var inputStream: InputStream? = null
            try {
                inputStream = context.resources.assets.open("rules.lcr.$bundleCount")
                val rulesBundle = CloudRulesBundle.parseFrom(inputStream)
                val rulesList = mutableListOf<RuleEntity>()
                rulesBundle.rulesList.cloudRulesList.forEach {
                    it?.let {
                        rulesList.add(RuleEntity(it.name, it.label, it.type, it.iconIndex, it.isRegexRule, it.regexName))
                    }
                }
                repository.insertRules(rulesList)
                GlobalValues.localRulesVersion = rulesBundle.version
            } catch (e: Exception) {
                Timber.e(e)
            } finally {
                inputStream?.close()
            }
        }
    }

    fun initRegexRules() = viewModelScope.launch(Dispatchers.IO) {
        val list = repository.getRegexRules()
        list.forEach {
            AppItemRepository.rulesRegexList[Pattern.compile(it.name)] = it
        }
    }
}