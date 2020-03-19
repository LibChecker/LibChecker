package com.absinthe.libchecker.ui.applist

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.viewholder.AppItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                            getAbi("")
                        )
                    )
                }
            }
            withContext(Dispatchers.Main) {
                items.value = newItems
            }
        }
    }

    private fun getAbi(path: String): String {
        return "ARMv8"
    }
}