package com.absinthe.libchecker.constant

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.core.content.edit
import androidx.lifecycle.MutableLiveData
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.LibCheckerApp
import com.absinthe.libchecker.ui.fragment.applist.MODE_SORT_BY_SIZE
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.PackageUtils

const val SP_NAME = "${BuildConfig.APPLICATION_ID}_preferences"

object GlobalValues {

    private val preferences: SharedPreferences = LibCheckerApp.context
        .getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)

    private fun getPreferences(): SharedPreferences {
        return preferences
    }

    var repo = getPreferences().getString(Constants.PREF_RULES_REPO, Constants.REPO_GITEE)
        ?: Constants.REPO_GITEE

    var snapshotTimestamp: Long = 0
        get() = getPreferences().getLong(Constants.PREF_SNAPSHOT_TIMESTAMP, 0)
        set(value) {
            field = value
            getPreferences().edit { putLong(Constants.PREF_SNAPSHOT_TIMESTAMP, value) }
        }

    var localRulesVersion: Int = RULES_VERSION
        get() = getPreferences().getInt(Constants.PREF_LOCAL_RULES_VERSION, RULES_VERSION)
        set(value) {
            field = value
            getPreferences().edit { putInt(Constants.PREF_LOCAL_RULES_VERSION, value) }
        }

    val shouldRequestChange: MutableLiveData<Boolean> = MutableLiveData(true)

    val isShowSystemApps: MutableLiveData<Boolean> =
        MutableLiveData(getPreferences().getBoolean(Constants.PREF_SHOW_SYSTEM_APPS, false))
    val isShowEntryAnimation: MutableLiveData<Boolean> =
        MutableLiveData(getPreferences().getBoolean(Constants.PREF_ENTRY_ANIMATION, false))
    val isColorfulIcon: MutableLiveData<Boolean> =
        MutableLiveData(getPreferences().getBoolean(Constants.PREF_COLORFUL_ICON, true))
    val isAnonymousAnalyticsEnabled: MutableLiveData<Boolean> =
        MutableLiveData(getPreferences().getBoolean(Constants.PREF_ANONYMOUS_ANALYTICS, true))

    val appSortMode: MutableLiveData<Int> = MutableLiveData(
        getPreferences().getInt(
            Constants.PREF_APP_SORT_MODE,
            Constants.SORT_MODE_DEFAULT
        )
    )
    val libSortMode: MutableLiveData<Int> =
        MutableLiveData(getPreferences().getInt(Constants.PREF_LIB_SORT_MODE, MODE_SORT_BY_SIZE))
    val libReferenceThreshold: MutableLiveData<Int> =
        MutableLiveData(getPreferences().getInt(Constants.PREF_LIB_REF_THRESHOLD, 2))

    val season = LCAppUtils.getCurrentSeason()

    val deviceSupportedAbis = if (PackageUtils.isIntelCpu()) {
        Build.SUPPORTED_ABIS.filter { it.startsWith("x86") }
    } else {
        Build.SUPPORTED_ABIS.toList()
    }
}