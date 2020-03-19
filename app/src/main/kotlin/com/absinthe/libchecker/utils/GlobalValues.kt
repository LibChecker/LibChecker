package com.absinthe.libchecker.utils

import android.content.Context

object GlobalValues {

    lateinit var context: Context

    var isShowSystemApps = false
    set(value) {
        field = value
        SPUtils.putBoolean(context, Constants.PREF_SHOW_SYSTEM_APPS, value)
    }

    fun init(context: Context) {
        this.context = context
        isShowSystemApps = SPUtils.getBoolean(context, Constants.PREF_SHOW_SYSTEM_APPS, false)
    }
}