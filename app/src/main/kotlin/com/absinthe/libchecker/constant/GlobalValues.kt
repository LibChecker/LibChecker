package com.absinthe.libchecker.constant

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.MutableLiveData
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.api.bean.Configuration
import com.absinthe.libchecker.ui.fragment.applist.MODE_SORT_BY_SIZE
import com.blankj.utilcode.util.Utils

const val SP_NAME = "${BuildConfig.APPLICATION_ID}_preferences"

object GlobalValues {

    private val preferences: SharedPreferences = Utils.getApp()
        .getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)

    private fun getPreferences(): SharedPreferences {
        return preferences
    }

    var config = Configuration(
        enableLibDetail = false,
        enableComponentsDetail = false,
        showLibName = false,
        showTeamName = false,
        showContributor = false,
        showLibDescription = false,
        showRelativeUrl = false
    )

    var repo = getPreferences().getString(Constants.PREF_RULES_REPO, Constants.REPO_GITEE)
        ?: Constants.REPO_GITEE

    var shouldRequestChange: MutableLiveData<Boolean> = MutableLiveData(true)

    var isShowSystemApps: MutableLiveData<Boolean> =
        MutableLiveData(getPreferences().getBoolean(Constants.PREF_SHOW_SYSTEM_APPS, false))
    var isShowEntryAnimation: MutableLiveData<Boolean> =
        MutableLiveData(getPreferences().getBoolean(Constants.PREF_ENTRY_ANIMATION, true))
    var isColorfulIcon: MutableLiveData<Boolean> =
        MutableLiveData(getPreferences().getBoolean(Constants.PREF_COLORFUL_ICON, true))

    var appSortMode: MutableLiveData<Int> = MutableLiveData(
        getPreferences().getInt(
            Constants.PREF_APP_SORT_MODE,
            Constants.SORT_MODE_DEFAULT
        )
    )
    var libSortMode: MutableLiveData<Int> =
        MutableLiveData(getPreferences().getInt(Constants.PREF_LIB_SORT_MODE, MODE_SORT_BY_SIZE))
    var libReferenceThreshold: MutableLiveData<Int> =
        MutableLiveData(getPreferences().getInt(Constants.PREF_LIB_REF_THRESHOLD, 2))

    var isObservingDBItems: MutableLiveData<Boolean> = MutableLiveData(true)
    var snapshotTimestamp: Long = 0
        get() = getPreferences().getLong(Constants.PREF_SNAPSHOT_TIMESTAMP, 0)
        set(value) {
            field = value
            getPreferences().edit { putLong(Constants.PREF_SNAPSHOT_TIMESTAMP, value) }
        }
}