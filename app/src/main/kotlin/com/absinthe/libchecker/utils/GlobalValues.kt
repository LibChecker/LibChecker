package com.absinthe.libchecker.utils

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.absinthe.libchecker.ui.fragment.applist.MODE_SORT_BY_SIZE

object GlobalValues {

    var isShowSystemApps: MutableLiveData<Boolean> = MutableLiveData()
    var isObservingDBItems: MutableLiveData<Boolean> = MutableLiveData()
    var appSortMode: MutableLiveData<Int> = MutableLiveData()
    var libSortMode: MutableLiveData<Int> = MutableLiveData()

    fun init(context: Context) {
        isShowSystemApps.value = SPUtils.getBoolean(context, Constants.PREF_SHOW_SYSTEM_APPS, false)
        isObservingDBItems.value = true
        appSortMode.value = SPUtils.getInt(context, Constants.PREF_APP_SORT_MODE, Constants.SORT_MODE_DEFAULT)
        libSortMode.value = SPUtils.getInt(context, Constants.PREF_LIB_SORT_MODE, MODE_SORT_BY_SIZE)
    }
}