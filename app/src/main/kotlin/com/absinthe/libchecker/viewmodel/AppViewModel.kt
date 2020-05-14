package com.absinthe.libchecker.viewmodel

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.system.ErrnoException
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.api.Configuration
import com.absinthe.libchecker.api.request.ConfigurationRequest
import com.absinthe.libchecker.database.LCDatabase
import com.absinthe.libchecker.database.LCItem
import com.absinthe.libchecker.database.LCRepository
import com.absinthe.libchecker.utils.Constants
import com.absinthe.libchecker.utils.GlobalValues
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.viewholder.*
import com.blankj.utilcode.util.AppUtils
import com.microsoft.appcenter.analytics.Analytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileNotFoundException
import java.util.zip.ZipException
import java.util.zip.ZipFile

class AppViewModel(application: Application) : AndroidViewModel(application) {

    val dbItems: LiveData<List<LCItem>>
    val appItems: MutableLiveData<ArrayList<AppItem>> = MutableLiveData()
    val configuration: MutableLiveData<Configuration> = MutableLiveData()
    var isInit = false

    private val tag = AppViewModel::class.java.simpleName
    private val repository: LCRepository

    init {
        val lcDao = LCDatabase.getDatabase(application).lcDao()
        repository = LCRepository(lcDao)
        dbItems = repository.allItems
    }

