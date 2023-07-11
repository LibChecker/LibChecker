package com.absinthe.libchecker

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.window.embedding.RuleController
import androidx.window.embedding.SplitController
import coil.Coil
import coil.ImageLoader
import com.absinthe.libchecker.app.MainLooperFilter
import com.absinthe.libchecker.app.SystemServices
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.timber.ReleaseTree
import com.absinthe.libchecker.utils.timber.ThreadAwareDebugTree
import com.absinthe.libraries.utils.utils.Utility
import com.absinthe.rulesbundle.LCRemoteRepo
import com.absinthe.rulesbundle.LCRules
import com.google.android.material.color.DynamicColors
import com.jakewharton.processphoenix.ProcessPhoenix
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import java.util.UUID
import jonathanfinerty.once.Once
import me.zhanghai.android.appiconloader.coil.AppIconFetcher
import me.zhanghai.android.appiconloader.coil.AppIconKeyer
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.material.app.LocaleDelegate
import timber.log.Timber

class LibCheckerApp : Application() {

  override fun onCreate() {
    super.onCreate()

    if (ProcessPhoenix.isPhoenixProcess(this)) {
      return
    }

    if (OsUtils.atLeastP()) {
      HiddenApiBypass.addHiddenApiExemptions("")
    }

    app = this
    if (!BuildConfig.DEBUG && GlobalValues.isAnonymousAnalyticsEnabled.value == true) {
      AppCenter.start(
        this,
        BuildConfig.APP_CENTER_SECRET,
        Analytics::class.java,
        Crashes::class.java
      )
    }

    if (BuildConfig.DEBUG) {
      Timber.plant(ThreadAwareDebugTree())
    } else {
      Timber.plant(ReleaseTree())
    }

    LCRules.init(this)
    LCRules.setRemoteRepo(
      if (GlobalValues.repo == Constants.REPO_GITHUB) {
        LCRemoteRepo.Github
      } else {
        LCRemoteRepo.Gitlab
      }
    )
    Utility.init(this)
    LocaleDelegate.defaultLocale = GlobalValues.locale
    AppCompatDelegate.setDefaultNightMode(UiUtils.getNightMode())
    Once.initialise(this)
    Repositories.init(this)
    DynamicColors.applyToActivitiesIfAvailable(this)
    initSplitController()

    Coil.setImageLoader {
      ImageLoader.Builder(this)
        .components {
          add(AppIconKeyer())
          add(AppIconFetcher.Factory(40.dp, false, this@LibCheckerApp))
        }
        .build()
    }
  }

  override fun attachBaseContext(base: Context?) {
    super.attachBaseContext(base)
    MainLooperFilter.start()
  }

  private fun initSplitController() {
    val ratio = UiUtils.getScreenAspectRatio()
    val hasHinge = if (OsUtils.atLeastR()) {
      SystemServices.packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_HINGE_ANGLE)
    } else {
      false
    }
    Timber.d("initSplitController: getScreenAspectRatio: $ratio, hasHinge=$hasHinge")
    runCatching {
      if (SplitController.getInstance(this).splitSupportStatus == SplitController.SplitSupportStatus.SPLIT_AVAILABLE) {
        RuleController.getInstance(this).setRules(
          if (hasHinge || ratio in 0.85f..1.15f) {
            RuleController.parseRules(this, R.xml.main_split_config_foldable)
          } else {
            RuleController.parseRules(this, R.xml.main_split_config)
          }
        )
      }
    }
  }

  companion object {
    //noinspection StaticFieldLeak
    lateinit var app: Application

    fun generateAuthKey(): Int {
      if (GlobalValues.uuid.isEmpty()) {
        GlobalValues.uuid = UUID.randomUUID().toString()
      }
      return (GlobalValues.uuid.hashCode() + PackageUtils.getPackageInfo(app.packageName).firstInstallTime).mod(90000) + 10000
    }
  }
}
