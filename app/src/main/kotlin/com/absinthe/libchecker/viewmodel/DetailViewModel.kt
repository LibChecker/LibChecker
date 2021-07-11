package com.absinthe.libchecker.viewmodel

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.util.SparseArray
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.LibCheckerApp
import com.absinthe.libchecker.annotation.*
import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.api.bean.LibDetailBean
import com.absinthe.libchecker.api.request.LibDetailRequest
import com.absinthe.libchecker.bean.LibStringItemChip
import com.absinthe.libchecker.bean.StatefulComponent
import com.absinthe.libchecker.compat.VersionCompat
import com.absinthe.libchecker.constant.AbilityType
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.LibChip
import com.absinthe.libchecker.constant.librarymap.IconResMap
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.ui.fragment.detail.LocatedCount
import com.absinthe.libchecker.ui.fragment.detail.MODE_SORT_BY_SIZE
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.harmony.ApplicationDelegate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ohos.bundle.AbilityInfo
import ohos.bundle.IBundleManager
import timber.log.Timber

class DetailViewModel(application: Application) : AndroidViewModel(application) {

    val detailBean: MutableLiveData<LibDetailBean?> = MutableLiveData()

    val nativeLibItems: MutableLiveData<List<LibStringItemChip>> = MutableLiveData()
    val staticLibItems: MutableLiveData<List<LibStringItemChip>> = MutableLiveData()
    val dexLibItems: MutableLiveData<List<LibStringItemChip>> = MutableLiveData()
    val componentsMap = SparseArray<MutableLiveData<List<StatefulComponent>>>()
    val abilitiesMap = SparseArray<MutableLiveData<List<StatefulComponent>>>()
    val itemsCountLiveData: MutableLiveData<LocatedCount> = MutableLiveData(LocatedCount(0, 0))
    val itemsCountList = MutableList(7) { 0 }
    var sortMode = GlobalValues.libSortMode.value ?: MODE_SORT_BY_SIZE
    var packageName: String = ""
    var is32bit = false
    var isApk = false

    init {
        componentsMap.put(SERVICE, MutableLiveData())
        componentsMap.put(ACTIVITY, MutableLiveData())
        componentsMap.put(RECEIVER, MutableLiveData())
        componentsMap.put(PROVIDER, MutableLiveData())
    }

