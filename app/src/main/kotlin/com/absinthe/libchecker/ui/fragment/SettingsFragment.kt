package com.absinthe.libchecker.ui.fragment

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.preference.DropDownPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.URLManager
import com.absinthe.libchecker.ui.detail.ApkDetailActivity
import com.absinthe.libchecker.utils.AppUtils
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.view.dialogfragment.LibThresholdDialogFragment
import com.blankj.utilcode.util.BarUtils
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.analytics.EventProperties

class SettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener,
    Preference.OnPreferenceClickListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)

        findPreference<SwitchPreferenceCompat>(Constants.PREF_SHOW_SYSTEM_APPS)?.apply {
            onPreferenceChangeListener = this@SettingsFragment
        }
        findPreference<SwitchPreferenceCompat>(Constants.PREF_ENTRY_ANIMATION)?.apply {
            onPreferenceChangeListener = this@SettingsFragment
        }
        findPreference<SwitchPreferenceCompat>(Constants.PREF_APK_ANALYTICS)?.apply {
            onPreferenceChangeListener = this@SettingsFragment
        }
        findPreference<SwitchPreferenceCompat>(Constants.PREF_COLORFUL_ICON)?.apply {
            onPreferenceChangeListener = this@SettingsFragment
        }
        findPreference<DropDownPreference>(Constants.PREF_RULES_REPO)?.apply {
            onPreferenceChangeListener = this@SettingsFragment
        }
        findPreference<Preference>(Constants.PREF_LIB_REF_THRESHOLD)?.apply {
            onPreferenceClickListener = this@SettingsFragment
        }

        findPreference<Preference>(Constants.PREF_ABOUT)?.apply {
            summary = "${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})"
        }
        findPreference<Preference>(Constants.PREF_RATE)?.apply {
            onPreferenceClickListener = this@SettingsFragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listView.setPadding(
            0,
            UiUtils.getActionBarSize(requireActivity()) + BarUtils.getStatusBarHeight(),
            0,
            0
        )
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        return when (preference.key) {
            Constants.PREF_SHOW_SYSTEM_APPS -> {
                GlobalValues.isShowSystemApps.value = newValue as Boolean
                Analytics.trackEvent(Constants.Event.SETTINGS, EventProperties().set("PREF_SHOW_SYSTEM_APPS", newValue))
                true
            }
            Constants.PREF_RULES_REPO -> {
                GlobalValues.repo = newValue as String
                AppUtils.requestConfiguration()
                Analytics.trackEvent(Constants.Event.SETTINGS, EventProperties().set("PREF_RULES_REPO", newValue))
                true
            }
            Constants.PREF_ENTRY_ANIMATION -> {
                GlobalValues.isShowEntryAnimation.value = newValue as Boolean
                Analytics.trackEvent(Constants.Event.SETTINGS, EventProperties().set("PREF_ENTRY_ANIMATION", newValue))
                true
            }
            Constants.PREF_APK_ANALYTICS -> {
                val flag = if (newValue as Boolean) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                }

                requireActivity().packageManager.setComponentEnabledSetting(
                    ComponentName(requireActivity(), ApkDetailActivity::class.java),
                    flag, PackageManager.DONT_KILL_APP
                )
                Analytics.trackEvent(Constants.Event.SETTINGS, EventProperties().set("PREF_APK_ANALYTICS", newValue))
                true
            }
            Constants.PREF_COLORFUL_ICON -> {
                GlobalValues.isColorfulIcon.value = newValue as Boolean
                Analytics.trackEvent(Constants.Event.SETTINGS, EventProperties().set("PREF_COLORFUL_ICON", newValue))
                true
            }
            else -> false
        }
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        return when (preference.key) {
            Constants.PREF_LIB_REF_THRESHOLD -> {
                LibThresholdDialogFragment().show(requireActivity().supportFragmentManager, tag)
                true
            }
            Constants.PREF_RATE -> {
                val hasInstallCoolApk = com.blankj.utilcode.util.AppUtils.isAppInstalled("com.coolapk.market")
                val marketUrl = if (hasInstallCoolApk) {
                    URLManager.COOLAPK_APP_PAGE
                } else {
                    URLManager.MARKET_PAGE
                }
                startActivity(Intent.parseUri(marketUrl, 0))
                Analytics.trackEvent(Constants.Event.SETTINGS, EventProperties().set("PREF_RATE", "Clicked"))
                true
            }
            else -> false
        }
    }
}
