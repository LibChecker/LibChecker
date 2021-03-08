package com.absinthe.libchecker

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.absinthe.libchecker.app.Global
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.database.LCDatabase
import com.absinthe.libchecker.database.LCRepository
import com.absinthe.libchecker.utils.timber.ReleaseTree
import com.absinthe.libchecker.utils.timber.ThreadAwareDebugTree
import com.absinthe.libraries.utils.utils.Utility
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import jonathanfinerty.once.Once
import rikka.material.app.DayNightDelegate
import timber.log.Timber

class LibCheckerApp : Application() {

    override fun onCreate() {
        super.onCreate()

        context = this
        if (!BuildConfig.DEBUG && GlobalValues.isAnonymousAnalyticsEnabled.value == true) {
            AppCenter.start(
                this, Constants.APP_CENTER_SECRET,
                Analytics::class.java, Crashes::class.java
            )
        }

        if (BuildConfig.DEBUG) {
            Timber.plant(ThreadAwareDebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }

        Utility.init(this)
        DayNightDelegate.setApplicationContext(this)
        DayNightDelegate.setDefaultNightMode(AppCompatDelegate.getDefaultNightMode())
        Once.initialise(this)
        repository = LCRepository(LCDatabase.getDatabase(this).lcDao())
//        ProcessLifecycleOwner.get().lifecycle.addObserver(GlobalLifecycleObserver())
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        Global.loop()
    }

    companion object {
        lateinit var repository: LCRepository

        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
    }
}