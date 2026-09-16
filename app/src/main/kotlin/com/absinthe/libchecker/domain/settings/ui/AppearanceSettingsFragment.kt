package com.absinthe.libchecker.domain.settings.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.text.HtmlCompat
import androidx.preference.ListPreference
import androidx.preference.TwoStatePreference
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.domain.settings.model.LocalePreferenceSummary
import com.absinthe.libchecker.ui.base.IAppBarContainer
import com.absinthe.libchecker.ui.base.ThemeTransitionController
import com.absinthe.libchecker.utils.OsUtils
import timber.log.Timber

class AppearanceSettingsFragment : BaseSettingsFragment() {
  override val preferencesResource = R.xml.settings_appearance

  private companion object {
    const val TOGGLE_TRANSITION_DELAY_MS = 300L
  }

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    super.onCreatePreferences(savedInstanceState, rootKey)
    findPreference<TwoStatePreference>(Constants.PREF_COLORFUL_ICON)?.apply {
      setOnPreferenceChangeListener { _, newValue ->
        settingsViewModel.setColorfulRuleIcon(newValue as Boolean)
        recordPreferenceEvent(Constants.PREF_COLORFUL_ICON, newValue)
        true
      }
    }
    findPreference<TwoStatePreference>(Constants.PREF_AMOLED_THEME)?.apply {
      setOnPreferenceChangeListener { _, newValue ->
        val enabled = newValue as Boolean
        val hostActivity = activity
        GlobalValues.isAmoledTheme = enabled
        if (hostActivity is AppCompatActivity) {
          hostActivity.window.decorView.postDelayed({
            ThemeTransitionController.recreateWithTransition(hostActivity)
          }, TOGGLE_TRANSITION_DELAY_MS)
        }
        recordPreferenceEvent(Constants.PREF_AMOLED_THEME, enabled)
        true
      }
    }
    findPreference<TwoStatePreference>(Constants.PREF_BLUR_DESIGN)?.apply {
      isVisible = OsUtils.atLeastT()
      setOnPreferenceChangeListener { _, newValue ->
        val enabled = newValue as Boolean
        val hostActivity = activity
        GlobalValues.isBlurDesign = enabled
        // Blur only changes how the app bar and bottom bar are drawn, so swap it
        // in place. Recreating the window here made the whole screen flash.
        val appBarContainer = hostActivity as? IAppBarContainer
        if (appBarContainer != null) {
          hostActivity.window.decorView.postDelayed({
            if (GlobalValues.isBlurDesign == enabled) {
              appBarContainer.setBlurDesignEnabled(enabled)
            }
          }, TOGGLE_TRANSITION_DELAY_MS)
        } else if (hostActivity is AppCompatActivity) {
          hostActivity.window.decorView.postDelayed({
            ThemeTransitionController.recreateWithTransition(hostActivity)
          }, TOGGLE_TRANSITION_DELAY_MS)
        }
        recordPreferenceEvent(Constants.PREF_BLUR_DESIGN, enabled)
        true
      }
    }
    findPreference<TwoStatePreference>(Constants.PREF_FLOATING_NAV_BAR)?.apply {
      setOnPreferenceChangeListener { _, newValue ->
        val enabled = newValue as Boolean
        val hostActivity = activity
        GlobalValues.isFloatingNavBar = enabled
        val appBarContainer = hostActivity as? IAppBarContainer
        if (appBarContainer != null) {
          hostActivity.window.decorView.postDelayed({
            if (GlobalValues.isFloatingNavBar == enabled) {
              appBarContainer.setFloatingNavBarEnabled(enabled)
            }
          }, TOGGLE_TRANSITION_DELAY_MS)
        } else if (hostActivity is AppCompatActivity) {
          hostActivity.window.decorView.postDelayed({
            ThemeTransitionController.recreateWithTransition(hostActivity)
          }, TOGGLE_TRANSITION_DELAY_MS)
        }
        recordPreferenceEvent(Constants.PREF_FLOATING_NAV_BAR, enabled)
        true
      }
    }
    val languagePreference =
      findPreference<ListPreference>(Constants.PREF_LOCALE)?.apply {
        isVisible = !OsUtils.atLeastT()
        setOnPreferenceChangeListener { _, newValue ->
          if (newValue is String) {
            val locale = settingsViewModel.selectLocale(newValue)
            Timber.d("Locale = $locale")
            activity?.recreate()
          }
          true
        }
      }
    findPreference<ListPreference>(Constants.PREF_DARK_MODE)?.apply {
      summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
      setIcon(darkModePreferenceIconRes(value))
      setOnPreferenceChangeListener { _, newValue ->
        val selectedValue = newValue.toString()
        val nightMode = settingsViewModel.selectDarkMode(selectedValue)
        val applyPreferencePresentation = {
          value = selectedValue
          setIcon(darkModePreferenceIconRes(selectedValue))
        }
        val hostActivity = activity
        if (hostActivity is AppCompatActivity) {
          ThemeTransitionController.applyNightMode(
            activity = hostActivity,
            nightMode = nightMode,
            onWindowHidden = applyPreferencePresentation
          )
        } else {
          applyPreferencePresentation()
          AppCompatDelegate.setDefaultNightMode(nightMode)
        }
        true
      }
    }
    bindInlinePreferenceClickListeners()
    languagePreference?.let(::bindLocalePreference)
  }

  private fun bindLocalePreference(languagePreference: ListPreference) {
    if (!languagePreference.isVisible) return
    val tag = languagePreference.value
    val displayData = settingsViewModel.buildLocalePreferenceData(
      entries = languagePreference.entries.toList(),
      entryValues = languagePreference.entryValues.toList(),
      selectedTag = tag
    )
    Timber.d(
      "Locale = $tag, index = ${languagePreference.entryValues.indexOf(tag)}, " +
        "entries = ${languagePreference.entryValues.toList()}"
    )
    displayData.entries.forEach { entry ->
      languagePreference.entries[entry.index] = if (entry.selected) {
        entry.label
      } else {
        HtmlCompat.fromHtml(entry.label, HtmlCompat.FROM_HTML_MODE_LEGACY)
      }
    }
    when (val summary = displayData.summary) {
      LocalePreferenceSummary.FollowSystem -> {
        languagePreference.summary = getString(rikka.core.R.string.follow_system)
      }

      is LocalePreferenceSummary.LocaleName -> {
        languagePreference.summary = summary.name
      }

      LocalePreferenceSummary.Unchanged -> Unit
    }
  }
}