    fun initSoAnalysisData(packageName: String) = viewModelScope.launch(Dispatchers.IO) {
        val context: Context = getApplication<LibCheckerApp>()
        val list = ArrayList<LibStringItemChip>()

        try {
            val isApk = packageName.endsWith("/temp.apk")
            val info = if (isApk) {
                context.packageManager.getPackageArchiveInfo(
                    packageName,
                    0
                )?.applicationInfo?.apply {
                    sourceDir = packageName
                    publicSourceDir = packageName
                }
            } else {
                context.packageManager.getApplicationInfo(packageName, 0)
            }

            info?.let {
                list.addAll(
                    getNativeChipList(info, is32bit, isApk)
                )
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Timber.e(e)
        }

        nativeLibItems.postValue(list)
    }

    fun initStaticData(packageName: String) = viewModelScope.launch(Dispatchers.IO) {
        staticLibItems.postValue(getStaticChipList(packageName))
    }

    fun initDexData(packageName: String) = viewModelScope.launch(Dispatchers.IO) {
        dexLibItems.postValue(getDexChipList(packageName))
    }

    fun initComponentsData(packageName: String) = viewModelScope.launch(Dispatchers.IO) {
            val context: Context = getApplication<LibCheckerApp>()

            try {
                if (packageName.endsWith("/temp.apk")) {
                    context.packageManager.getPackageArchiveInfo(
                        packageName,
                        PackageManager.GET_SERVICES
                                or PackageManager.GET_ACTIVITIES
                                or PackageManager.GET_RECEIVERS
                                or PackageManager.GET_PROVIDERS
                                or VersionCompat.MATCH_DISABLED_COMPONENTS
                    )?.apply {
                        applicationInfo.sourceDir = packageName
                        applicationInfo.publicSourceDir = packageName
                    }?.let {
                        val services = PackageUtils.getComponentList(it.packageName, it.services, true)
                        val activities = PackageUtils.getComponentList(it.packageName, it.activities, true)
                        val receivers = PackageUtils.getComponentList(it.packageName, it.receivers, true)
                        val providers = PackageUtils.getComponentList(it.packageName, it.providers, true)

                        componentsMap[SERVICE]?.postValue(services)
                        componentsMap[ACTIVITY]?.postValue(activities)
                        componentsMap[RECEIVER]?.postValue(receivers)
                        componentsMap[PROVIDER]?.postValue(providers)
                    }
                } else {
                    PackageUtils.getPackageInfo(packageName).let {
                        val services = PackageUtils.getComponentList(it.packageName, SERVICE, true)
                        val activities = PackageUtils.getComponentList(it.packageName, ACTIVITY, true)
                        val receivers = PackageUtils.getComponentList(it.packageName, RECEIVER, true)
                        val providers = PackageUtils.getComponentList(it.packageName, PROVIDER, true)

                        componentsMap[SERVICE]?.postValue(services)
                        componentsMap[ACTIVITY]?.postValue(activities)
                        componentsMap[RECEIVER]?.postValue(receivers)
                        componentsMap[PROVIDER]?.postValue(providers)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }

    private val request: LibDetailRequest = ApiManager.create()

    fun requestLibDetail(libName: String, @LibType type: Int, isRegex: Boolean = false) =
        viewModelScope.launch(Dispatchers.IO) {
            Timber.d("requestLibDetail")
            var categoryDir = when (type) {
                NATIVE -> "native-libs"
                SERVICE -> "services-libs"
                ACTIVITY -> "activities-libs"
                RECEIVER -> "receivers-libs"
                PROVIDER -> "providers-libs"
                DEX -> "dex-libs"
                STATIC -> "static-libs"
                else -> throw IllegalArgumentException("Illegal LibType.")
            }
            if (isRegex) {
                categoryDir += "/regex"
            }

            val libDetailBean = try {
                request.requestLibDetail(categoryDir, libName)
            } catch (t: Throwable) {
                Timber.e(t, "DetailViewModel")
                null
            }
            detailBean.postValue(libDetailBean)
        }

    private suspend fun getNativeChipList(info: ApplicationInfo, is32bit: Boolean, isApk: Boolean): List<LibStringItemChip> {
        val packageInfo = if (!isApk) {
            PackageUtils.getPackageInfo(info.packageName)
        } else {
            PackageInfo().apply {
                packageName = info.packageName
                applicationInfo = info
            }
        }
        val list = PackageUtils.getNativeDirLibs(packageInfo, is32bit).toMutableList()
        val chipList = mutableListOf<LibStringItemChip>()
        var chip: LibChip?

        if (list.isEmpty()) {
            return chipList
        } else {
            list.forEach {
                chip = null
                LCAppUtils.getRuleWithRegex(it.name, NATIVE, info.packageName)?.let { rule ->
                    chip = LibChip(iconRes = IconResMap.getIconRes(rule.iconIndex), name = rule.label, regexName = rule.regexName)
                }
                chipList.add(LibStringItemChip(it, chip))
            }
            if (GlobalValues.libSortMode.value == MODE_SORT_BY_SIZE) {
                chipList.sortByDescending { it.item.size }
            } else {
                chipList.sortByDescending { it.chip != null }
            }
        }
        return chipList
    }

    private suspend fun getStaticChipList(packageName: String): List<LibStringItemChip> {
        Timber.d("getStaticChipList")
        val list = PackageUtils.getStaticLibs(PackageUtils.getPackageInfo(packageName))
        val chipList = mutableListOf<LibStringItemChip>()
        var chip: LibChip?

        if (list.isEmpty()) {
            return chipList
        } else {
            list.forEach {
                chip = null
                Repositories.ruleRepository.getRule(it.name)?.let { rule ->
                    chip = LibChip(iconRes = IconResMap.getIconRes(rule.iconIndex), name = rule.label, regexName = rule.regexName)
                }
                chipList.add(LibStringItemChip(it, chip))
            }
            if (GlobalValues.libSortMode.value == MODE_SORT_BY_SIZE) {
                chipList.sortByDescending { it.item.name }
            } else {
                chipList.sortByDescending { it.chip != null }
            }
        }
        return chipList
    }

    private suspend fun getDexChipList(packageName: String): List<LibStringItemChip> {
        Timber.d("getDexChipList")
        val list = PackageUtils.getDexList(packageName, packageName.endsWith("/temp.apk")).toMutableList()
        val chipList = mutableListOf<LibStringItemChip>()
        var chip: LibChip?

        if (list.isEmpty()) {
            return chipList
        } else {
            list.forEach {
                chip = null
                LCAppUtils.getRuleWithRegex(it.name, DEX)?.let { rule ->
                    chip = LibChip(iconRes = IconResMap.getIconRes(rule.iconIndex), name = rule.label, regexName = rule.regexName)
                }
                chipList.add(LibStringItemChip(it, chip))
            }
            if (GlobalValues.libSortMode.value == MODE_SORT_BY_SIZE) {
                chipList.sortByDescending { it.item.name }
            } else {
                chipList.sortByDescending { it.chip != null }
            }
        }
        return chipList
    }

    fun initAbilities(packageName: String) = viewModelScope.launch(Dispatchers.IO) {
        abilitiesMap.put(AbilityType.PAGE, MutableLiveData())
        abilitiesMap.put(AbilityType.SERVICE, MutableLiveData())
        abilitiesMap.put(AbilityType.WEB, MutableLiveData())
        abilitiesMap.put(AbilityType.DATA, MutableLiveData())

        val context: Context = getApplication<LibCheckerApp>()

        try {
            ApplicationDelegate(context).iBundleManager?.getBundleInfo(
                packageName, IBundleManager.GET_BUNDLE_WITH_ABILITIES
            )?.abilityInfos?.let { abilities ->
                val pages = abilities.asSequence()
                    .filter { it.type == AbilityInfo.AbilityType.PAGE }
                    .map { StatefulComponent(it.className, it.enabled) }
                    .toList()
                val services = abilities.asSequence()
                    .filter { it.type == AbilityInfo.AbilityType.SERVICE }
                    .map { StatefulComponent(it.className, it.enabled) }
                    .toList()
                val webs = abilities.asSequence()
                    .filter { it.type == AbilityInfo.AbilityType.WEB }
                    .map { StatefulComponent(it.className, it.enabled) }
                    .toList()
                val datas = abilities.asSequence()
                    .filter { it.type == AbilityInfo.AbilityType.DATA }
                    .map { StatefulComponent(it.className, it.enabled) }
                    .toList()

                abilitiesMap[AbilityType.PAGE]?.postValue(pages)
                abilitiesMap[AbilityType.SERVICE]?.postValue(services)
                abilitiesMap[AbilityType.WEB]?.postValue(webs)
                abilitiesMap[AbilityType.DATA]?.postValue(datas)
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }
}
