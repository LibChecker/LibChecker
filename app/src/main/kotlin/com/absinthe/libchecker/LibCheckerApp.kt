package com.absinthe.libchecker

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.Build
import com.absinthe.libchecker.app.Global
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.timber.ReleaseTree
import com.absinthe.libchecker.utils.timber.ThreadAwareDebugTree
import com.absinthe.libraries.utils.utils.Utility
import com.jakewharton.processphoenix.ProcessPhoenix
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import jonathanfinerty.once.Once
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.material.app.DayNightDelegate
import rikka.material.app.LocaleDelegate
import timber.log.Timber

class LibCheckerApp : Application() {

  override fun onCreate() {
    super.onCreate()

    if (ProcessPhoenix.isPhoenixProcess(this)) {
      return
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      HiddenApiBypass.addHiddenApiExemptions("L")
    }

    app = this
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
    LocaleDelegate.defaultLocale = GlobalValues.locale
    DayNightDelegate.setApplicationContext(this)
    DayNightDelegate.setDefaultNightMode(LCAppUtils.getNightMode(GlobalValues.darkMode))
    Once.initialise(this)
    Repositories.init(this)
    Repositories.checkRulesDatabase()
  }

  override fun attachBaseContext(base: Context?) {
    super.attachBaseContext(base)
    Global.start()
  }

  companion object {
    @SuppressLint("StaticFieldLeak")
    lateinit var app: Application
  }
}
