package com.absinthe.libchecker.constant

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.absinthe.libchecker.api.bean.Configuration
import com.absinthe.libchecker.ui.fragment.applist.MODE_SORT_BY_SIZE
import com.absinthe.libchecker.utils.SPUtils

object GlobalValues {

    var isShowSystemApps: MutableLiveData<Boolean> = MutableLiveData()
    var isShowEntryAnimation: MutableLiveData<Boolean> = MutableLiveData()
    var isObservingDBItems: MutableLiveData<Boolean> = MutableLiveData()
    var appSortMode: MutableLiveData<Int> = MutableLiveData()
    var libSortMode: MutableLiveData<Int> = MutableLiveData()
    var libReferenceThreshold: MutableLiveData<Int> = MutableLiveData()

    var config = Configuration(
        enableLibDetail = false,
        showLibName = false,
        showTeamName = false,
        showLibDescription = false,
        showContributor = false,
        showRelativeUrl = false
    )

    var repo = Constants.REPO_GITHUB

    fun init(context: Context) {
        isShowSystemApps.value =
            SPUtils.getBoolean(
                context,
                Constants.PREF_SHOW_SYSTEM_APPS,
                false
            )
        isShowEntryAnimation.value =
            SPUtils.getBoolean(
                context,
                Constants.PREF_ENTRY_ANIMATION,
                true
            )
        isObservingDBItems.value = true
        appSortMode.value =
            SPUtils.getInt(
                context,
                Constants.PREF_APP_SORT_MODE,
                Constants.SORT_MODE_DEFAULT
            )
        libSortMode.value =
            SPUtils.getInt(
                context,
                Constants.PREF_LIB_SORT_MODE,
                MODE_SORT_BY_SIZE
            )
        libReferenceThreshold.value = SPUtils.getInt(
            context,
            Constants.PREF_LIB_REF_THRESHOLD,
            2
        )
        repo = SPUtils.getString(
            context,
            Constants.PREF_RULES_REPO,
            Constants.REPO_GITHUB
        ) ?: Constants.REPO_GITHUB
    }
}