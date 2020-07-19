package com.absinthe.libchecker

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ProcessLifecycleOwner
import com.absinthe.libchecker.app.Global
import com.absinthe.libchecker.app.GlobalLifecycleObserver
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.librarymap.NativeLibMap
import com.absinthe.libchecker.utils.AppUtils
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import jonathanfinerty.once.Once
import rikka.material.app.DayNightDelegate

class LibCheckerApp : Application() {

    override fun onCreate() {
        super.onCreate()

        if (!BuildConfig.DEBUG) {
            AppCenter.start(
                this, Constants.APP_CENTER_SECRET,
                Analytics::class.java, Crashes::class.java
            )
        }

        DayNightDelegate.setApplicationContext(this)
        DayNightDelegate.setDefaultNightMode(AppCompatDelegate.getDefaultNightMode())
        Once.initialise(this)
        AppUtils.requestConfiguration()
        ProcessLifecycleOwner.get().lifecycle.addObserver(GlobalLifecycleObserver())
        NativeLibMap.getMap()
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        Global.loop()
    }
}