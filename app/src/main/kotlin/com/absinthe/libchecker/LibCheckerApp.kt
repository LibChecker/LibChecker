package com.absinthe.libchecker

import android.app.Application
import android.content.Context
import android.content.pm.PackageParser
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.window.embedding.RuleController
import androidx.window.embedding.SplitController
import coil.Coil
import coil.ImageLoader
import com.absinthe.libchecker.app.MainLooperFilter
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.Telemetry
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.timber.ReleaseTree
import com.absinthe.libchecker.utils.timber.ThreadAwareDebugTree
import com.absinthe.libraries.utils.utils.Utility
import com.absinthe.rulesbundle.LCRemoteRepo
import com.absinthe.rulesbundle.LCRules
import com.google.android.material.color.DynamicColors
import com.jakewharton.processphoenix.ProcessPhoenix
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

    bypass()

    app = this

    if (BuildConfig.DEBUG) {
      Timber.plant(ThreadAwareDebugTree())
    } else {
      Timber.plant(ReleaseTree())
    }
    Telemetry.setEnable(GlobalValues.isAnonymousAnalyticsEnabled)
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
    if (OsUtils.atLeastT()) {
      AppCompatDelegate.setApplicationLocales(LocaleListCompat.create(GlobalValues.locale))
    }
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
    val hasHinge = UiUtils.hasHinge()
    val splitSupportStatus = SplitController.getInstance(this).splitSupportStatus
    Timber.d("initSplitController: getScreenAspectRatio: $ratio, hasHinge=$hasHinge, splitSupportStatus=$splitSupportStatus")
    runCatching {
      if (splitSupportStatus == SplitController.SplitSupportStatus.SPLIT_AVAILABLE) {
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

  private fun bypass() {
    if (OsUtils.atLeastP()) {
      HiddenApiBypass.addHiddenApiExemptions("")
    }

    // bypass PackageParser check
    // see also: https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/core/java/android/content/pm/PackageParser.java;l=2695
    @Suppress("SoonBlockedPrivateApi")
    PackageParser::class.java.getDeclaredField("SDK_VERSION").apply {
      isAccessible = true
      set(null, Integer.MAX_VALUE)
    }
  }

  companion object {
    //noinspection StaticFieldLeak
    lateinit var app: Application
  }
}
