package com.absinthe.libchecker.viewmodel

import android.app.Application
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.api.bean.NativeLibDetailBean
import com.absinthe.libchecker.api.request.NativeLibDetailRequest
import com.absinthe.libchecker.bean.LibStringItem
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.librarymap.NativeLibMap
import com.absinthe.libchecker.recyclerview.adapter.LibStringAdapter
import com.absinthe.libchecker.ui.fragment.applist.MODE_SORT_BY_SIZE
import com.absinthe.libchecker.ui.main.LibReferenceActivity
import com.absinthe.libchecker.utils.FreezeUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.TypeConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class DetailViewModel(application: Application) : AndroidViewModel(application) {

    val libItems: MutableLiveData<List<LibStringItem>> = MutableLiveData()
    val detailBean: MutableLiveData<NativeLibDetailBean?> = MutableLiveData()
    val componentsMap: HashMap<LibStringAdapter.Mode, MutableLiveData<List<String>>> = hashMapOf(
        Pair(LibStringAdapter.Mode.SERVICE, MutableLiveData()),
        Pair(LibStringAdapter.Mode.ACTIVITY, MutableLiveData()),
        Pair(LibStringAdapter.Mode.RECEIVER, MutableLiveData()),
        Pair(LibStringAdapter.Mode.PROVIDER, MutableLiveData())
    )

    fun initSoAnalysisData(context: Context, packageName: String) =
        viewModelScope.launch(Dispatchers.IO) {
            val list = ArrayList<LibStringItem>()

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
                        getAbiByNativeDir(
                            context,
                            info.sourceDir,
                            info.nativeLibraryDir ?: ""
                        )
                    )
                }
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
                list.add(LibStringItem("Not found"))
            }

            withContext(Dispatchers.Main) {
                libItems.value = list
            }
        }

    fun initComponentsData(context: Context, packageName: String) =
        viewModelScope.launch(Dispatchers.IO) {
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
                        withContext(Dispatchers.IO) {
                            val services = PackageUtils.getComponentList(it.packageName, it.services, true)
                            withContext(Dispatchers.Main) {
                                componentsMap[LibStringAdapter.Mode.SERVICE]?.value = services
                            }
                        }
                        withContext(Dispatchers.IO) {
                            val activities = PackageUtils.getComponentList(it.packageName, it.activities, true)
                            withContext(Dispatchers.Main) {
                                componentsMap[LibStringAdapter.Mode.ACTIVITY]?.value = activities
                            }
                        }
                        withContext(Dispatchers.IO) {
                            val receivers = PackageUtils.getComponentList(it.packageName, it.receivers, true)
                            withContext(Dispatchers.Main) {
                                componentsMap[LibStringAdapter.Mode.RECEIVER]?.value = receivers
                            }
                        }
                        withContext(Dispatchers.IO) {
                            val providers = PackageUtils.getComponentList(it.packageName, it.providers, true)
                            withContext(Dispatchers.Main) {
                                componentsMap[LibStringAdapter.Mode.PROVIDER]?.value = providers
                            }
                        }
                    }
                } else {
                    var packageInfo: PackageInfo? = null
                    while (packageInfo == null) {
                        packageInfo = context.packageManager.getPackageInfo(
                            packageName,
                            FreezeUtils.PM_FLAGS_GET_APP_INFO
                        )
                    }
                    packageInfo.let {
                        if (!FreezeUtils.isAppFrozen(it.applicationInfo)) {
                            withContext(Dispatchers.IO) {
                                val services = PackageUtils.getComponentList(it.packageName, LibReferenceActivity.Type.TYPE_SERVICE, true)
                                withContext(Dispatchers.Main) {
                                    componentsMap[LibStringAdapter.Mode.SERVICE]?.value = services
                                }
                            }
                            withContext(Dispatchers.IO) {
                                val activities = PackageUtils.getComponentList(it.packageName, LibReferenceActivity.Type.TYPE_ACTIVITY, true)
                                withContext(Dispatchers.Main) {
                                    componentsMap[LibStringAdapter.Mode.ACTIVITY]?.value = activities
                                }
                            }
                            withContext(Dispatchers.IO) {
                                val receivers = PackageUtils.getComponentList(it.packageName, LibReferenceActivity.Type.TYPE_BROADCAST_RECEIVER, true)
                                withContext(Dispatchers.Main) {
                                    componentsMap[LibStringAdapter.Mode.RECEIVER]?.value = receivers
                                }
                            }
                            withContext(Dispatchers.IO) {
                                val providers = PackageUtils.getComponentList(it.packageName, LibReferenceActivity.Type.TYPE_CONTENT_PROVIDER, true)
                                withContext(Dispatchers.Main) {
                                    componentsMap[LibStringAdapter.Mode.PROVIDER]?.value = providers
                                }
                            }
                        } else {
                            context.packageManager.getPackageArchiveInfo(
                                context.packageManager.getPackageInfo(packageName, 0).applicationInfo.sourceDir,
                                PackageManager.GET_SERVICES
                                        or PackageManager.GET_ACTIVITIES
                                        or PackageManager.GET_RECEIVERS
                                        or PackageManager.GET_PROVIDERS
                                        or pmFlag
                            )?.let {
                                withContext(Dispatchers.IO) {
                                    val services = PackageUtils.getComponentList(it, TypeConverter.libModeToRefType(LibStringAdapter.Mode.SERVICE), true)
                                    withContext(Dispatchers.Main) {
                                        componentsMap[LibStringAdapter.Mode.SERVICE]?.value = services
                                    }
                                }
                                withContext(Dispatchers.IO) {
                                    val activities = PackageUtils.getComponentList(it, TypeConverter.libModeToRefType(LibStringAdapter.Mode.ACTIVITY), true)
                                    withContext(Dispatchers.Main) {
                                        componentsMap[LibStringAdapter.Mode.ACTIVITY]?.value = activities
                                    }
                                }
                                withContext(Dispatchers.IO) {
                                    val receivers = PackageUtils.getComponentList(it, TypeConverter.libModeToRefType(LibStringAdapter.Mode.RECEIVER), true)
                                    withContext(Dispatchers.Main) {
                                        componentsMap[LibStringAdapter.Mode.RECEIVER]?.value = receivers
                                    }
                                }
                                withContext(Dispatchers.IO) {
                                    val providers = PackageUtils.getComponentList(it, TypeConverter.libModeToRefType(LibStringAdapter.Mode.PROVIDER), true)
                                    withContext(Dispatchers.Main) {
                                        componentsMap[LibStringAdapter.Mode.PROVIDER]?.value = providers
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    fun requestLibDetail(libName: String, type: LibStringAdapter.Mode, isRegex: Boolean = false) =
        viewModelScope.launch(Dispatchers.IO) {
            val retrofit = Retrofit.Builder()
                .baseUrl(ApiManager.root)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            val request = retrofit.create(NativeLibDetailRequest::class.java)

            var categoryDir = when (type) {
                LibStringAdapter.Mode.NATIVE -> "native-libs"
                LibStringAdapter.Mode.SERVICE -> "services-libs"
                LibStringAdapter.Mode.ACTIVITY -> "activities-libs"
                LibStringAdapter.Mode.RECEIVER -> "receivers-libs"
                LibStringAdapter.Mode.PROVIDER -> "providers-libs"
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

    private fun getAbiByNativeDir(
        context: Context,
        sourcePath: String,
        nativePath: String
    ): List<LibStringItem> {
        val list = PackageUtils.getNativeDirLibs(sourcePath, nativePath).toMutableList()

        if (list.isEmpty()) {
            list.add(LibStringItem(context.getString(R.string.empty_list)))
        } else {
            if (GlobalValues.libSortMode.value == MODE_SORT_BY_SIZE) {
                list.sortByDescending { it.size }
            } else {
                list.sortByDescending {
                    NativeLibMap.contains(it.name)
                }
            }
        }
        return list
    }
}