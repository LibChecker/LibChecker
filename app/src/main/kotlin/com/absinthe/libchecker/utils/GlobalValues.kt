package com.absinthe.libchecker.utils

import android.content.Context
import androidx.lifecycle.MutableLiveData

object GlobalValues {

    var isShowSystemApps: MutableLiveData<Boolean> = MutableLiveData()
    var isObservingDBItems: MutableLiveData<Boolean> = MutableLiveData()

    fun init(context: Context) {
        isShowSystemApps.value = SPUtils.getBoolean(context, Constants.PREF_SHOW_SYSTEM_APPS, false)
        isObservingDBItems.value = true
    }
}