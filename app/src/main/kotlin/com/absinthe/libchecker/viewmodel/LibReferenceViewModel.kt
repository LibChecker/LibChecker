package com.absinthe.libchecker.viewmodel

import android.app.Application
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.model.LibStringItem
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
    dbItems.value?.let {
      setDataInternal(it, name, type)
    }
  }

  fun setData(packagesList: List<String>) = viewModelScope.launch(Dispatchers.IO) {
    dbItems.value?.let { dbList ->
      val list = mutableListOf<LCItem>()
      packagesList.forEach { pkgName ->
        dbList.find { it.packageName == pkgName }?.let { lcItem ->
          list.add(lcItem)
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

  private fun setDataInternal(lcItems: List<LCItem>, name: String, @LibType type: Int) {
    val list = mutableListOf<LCItem>()

    lcItems.let { items ->
      when (type) {
        NATIVE -> {
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
        }
        SERVICE, ACTIVITY, RECEIVER, PROVIDER -> {
          for (item in items) {
            try {
              val componentStringList =
                PackageUtils.getComponentStringList(item.packageName, type, false)
              if (componentStringList.contains(name)) {
                list.add(item)
              }
            } catch (e: Exception) {
              continue
            }
          }
        }
        PERMISSION -> {
          for (item in items) {
            try {
              val permissionList = PackageUtils.getPermissionsList(item.packageName)
              if (permissionList.contains(name)) {
                list.add(item)
              }
            } catch (e: Exception) {
              continue
            }
          }
        }
        METADATA -> {
          for (item in items) {
            try {
              val metadataList =
                PackageUtils.getMetaDataItems(
                  PackageUtils.getPackageInfo(
                    item.packageName,
                    PackageManager.GET_META_DATA
                  )
                )
              if (metadataList.any { it.name == name }) {
                list.add(item)
              }
            } catch (e: Exception) {
              continue
            }
          }
        }
        else -> {
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
