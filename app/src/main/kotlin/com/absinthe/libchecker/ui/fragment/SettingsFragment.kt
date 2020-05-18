package com.absinthe.libchecker.ui.fragment

import android.os.Bundle
import androidx.preference.DropDownPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues

class SettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)

        findPreference<SwitchPreferenceCompat>(Constants.PREF_SHOW_SYSTEM_APPS)?.apply {
            onPreferenceChangeListener = this@SettingsFragment
        }
        findPreference<DropDownPreference>(Constants.PREF_RULES_REPO)?.apply {
            onPreferenceChangeListener = this@SettingsFragment
        }

        findPreference<Preference>(Constants.PREF_ABOUT)?.apply {
            summary = "${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})"
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        if (preference.key == Constants.PREF_SHOW_SYSTEM_APPS) {
            GlobalValues.isShowSystemApps.value = newValue as Boolean
            return true
        } else if (preference.key == Constants.PREF_RULES_REPO) {
            GlobalValues.repo = newValue as String
            return true
        }
        return false
    }
}
