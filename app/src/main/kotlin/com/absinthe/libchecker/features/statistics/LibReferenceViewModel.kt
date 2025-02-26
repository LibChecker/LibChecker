package com.absinthe.libchecker.features.statistics

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.features.statistics.bean.LibStringItem
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.PackageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

class LibReferenceViewModel : ViewModel() {

  val libRefListFlow: MutableSharedFlow<List<LCItem>> = MutableSharedFlow()
  val dbItemsFlow: Flow<List<LCItem>> = Repositories.lcRepository.allLCItemsFlow

  fun setData(name: String, @LibType type: Int) = viewModelScope.launch(Dispatchers.IO) {
    dbItemsFlow.collectLatest {
      setDataInternal(it, name, type)
    }
  }

  fun setData(packagesList: List<String>) = viewModelScope.launch(Dispatchers.IO) {
    val dbItemsStateFlow = dbItemsFlow.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val dbItems = dbItemsStateFlow.value.takeUnless { it.isNullOrEmpty() }
      ?: dbItemsStateFlow.filterNotNull().first()

    val dbItemsMap = dbItems.associateBy { it.packageName }
    val list = packagesList.mapNotNull { dbItemsMap[it] }

    val filterList = list.takeIf { GlobalValues.isShowSystemApps } ?: list.filter { !it.isSystem }
    libRefListFlow.emit(filterList)
  }

  private suspend fun setDataInternal(lcItems: List<LCItem>, name: String, @LibType type: Int) {
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
            val componentStringList =
              PackageUtils.getComponentStringList(item.packageName, type, false)
            if (componentStringList.contains(name)) {
              list.add(item)
            }
          }
        }

        PERMISSION -> {
          for (item in items) {
            val permissionList = PackageUtils.getPermissionsList(item.packageName)
            if (permissionList.contains(name)) {
              list.add(item)
            }
          }
        }

        METADATA -> {
          for (item in items) {
            val pi = PackageUtils.getPackageInfo(item.packageName, PackageManager.GET_META_DATA)
            val metadataList = PackageUtils.getMetaDataItems(pi)
            if (metadataList.any { it.name == name }) {
              list.add(item)
            }
          }
        }

        else -> {
        }
      }
    }

    val filterList = if (GlobalValues.isShowSystemApps) {
      list
    } else {
      list.filter { !it.isSystem }
    }

    libRefListFlow.emit(filterList)
  }
}