    fun initItems(context: Context) = viewModelScope.launch(Dispatchers.IO) {
        Log.d(tag, "initItems")

        val appList = context.packageManager
            .getInstalledApplications(PackageManager.GET_SHARED_LIBRARY_FILES)
        val newItems = ArrayList<AppItem>()

        for (info in appList) {
            try {
                val packageInfo = PackageUtils.getPackageInfo(info)
                val versionCode = PackageUtils.getVersionCode(packageInfo)

                val appItem = AppItem().apply {
                    icon = info.loadIcon(context.packageManager)
                    appName = info.loadLabel(context.packageManager).toString()
                    packageName = info.packageName
                    versionName = PackageUtils.getVersionString(info.packageName)
                    abi = getAbi(info.sourceDir, info.nativeLibraryDir)
                    isSystem =
                        (info.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM
                    updateTime = packageInfo.lastUpdateTime
                }
                val item = LCItem(
                    info.packageName,
                    info.loadLabel(context.packageManager).toString(),
                    packageInfo.versionName ?: "",
                    versionCode,
                    packageInfo.firstInstallTime,
                    packageInfo.lastUpdateTime,
                    (info.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM,
                    getAbi(info.sourceDir, info.nativeLibraryDir).toShort()
                )

                GlobalValues.isShowSystemApps.value?.let {
                    if (it || (!it && !item.isSystem)) {
                        newItems.add(appItem)
                    }
                }

                insert(item)
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
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
            appItems.value = newItems
        }
    }

    fun addItem() = viewModelScope.launch(Dispatchers.IO) {
        Log.d(tag, "addItems")

        dbItems.value?.let { value ->
            val newItems = ArrayList<AppItem>()

            for (item in value) {
                val appItem = AppItem().apply {
                    icon = AppUtils.getAppIcon(item.packageName)
                        ?: ColorDrawable(Color.TRANSPARENT)
                    appName = item.label
                    packageName = item.packageName
                    versionName = PackageUtils.getVersionString(item.packageName)
                    abi = item.abi.toInt()
                    isSystem = item.isSystem
                    updateTime = item.lastUpdatedTime
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
                appItems.value = newItems
            }
        }
    }

    fun requestChange(context: Context) = viewModelScope.launch(Dispatchers.IO) {
        Log.d(tag, "requestChange")

        val appList = context.packageManager
            .getInstalledApplications(PackageManager.GET_SHARED_LIBRARY_FILES)

        dbItems.value?.let { value ->
            for (dbItem in value) {
                try {
                    appList.find { it.packageName == dbItem.packageName }?.let {
                        val packageInfo = PackageUtils.getPackageInfo(it)
                        val versionCode = PackageUtils.getVersionCode(packageInfo)

                        if (packageInfo.lastUpdateTime != dbItem.lastUpdatedTime) {
                            val item = LCItem(
                                it.packageName,
                                it.loadLabel(context.packageManager).toString(),
                                packageInfo.versionName ?: "null",
                                versionCode,
                                packageInfo.firstInstallTime,
                                packageInfo.lastUpdateTime,
                                (it.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM,
                                getAbi(it.sourceDir, it.nativeLibraryDir).toShort()
                            )
                            update(item)
                        }

                        appList.remove(it)
                    } ?: run {
                        delete(dbItem)
                    }
                } catch (e: PackageManager.NameNotFoundException) {
                    e.printStackTrace()
                    continue
                }
            }

            for (info in appList) {
                try {
                    val packageInfo = PackageUtils.getPackageInfo(info)
                    val versionCode = PackageUtils.getVersionCode(packageInfo)

                    val lcItem = LCItem(
                        info.packageName,
                        info.loadLabel(context.packageManager).toString(),
                        packageInfo.versionName ?: "null",
                        versionCode,
                        packageInfo.firstInstallTime,
                        packageInfo.lastUpdateTime,
                        (info.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM,
                        getAbi(info.sourceDir, info.nativeLibraryDir).toShort()
                    )

                    insert(lcItem)
                } catch (e: PackageManager.NameNotFoundException) {
                    e.printStackTrace()
                    continue
                }
            }
        }
    }

    fun collectPopularLibraries(context: Context) = viewModelScope.launch(Dispatchers.IO) {
        val appList = context.packageManager
            .getInstalledApplications(PackageManager.GET_SHARED_LIBRARY_FILES)
        val map = HashMap<String, Int>()

        for (item in appList) {
            val libList = PackageUtils.getAbiByNativeDir(
                item.sourceDir,
                item.nativeLibraryDir
            )

            for (lib in libList) {
                val count = map[lib.name] ?: 0
                map[lib.name] = count + 1
            }
        }

        for (entry in map) {
            if (entry.value > 3) {
                val properties: MutableMap<String, String> = java.util.HashMap()
                properties["Library name"] = entry.key
                properties["Library count"] = entry.value.toString()

                Analytics.trackEvent("Library", properties)
            }
        }
    }

    fun requestConfiguration() = viewModelScope.launch(Dispatchers.IO) {
        val retrofit = Retrofit.Builder()
            .baseUrl(ApiManager.ROOT_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val request = retrofit.create(ConfigurationRequest::class.java)
        val config = request.requestConfiguration()
        config.enqueue(object : Callback<Configuration> {
            override fun onFailure(call: Call<Configuration>, t: Throwable) {

            }

            override fun onResponse(call: Call<Configuration>, response: Response<Configuration>) {
                viewModelScope.launch(Dispatchers.Main) {
                    response.body()?.let {
                        configuration.value = it
                    }
                }
            }
        })
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

    private fun getAbi(path: String, nativePath: String): Int {
        val abiList = ArrayList<String>()

        try {
            val file = File(path)
            val zipFile = ZipFile(file)
            val entries = zipFile.entries()

            while (entries.hasMoreElements()) {
                val name = entries.nextElement().name

                if (name.contains("lib/")) {
                    abiList.add(name.split("/")[1])
                }
            }
            zipFile.close()
        } catch (e: ErrnoException) {
            e.printStackTrace()
            return ERROR
        } catch (e: ZipException) {
            e.printStackTrace()
            return ERROR
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            return ERROR
        }

        return when {
            abiList.contains("arm64-v8a") -> ARMV8
            abiList.contains("armeabi-v7a") -> ARMV7
            abiList.contains("armeabi") -> ARMV5
            else -> getAbiByNativeDir(nativePath)
        }
    }

    private fun getAbiByNativeDir(nativePath: String): Int {
        val file = File(nativePath.substring(0, nativePath.lastIndexOf("/")))
        val abiList = ArrayList<String>()

        val fileList = file.listFiles() ?: return NO_LIBS
        fileList.iterator().forEach { abiList.add(it.name) }

        return when {
            abiList.contains("arm64") -> ARMV8
            abiList.contains("arm") -> ARMV7
            else -> NO_LIBS
        }
    }
}