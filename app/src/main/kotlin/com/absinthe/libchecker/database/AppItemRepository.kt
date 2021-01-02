package com.absinthe.libchecker.database

import android.content.pm.ApplicationInfo
import androidx.lifecycle.MutableLiveData

object AppItemRepository {
    val allApplicationInfoItems: MutableLiveData<List<ApplicationInfo>> = MutableLiveData()
    var trackItemsChanged = false
    var shouldRefreshAppList = false
}