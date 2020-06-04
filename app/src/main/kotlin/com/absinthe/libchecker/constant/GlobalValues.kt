package com.absinthe.libchecker.constant

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.api.bean.Configuration
import com.absinthe.libchecker.ui.fragment.applist.MODE_SORT_BY_SIZE

const val SP_NAME = "${BuildConfig.APPLICATION_ID}_preferences"

object GlobalValues {

    private var sPreferences: SharedPreferences? = null

    private fun getPreferences(): SharedPreferences {
        return sPreferences!!
    }

    var isShowSystemApps: MutableLiveData<Boolean> = MutableLiveData()
    var isShowEntryAnimation: MutableLiveData<Boolean> = MutableLiveData()
    var isObservingDBItems: MutableLiveData<Boolean> = MutableLiveData()
    var appSortMode: MutableLiveData<Int> = MutableLiveData()
    var libSortMode: MutableLiveData<Int> = MutableLiveData()
    var libReferenceThreshold: MutableLiveData<Int> = MutableLiveData()

    var config = Configuration(
        enableLibDetail = false,
        enableComponentsDetail = false,
        showLibName = false,
        showTeamName = false,
        showLibDescription = false,
        showContributor = false,
        showRelativeUrl = false
    )

    var repo = Constants.REPO_GITHUB

    var shouldRequestChange = false

    fun init(context: Context) {
        if (sPreferences == null) {
            sPreferences = context
                .getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
        }

        isObservingDBItems.value = true

        isShowSystemApps.value =
            getPreferences().getBoolean(Constants.PREF_SHOW_SYSTEM_APPS, false)
        isShowEntryAnimation.value =
            getPreferences().getBoolean(Constants.PREF_ENTRY_ANIMATION, true)
        appSortMode.value =
            getPreferences().getInt(Constants.PREF_APP_SORT_MODE, Constants.SORT_MODE_DEFAULT)
        libSortMode.value =
            getPreferences().getInt(Constants.PREF_LIB_SORT_MODE, MODE_SORT_BY_SIZE)
        libReferenceThreshold.value =
            getPreferences().getInt(Constants.PREF_LIB_REF_THRESHOLD, 2)

        repo = getPreferences().getString(Constants.PREF_RULES_REPO, Constants.REPO_GITHUB)
            ?: Constants.REPO_GITHUB
    }
}