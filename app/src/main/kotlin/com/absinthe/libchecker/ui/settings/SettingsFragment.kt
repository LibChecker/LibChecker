package com.absinthe.libchecker.ui.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.absinthe.libchecker.R

class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var notificationsViewModel: SettingsViewModel

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)
    }
}
