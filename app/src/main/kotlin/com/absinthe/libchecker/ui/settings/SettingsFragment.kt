package com.absinthe.libchecker.ui.settings

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.Constants
import com.absinthe.libchecker.utils.GlobalValues

class SettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)

        val prefShowSystemApps = findPreference<SwitchPreferenceCompat>(Constants.PREF_SHOW_SYSTEM_APPS)
        prefShowSystemApps?.onPreferenceChangeListener = this
    }

    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
        if (preference?.key == Constants.PREF_SHOW_SYSTEM_APPS) {
            GlobalValues.isShowSystemApps = newValue as Boolean
            return true
        }
        return false
    }
}
