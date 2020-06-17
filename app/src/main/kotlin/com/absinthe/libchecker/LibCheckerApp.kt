package com.absinthe.libchecker

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
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
                this, "5f11b856-0a27-4438-a038-9e18e4797133",
                Analytics::class.java, Crashes::class.java
            )
        }

        DayNightDelegate.setApplicationContext(this)
        DayNightDelegate.setDefaultNightMode(AppCompatDelegate.getDefaultNightMode())
        Once.initialise(this)
    }
}