package com.absinthe.libchecker.viewmodel

import android.app.Application
import android.content.pm.PackageInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.bean.AppItem
import com.absinthe.libchecker.bean.LibStringItem
import com.absinthe.libchecker.database.AppItemRepository
import com.absinthe.libchecker.utils.PackageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LibReferenceViewModel(application: Application) : AndroidViewModel(application) {

    val libRefList: MutableLiveData<List<AppItem>> = MutableLiveData()

    fun setData(name: String, @LibType type: Int) = viewModelScope.launch(Dispatchers.IO) {
        val list = mutableListOf<AppItem>()

        AppItemRepository.allDatabaseItems.value?.let { items ->
            if (type == NATIVE) {
                var packageInfo: PackageInfo
                var natives: List<LibStringItem>

                for (item in items) {
                    natives = try {
                        packageInfo = PackageUtils.getPackageInfo(item.packageName)
                        PackageUtils.getNativeDirLibs(
                            packageInfo.applicationInfo.sourceDir,
                            packageInfo.applicationInfo.nativeLibraryDir
                        )
                    } catch (e: Exception) {
                        listOf()
                    }

                    for (native in natives) {
                        if (native.name == name) {
                            list.add(item)
                            break
                        }
                    }
                }
            } else {
                for (item in items) {
                    try {
                        if (PackageUtils.getComponentList(item.packageName, type, false).contains(name)) {
                            list.add(item)
                        }
                    } catch (e: Exception) {
                        continue
                    }
                }
            }
        }

        withContext(Dispatchers.Main) {
            libRefList.value = list
        }
    }

}