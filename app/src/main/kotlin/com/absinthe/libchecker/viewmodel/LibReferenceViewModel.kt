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
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.PackageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class LibReferenceViewModel(application: Application) : AndroidViewModel(application) {

    val libRefList: MutableLiveData<List<LCItem>> = MutableLiveData()
    val dbItems: LiveData<List<LCItem>> = Repositories.lcRepository.allDatabaseItems

    fun setData(name: String, @LibType type: Int) = viewModelScope.launch(Dispatchers.IO) {
        val list = mutableListOf<LCItem>()

        dbItems.value?.let { items ->
            if (type == NATIVE) {
                var packageInfo: PackageInfo
                var natives: List<LibStringItem>

                for (item in items) {
                    natives = try {
                        packageInfo = PackageUtils.getPackageInfo(item.packageName)
                        PackageUtils.getNativeDirLibs(packageInfo)
                    } catch (e: Exception) {
                        Timber.e(e)
                        emptyList()
                    }

                    natives.find { it.name == name }?.run {
                        if (LCAppUtils.checkNativeLibValidation(item.packageName, name)) {
                            list.add(item)
                        }
                    }
                }
            } else {
                for (item in items) {
                    try {
                        if (PackageUtils.getComponentStringList(item.packageName, type, false)
                                .contains(name)
                        ) {
                            list.add(item)
                        }
                    } catch (e: Exception) {
                        continue
                    }
                }
            }
        }

        val filterList = if (GlobalValues.isShowSystemApps.value == true) {
            list
        } else {
            list.filter { !it.isSystem }
        }

        libRefList.postValue(filterList)
    }

}
