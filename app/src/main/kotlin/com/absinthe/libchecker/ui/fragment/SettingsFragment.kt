package com.absinthe.libchecker.ui.fragment

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
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.view.dialogfragment.LibThresholdDialogFragment
import com.blankj.utilcode.util.BarUtils

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
        findPreference<DropDownPreference>(Constants.PREF_RULES_REPO)?.apply {
            onPreferenceChangeListener = this@SettingsFragment
        }
        findPreference<Preference>(Constants.PREF_LIB_REF_THRESHOLD)?.apply {
            onPreferenceClickListener = this@SettingsFragment
        }

        findPreference<Preference>(Constants.PREF_ABOUT)?.apply {
            summary = "${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})"
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
                true
            }
            Constants.PREF_RULES_REPO -> {
                GlobalValues.repo = newValue as String
                true
            }
            Constants.PREF_ENTRY_ANIMATION -> {
                GlobalValues.isShowEntryAnimation.value = newValue as Boolean
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
            else -> false
        }
    }
}
