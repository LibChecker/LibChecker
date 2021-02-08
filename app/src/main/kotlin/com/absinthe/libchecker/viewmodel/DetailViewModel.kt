package com.absinthe.libchecker.viewmodel

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
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
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.LibChip
import com.absinthe.libchecker.constant.librarymap.IconResMap
import com.absinthe.libchecker.extensions.logd
import com.absinthe.libchecker.ui.fragment.detail.LocatedCount
import com.absinthe.libchecker.ui.fragment.detail.MODE_SORT_BY_SIZE
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.PackageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class DetailViewModel(application: Application) : AndroidViewModel(application) {

    val detailBean: MutableLiveData<LibDetailBean?> = MutableLiveData()
    val repository = LibCheckerApp.repository

    val nativeLibItems: MutableLiveData<List<LibStringItemChip>> = MutableLiveData()
    val dexLibItems: MutableLiveData<List<LibStringItemChip>> = MutableLiveData()
    val componentsMap: HashMap<Int, MutableLiveData<List<StatefulComponent>>> = hashMapOf(
        SERVICE to MutableLiveData(),
        ACTIVITY to MutableLiveData(),
        RECEIVER to MutableLiveData(),
        PROVIDER to MutableLiveData()
    )
    val itemsCountLiveData: MutableLiveData<LocatedCount> = MutableLiveData(LocatedCount(0, 0))
    val itemsCountList = mutableListOf(0, 0, 0, 0, 0, 0)
    var sortMode = GlobalValues.libSortMode.value ?: MODE_SORT_BY_SIZE
    var packageName: String = ""
    var is32bit = false

    fun initSoAnalysisData(packageName: String) = viewModelScope.launch(Dispatchers.IO) {
        val context: Context = getApplication<LibCheckerApp>()
        val list = ArrayList<LibStringItemChip>()

        try {
            val info = if (packageName.endsWith("/temp.apk")) {
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
                    getNativeChipList(info, is32bit)
                )
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        withContext(Dispatchers.Main) {
            nativeLibItems.value = list
        }
    }

    fun initDexData(packageName: String) = viewModelScope.launch(Dispatchers.IO) {
        val chipList = mutableListOf<LibStringItemChip>()
        chipList.addAll(getDexChipList(packageName))

        withContext(Dispatchers.Main) {
            dexLibItems.value = chipList
        }
    }

    fun initComponentsData(packageName: String) =
        viewModelScope.launch(Dispatchers.IO) {
            val context: Context = getApplication<LibCheckerApp>()

            val pmFlag = if (LCAppUtils.atLeastN()) {
                PackageManager.MATCH_DISABLED_COMPONENTS
            } else {
                PackageManager.GET_DISABLED_COMPONENTS
            }

            try {
                if (packageName.endsWith("/temp.apk")) {
                    context.packageManager.getPackageArchiveInfo(
                        packageName,
                        PackageManager.GET_SERVICES
                                or PackageManager.GET_ACTIVITIES
                                or PackageManager.GET_RECEIVERS
                                or PackageManager.GET_PROVIDERS
                                or pmFlag
                    )?.apply {
                        applicationInfo.sourceDir = packageName
                        applicationInfo.publicSourceDir = packageName
                    }?.let {
                        val services = PackageUtils.getComponentList(it.packageName, it.services, true)
                        val activities = PackageUtils.getComponentList(it.packageName, it.activities, true)
                        val receivers = PackageUtils.getComponentList(it.packageName, it.receivers, true)
                        val providers = PackageUtils.getComponentList(it.packageName, it.providers, true)

                        withContext(Dispatchers.Main) {
                            componentsMap[SERVICE]?.value = services
                            componentsMap[ACTIVITY]?.value = activities
                            componentsMap[RECEIVER]?.value = receivers
                            componentsMap[PROVIDER]?.value = providers
                        }
                    }
                } else {
                    PackageUtils.getPackageInfo(packageName).let {
                        val services = PackageUtils.getComponentList(it.packageName, SERVICE, true)
                        val activities = PackageUtils.getComponentList(it.packageName, ACTIVITY, true)
                        val receivers = PackageUtils.getComponentList(it.packageName, RECEIVER, true)
                        val providers = PackageUtils.getComponentList(it.packageName, PROVIDER, true)

                        withContext(Dispatchers.Main) {
                            componentsMap[SERVICE]?.value = services
                            componentsMap[ACTIVITY]?.value = activities
                            componentsMap[RECEIVER]?.value = receivers
                            componentsMap[PROVIDER]?.value = providers
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(ApiManager.root)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val request: LibDetailRequest = retrofit.create(LibDetailRequest::class.java)

    fun requestLibDetail(libName: String, @LibType type: Int, isRegex: Boolean = false) =
        viewModelScope.launch(Dispatchers.IO) {
            logd("requestLibDetail")
            var categoryDir = when (type) {
                NATIVE -> "native-libs"
                SERVICE -> "services-libs"
                ACTIVITY -> "activities-libs"
                RECEIVER -> "receivers-libs"
                PROVIDER -> "providers-libs"
                DEX -> "dex-libs"
                else -> throw IllegalArgumentException("Illegal LibType.")
            }
            if (isRegex) {
                categoryDir += "/regex"
            }

            val detail = request.requestLibDetail(categoryDir, libName)
            detail.enqueue(object : Callback<LibDetailBean> {
                override fun onFailure(call: Call<LibDetailBean>, t: Throwable) {
                    Log.e("DetailViewModel", t.message ?: "")
                    detailBean.value = null
                }

                override fun onResponse(
                    call: Call<LibDetailBean>,
                    response: Response<LibDetailBean>
                ) {
                    detailBean.value = response.body()
                }
            })
        }

    private suspend fun getNativeChipList(info: ApplicationInfo, is32bit: Boolean): List<LibStringItemChip> {
        val list = PackageUtils.getNativeDirLibs(info.sourceDir, info.nativeLibraryDir ?: "", is32bit).toMutableList()
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

    private suspend fun getDexChipList(packageName: String): List<LibStringItemChip> {
        logd("getDexChipList")
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
}