package com.absinthe.libchecker.ui.applist

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
                if ((info.flags and ApplicationInfo.FLAG_SYSTEM) != ApplicationInfo.FLAG_SYSTEM) {
                    newItems.add(
                        AppItem(
                            info.loadIcon(context.packageManager),
                            info.loadLabel(context.packageManager).toString(),
                            getAbi(info.sourceDir)
                        )
                    )
                }
            }

            //Sort
            newItems.sortWith(compareBy({ it.abi }, { it.appName }))

            withContext(Dispatchers.Main) {
                items.value = newItems
            }
        }
    }

    private fun getAbi(path: String): Int {
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
            else -> NO_LIBS
        }
    }
}