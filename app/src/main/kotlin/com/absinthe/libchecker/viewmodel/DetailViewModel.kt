package com.absinthe.libchecker.viewmodel

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.LibCheckerApp
import com.absinthe.libchecker.annotation.*
import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.api.bean.NativeLibDetailBean
import com.absinthe.libchecker.api.request.NativeLibDetailRequest
import com.absinthe.libchecker.bean.LibStringItemChip
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.librarymap.DexLibMap
import com.absinthe.libchecker.constant.librarymap.NativeLibMap
import com.absinthe.libchecker.ui.fragment.applist.MODE_SORT_BY_SIZE
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

    val detailBean: MutableLiveData<NativeLibDetailBean?> = MutableLiveData()

    val nativeLibItems: MutableLiveData<List<LibStringItemChip>> = MutableLiveData()
    val dexLibItems: MutableLiveData<List<LibStringItemChip>> = MutableLiveData()
    val componentsMap: HashMap<Int, MutableLiveData<List<String>>> = hashMapOf(
        SERVICE to MutableLiveData(),
        ACTIVITY to MutableLiveData(),
        RECEIVER to MutableLiveData(),
        PROVIDER to MutableLiveData()
    )
    val itemsCountLiveData: MutableLiveData<Int> = MutableLiveData(0)
    var sortMode = GlobalValues.libSortMode.value ?: MODE_SORT_BY_SIZE

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
                    getNativeChipList(info.sourceDir, info.nativeLibraryDir ?: "")
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

            val pmFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
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

    fun requestLibDetail(libName: String, @LibType type: Int, isRegex: Boolean = false) =
        viewModelScope.launch(Dispatchers.IO) {
            val retrofit = Retrofit.Builder()
                .baseUrl(ApiManager.root)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            val request = retrofit.create(NativeLibDetailRequest::class.java)

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

            val detail = request.requestNativeLibDetail(categoryDir, libName)
            detail.enqueue(object : Callback<NativeLibDetailBean> {
                override fun onFailure(call: Call<NativeLibDetailBean>, t: Throwable) {
                    Log.e("DetailViewModel", t.message ?: "")
                    detailBean.value = null
                }

                override fun onResponse(
                    call: Call<NativeLibDetailBean>,
                    response: Response<NativeLibDetailBean>
                ) {
                    detailBean.value = response.body()
                }
            })
        }

    private fun getNativeChipList(sourcePath: String, nativePath: String): List<LibStringItemChip> {
        val list = PackageUtils.getNativeDirLibs(sourcePath, nativePath).toMutableList()
        val chipList = mutableListOf<LibStringItemChip>()

        if (list.isEmpty()) {
            return chipList
        } else {
            list.forEach {
                chipList.add(LibStringItemChip(it, NativeLibMap.getChip(it.name)))
            }
            if (GlobalValues.libSortMode.value == MODE_SORT_BY_SIZE) {
                chipList.sortByDescending { it.item.size }
            } else {
                chipList.sortByDescending { it.chip != null }
            }
        }
        return chipList
    }

    private fun getDexChipList(packageName: String): List<LibStringItemChip> {
        val list = PackageUtils.getDexList(packageName).toMutableList()
        val chipList = mutableListOf<LibStringItemChip>()

        if (list.isEmpty()) {
            return chipList
        } else {
            list.forEach {
                chipList.add(LibStringItemChip(it, DexLibMap.getChip(it.name)))
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