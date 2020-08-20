package com.absinthe.libchecker.database

import android.content.pm.ApplicationInfo
import androidx.lifecycle.MutableLiveData
import com.absinthe.libchecker.bean.AppItem

object AppItemRepository {

    val allDatabaseItems: MutableLiveData<List<AppItem>> = MutableLiveData()

    val allApplicationInfoItems: MutableLiveData<List<ApplicationInfo>> = MutableLiveData()

    var shouldRefreshAppList = false
}