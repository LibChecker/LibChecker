package com.microsoft.appcenter

import android.app.Application

object AppCenter {

    @SafeVarargs
    fun start(application: Application, appSecret: String, vararg services: Class<out AppCenterService>) {
    }
}
