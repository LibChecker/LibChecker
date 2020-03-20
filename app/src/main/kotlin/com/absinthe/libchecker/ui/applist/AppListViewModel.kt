package com.absinthe.libchecker.ui.applist

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.utils.GlobalValues
import com.absinthe.libchecker.viewholder.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile

class AppListViewModel : ViewModel() {
    val items: MutableLiveData<ArrayList<AppItem>> = MutableLiveData()

    fun getItems(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val appList = context.packageManager
                .getInstalledApplications(PackageManager.GET_SHARED_LIBRARY_FILES)
            val newItems = ArrayList<AppItem>()

            for (info in appList) {
                if (((info.flags and ApplicationInfo.FLAG_SYSTEM) != ApplicationInfo.FLAG_SYSTEM)
                    || GlobalValues.isShowSystemApps
                ) {

                    val appItem = AppItem().apply {
                        icon = info.loadIcon(context.packageManager)
                        appName = info.loadLabel(context.packageManager).toString()
                        packageName = info.packageName
                        val packageInfo = context.packageManager.getPackageInfo(info.packageName, 0)
                        versionName = "${packageInfo.versionName}(${packageInfo.versionCode})"
                        abi = getAbi(info.sourceDir, info.nativeLibraryDir)
                    }

                    newItems.add(appItem)
                }
            }

            //Sort
            newItems.sortWith(compareBy({ it.abi }, { it.appName }))

            withContext(Dispatchers.Main) {
                items.value = newItems
            }
        }
    }

    private fun getAbi(path: String, nativePath: String): Int {
        val file = File(path)
        val zipFile = ZipFile(file)
        val entries = zipFile.entries()
        val abiList = ArrayList<String>()

        while (entries.hasMoreElements()) {
            val name = entries.nextElement().name
            if (name.contains("lib/")) {
                abiList.add(name.split("/")[1])
            }
        }
        zipFile.close()

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

        for (abi in fileList) {
            abiList.add(abi.name)
        }

        return when {
            abiList.contains("arm64") -> ARMV8
            abiList.contains("arm") -> ARMV7
            else -> NO_LIBS
        }
    }
}