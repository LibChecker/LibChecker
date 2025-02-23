package com.absinthe.libchecker.constant

import android.content.SharedPreferences
import androidx.core.content.edit
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.LibCheckerApp
import com.absinthe.libchecker.LibCheckerApp.Companion.app
import com.absinthe.libchecker.app.SystemServices
import com.absinthe.libchecker.constant.options.AdvancedOptions
import com.absinthe.libchecker.constant.options.LibReferenceOptions
import com.absinthe.libchecker.constant.options.SnapshotOptions
import com.absinthe.libchecker.features.applist.MODE_SORT_BY_SIZE
import com.absinthe.libchecker.utils.DateUtils
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.SPDelegates
import com.absinthe.libchecker.utils.SPUtils
import com.absinthe.libchecker.utils.extensions.unsafeLazy
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.MutableSharedFlow
import timber.log.Timber

const val SP_NAME = "${BuildConfig.APPLICATION_ID}_preferences"

object GlobalValues {

  fun generateAuthKey(): Int {
    if (uuid.isEmpty()) {
      uuid = UUID.randomUUID().toString()
    }
    return (uuid.hashCode() + PackageUtils.getPackageInfo(app.packageName).firstInstallTime).mod(90000) + 10000
  }

  private fun getPreferences(): SharedPreferences {
    return SPUtils.sp
  }

  val preferencesFlow = MutableSharedFlow<Pair<String, Any>>()

  var advancedOptions: Int by SPDelegates(Constants.PREF_ADVANCED_OPTIONS, AdvancedOptions.DEFAULT_OPTIONS)

  var itemAdvancedOptions: Int by SPDelegates(Constants.PREF_ITEM_ADVANCED_OPTIONS, AdvancedOptions.ITEM_DEFAULT_OPTIONS)

  var libReferenceOptions: Int by SPDelegates(Constants.PREF_LIB_REF_OPTIONS, LibReferenceOptions.DEFAULT_OPTIONS)

  var snapshotOptions: Int by SPDelegates(Constants.PREF_SNAPSHOT_OPTIONS, SnapshotOptions.DEFAULT_OPTIONS)

  var repo: String by SPDelegates(Constants.PREF_RULES_REPO, Constants.REPO_GITLAB)

  var snapshotTimestamp: Long by SPDelegates(Constants.PREF_SNAPSHOT_TIMESTAMP, 0)

  var distributionUpdateTimestamp: Long by SPDelegates(Constants.PREF_DISTRIBUTION_UPDATE_TIMESTAMP, 0)

  var debugMode: Boolean by SPDelegates(Constants.PREF_DEBUG_MODE, false)

  var snapshotKeep: String by SPDelegates(Constants.PREF_SNAPSHOT_KEEP, Constants.SNAPSHOT_DEFAULT)

  var darkMode: String by SPDelegates(Constants.PREF_DARK_MODE, Constants.DARK_MODE_FOLLOW_SYSTEM)

  var libSortMode: Int by SPDelegates(Constants.PREF_LIB_SORT_MODE, MODE_SORT_BY_SIZE)

  var processMode: Boolean by SPDelegates(Constants.PREF_PROCESS_MODE, false)

  var libReferenceThreshold: Int by SPDelegates(Constants.PREF_LIB_REF_THRESHOLD, 2)

  val isShowSystemApps: Boolean
    get() = (advancedOptions and AdvancedOptions.SHOW_SYSTEM_APPS) > 0

  var isColorfulIcon: Boolean by SPDelegates(Constants.PREF_COLORFUL_ICON, true)

  val isAnonymousAnalyticsEnabled: Boolean by SPDelegates(Constants.PREF_ANONYMOUS_ANALYTICS, true)

  var isDetailedAbiChart: Boolean by SPDelegates(Constants.PREF_DETAILED_ABI_CHART, false)

  var preferredRuleLanguage: String by SPDelegates(Constants.PREF_RULE_LANGUAGE, "zh-Hans")

  val season by unsafeLazy { DateUtils.getCurrentSeason() }

  var locale: Locale = Locale.getDefault()
    get() {
      if (OsUtils.atLeastT()) {
        val systemSelectedLocale = SystemServices.localeManager.getApplicationLocales(LibCheckerApp.app.packageName)
        Timber.d("System selected locale: $systemSelectedLocale")
        val locale = systemSelectedLocale.get(0) ?: Locale.getDefault()
        if (locale != field) {
          field = locale
          getPreferences().edit { putString(Constants.PREF_LOCALE, locale.toLanguageTag()) }
        }
        return locale
      }
      val tag = getPreferences().getString(Constants.PREF_LOCALE, null)
      if (tag.isNullOrEmpty() || "SYSTEM" == tag) {
        return Locale.getDefault()
      }
      return Locale.forLanguageTag(tag)
    }
    set(value) {
      field = value
      getPreferences().edit { putString(Constants.PREF_LOCALE, value.toLanguageTag()) }
    }

  var uuid: String by SPDelegates(Constants.PREF_UUID, String())

  var isGitHubReachable = true

  var trackItemsChanged = false

  var snapshotAutoRemoveThreshold: Int by SPDelegates(Constants.PREF_SNAPSHOT_AUTO_REMOVE_THRESHOLD, -1)
}
