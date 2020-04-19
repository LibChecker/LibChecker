package com.absinthe.libchecker.ui.settings

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.Constants
import com.absinthe.libchecker.utils.GlobalValues
import com.absinthe.libchecker.utils.SPUtils
import com.absinthe.libchecker.viewmodel.AppViewModel

class SettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener {

    private lateinit var viewModel: AppViewModel

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)
        viewModel = ViewModelProvider(requireActivity()).get(AppViewModel::class.java)

        val prefShowSystemApps = findPreference<SwitchPreferenceCompat>(Constants.PREF_SHOW_SYSTEM_APPS)
        prefShowSystemApps?.onPreferenceChangeListener = this

        val prefAbout = findPreference<Preference>("about")
        prefAbout?.summary = "${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})"
    }

    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
        if (preference?.key == Constants.PREF_SHOW_SYSTEM_APPS) {
            GlobalValues.isShowSystemApps.value = newValue as Boolean
            SPUtils.putBoolean(requireContext(), Constants.PREF_SHOW_SYSTEM_APPS, newValue)
            return true
        }
        return false
    }
}
