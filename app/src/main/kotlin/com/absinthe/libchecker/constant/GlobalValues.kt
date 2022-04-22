package com.absinthe.libchecker.constant

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.MutableLiveData
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.ui.fragment.detail.MODE_SORT_BY_SIZE
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.SPDelegates
import com.absinthe.libchecker.utils.SPUtils
import com.absinthe.rulesbundle.LCRules
import java.util.Locale

const val SP_NAME = "${BuildConfig.APPLICATION_ID}_preferences"

object GlobalValues {

  private fun getPreferences(): SharedPreferences {
    return SPUtils.sp
  }

  var repo: String by SPDelegates(Constants.PREF_RULES_REPO, Constants.REPO_GITEE)

  var snapshotTimestamp: Long by SPDelegates(Constants.PREF_SNAPSHOT_TIMESTAMP, 0)

  var localRulesVersion: Int by SPDelegates(Constants.PREF_LOCAL_RULES_VERSION, LCRules.getVersion())

  var currentLibRefType: Int by SPDelegates(Constants.CURRENT_LIB_REF_TYPE, NATIVE)

  var debugMode: Boolean by SPDelegates(Constants.PREF_DEBUG_MODE, false)

  var darkMode: String by SPDelegates(Constants.PREF_DARK_MODE, Constants.DARK_MODE_FOLLOW_SYSTEM)

  var rengeTheme: Boolean by SPDelegates(Constants.RENGE_THEME, false)

  var appSortMode: Int by SPDelegates(Constants.PREF_APP_SORT_MODE, Constants.SORT_MODE_DEFAULT)

  var libSortMode: Int by SPDelegates(Constants.PREF_LIB_SORT_MODE, MODE_SORT_BY_SIZE)

  var libReferenceThreshold: Int by SPDelegates(Constants.PREF_LIB_REF_THRESHOLD, 2)

  var md3Theme: Boolean by SPDelegates(Constants.PREF_MD3, false)

  val shouldRequestChange: MutableLiveData<Boolean> = MutableLiveData(false)

  val isShowSystemApps: MutableLiveData<Boolean> =
    MutableLiveData(getPreferences().getBoolean(Constants.PREF_SHOW_SYSTEM_APPS, false))

  val isColorfulIcon: MutableLiveData<Boolean> =
    MutableLiveData(getPreferences().getBoolean(Constants.PREF_COLORFUL_ICON, true))

  val isAnonymousAnalyticsEnabled: MutableLiveData<Boolean> =
    MutableLiveData(getPreferences().getBoolean(Constants.PREF_ANONYMOUS_ANALYTICS, true))

  val appSortModeLiveData: MutableLiveData<Int> = MutableLiveData(appSortMode)

  val libSortModeLiveData: MutableLiveData<Int> = MutableLiveData(libSortMode)

  val libReferenceThresholdLiveData: MutableLiveData<Int> = MutableLiveData(libReferenceThreshold)

  val season = LCAppUtils.getCurrentSeason()

  var locale: Locale = Locale.getDefault()
    get() {
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

  var uuid: String by SPDelegates(Constants.PREF_UUID, "")
}
