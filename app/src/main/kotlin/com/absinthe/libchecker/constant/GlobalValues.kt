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
import com.absinthe.libchecker.utils.extensions.unsafeLazy
import java.util.Locale

const val SP_NAME = "${BuildConfig.APPLICATION_ID}_preferences"

object GlobalValues {

  private fun getPreferences(): SharedPreferences {
    return SPUtils.sp
  }

  var advancedOptions: Int by SPDelegates(Constants.PREF_ADVANCED_OPTIONS, AdvancedOptions.DEFAULT_OPTIONS)
  val advancedOptionsLiveData: MutableLiveData<Int> = MutableLiveData(advancedOptions)

  var itemAdvancedOptions: Int by SPDelegates(Constants.PREF_ITEM_ADVANCED_OPTIONS, AdvancedOptions.ITEM_DEFAULT_OPTIONS)
  val itemAdvancedOptionsLiveData: MutableLiveData<Int> = MutableLiveData(itemAdvancedOptions)

  var libReferenceOptions: Int by SPDelegates(Constants.PREF_LIB_REF_OPTIONS, LibReferenceOptions.DEFAULT_OPTIONS)
  val libReferenceOptionsLiveData: MutableLiveData<Int> = MutableLiveData(libReferenceOptions)

  var repo: String by SPDelegates(Constants.PREF_RULES_REPO, Constants.REPO_GITLAB)

  var snapshotTimestamp: Long by SPDelegates(Constants.PREF_SNAPSHOT_TIMESTAMP, 0)

  var debugMode: Boolean by SPDelegates(Constants.PREF_DEBUG_MODE, false)

  var snapshotKeep: String by SPDelegates(Constants.PREF_SNAPSHOT_KEEP, Constants.SNAPSHOT_DEFAULT)

  var darkMode: String by SPDelegates(Constants.PREF_DARK_MODE, Constants.DARK_MODE_FOLLOW_SYSTEM)

  var rengeTheme: Boolean by SPDelegates(Constants.RENGE_THEME, false)

  var libSortMode: Int by SPDelegates(Constants.PREF_LIB_SORT_MODE, MODE_SORT_BY_SIZE)

  var processMode: Boolean by SPDelegates(Constants.PREF_PROCESS_MODE, false)

  var libReferenceThreshold: Int by SPDelegates(Constants.PREF_LIB_REF_THRESHOLD, 2)

  val isShowSystemApps: MutableLiveData<Boolean> =
    MutableLiveData((advancedOptions and AdvancedOptions.SHOW_SYSTEM_APPS) > 0)

  val isColorfulIcon: MutableLiveData<Boolean> =
    MutableLiveData(getPreferences().getBoolean(Constants.PREF_COLORFUL_ICON, true))

  val isAnonymousAnalyticsEnabled: MutableLiveData<Boolean> =
    MutableLiveData(getPreferences().getBoolean(Constants.PREF_ANONYMOUS_ANALYTICS, true))

  val libSortModeLiveData: MutableLiveData<Int> = MutableLiveData(libSortMode)

  val libReferenceThresholdLiveData: MutableLiveData<Int> = MutableLiveData(libReferenceThreshold)

  val season by unsafeLazy { LCAppUtils.getCurrentSeason() }

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

  var uuid: String by SPDelegates(Constants.PREF_UUID, String())

  var isGitHubUnreachable = true

  var trackItemsChanged = false
}
