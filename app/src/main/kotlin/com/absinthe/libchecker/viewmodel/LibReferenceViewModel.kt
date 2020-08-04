package com.absinthe.libchecker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.bean.AppItem
import com.absinthe.libchecker.constant.LibType
import com.absinthe.libchecker.constant.NATIVE
import com.absinthe.libchecker.database.AppItemRepository
import com.absinthe.libchecker.utils.PackageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LibReferenceViewModel(application: Application) : AndroidViewModel(application) {

    val libRefList: MutableLiveData<List<AppItem>> = MutableLiveData()

    fun setData(name: String, @LibType type: Int) = viewModelScope.launch(Dispatchers.IO) {
        val list = mutableListOf<AppItem>()

        AppItemRepository.allItems.value?.let { items ->
            try {
                if (type == NATIVE) {
                    for (item in items) {
                        val packageInfo = PackageUtils.getPackageInfo(item.packageName)

                        val natives = PackageUtils.getNativeDirLibs(
                            packageInfo.applicationInfo.sourceDir,
                            packageInfo.applicationInfo.nativeLibraryDir
                        )

                        for (native in natives) {
                            if (native.name == name) {
                                list.add(item)
                                break
                            }
                        }
                    }
                } else {
                    for (item in items) {
                        if (PackageUtils.getComponentList(item.packageName, type, false).contains(name)) {
                            list.add(item)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        withContext(Dispatchers.Main) {
            libRefList.value = list
        }
    }

}