package com.absinthe.libchecker.viewmodel

import android.app.Application
import android.content.pm.PackageInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.bean.LibStringItem
import com.absinthe.libchecker.database.LCDatabase
import com.absinthe.libchecker.database.LCRepository
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.utils.PackageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LibReferenceViewModel(application: Application) : AndroidViewModel(application) {

    val libRefList: MutableLiveData<List<LCItem>> = MutableLiveData()
    val dbItems: LiveData<List<LCItem>>
    private val repository: LCRepository

    init {
        val lcDao = LCDatabase.getDatabase(application).lcDao()
        repository = LCRepository(lcDao)
        dbItems = repository.allDatabaseItems
    }

    fun setData(name: String, @LibType type: Int) = viewModelScope.launch(Dispatchers.IO) {
        val list = mutableListOf<LCItem>()

        dbItems.value?.let { items ->
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
                        emptyList()
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