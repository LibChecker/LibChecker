package com.absinthe.libchecker.domain.settings.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.net.toUri
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.TwoStatePreference
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.URLManager
import com.absinthe.libchecker.domain.about.ui.AboutPageBuilder
import com.absinthe.libchecker.domain.home.presentation.HomeViewModel
import com.absinthe.libchecker.ui.base.BaseAlertDialogBuilder
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.openUrlInBrowser
import com.absinthe.libraries.utils.extensions.getBoolean
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber

class SettingsFragment : BaseSettingsFragment() {
  override val preferencesResource = R.xml.settings
  private val homeViewModel: HomeViewModel by activityViewModels()

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    super.onCreatePreferences(savedInstanceState, rootKey)
    findPreference<Preference>(APPEARANCE_PREFERENCE_KEY)?.bindDebouncedClick {
      (parentFragment as SettingsContainerFragment).openAppearance()
    }
    findPreference<TwoStatePreference>(Constants.PREF_APK_ANALYTICS)?.apply {
      setOnPreferenceChangeListener { _, newValue ->
        settingsViewModel.setApkAnalysisEnabled(newValue as Boolean).onFailure { e ->
          Timber.e(e)
          Toasty.showShort(requireContext(), e.toString())
        }
        recordPreferenceEvent(Constants.PREF_APK_ANALYTICS, newValue)
        true
      }
    }
    findPreference<ListPreference>(Constants.PREF_RULES_REPO)?.apply {
      summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
      setOnPreferenceChangeListener { _, newValue ->
        settingsViewModel.selectRemoteRulesRepository(newValue.toString())
        recordPreferenceEvent(Constants.PREF_RULES_REPO, newValue)
        true
      }
    }
    findPreference<ListPreference>(Constants.PREF_SNAPSHOT_KEEP)?.apply {
      summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
      setOnPreferenceChangeListener { _, newValue ->
        settingsViewModel.setSnapshotKeepRule(newValue.toString())
        true
      }
    }
    findPreference<Preference>(Constants.PREF_CLOUD_RULES)?.bindDebouncedClick {
      CloudRulesDialogFragment().show(
        childFragmentManager,
        CloudRulesDialogFragment::class.java.name
      )
      recordPreferenceEvent(Constants.PREF_CLOUD_RULES)
    }
    findPreference<Preference>(Constants.PREF_RELOAD_APPS)?.bindDebouncedClick {
      BaseAlertDialogBuilder(requireContext())
        .setTitle(R.string.dialog_title_reload_apps)
        .setMessage(R.string.dialog_subtitle_reload_apps)
        .setPositiveButton(android.R.string.ok) { _, _ ->
          homeViewModel.reloadApps()
          recordPreferenceEvent(Constants.PREF_RELOAD_APPS)
        }
        .setNegativeButton(android.R.string.cancel, null)
        .create()
        .show()
    }

    findPreference<Preference>(Constants.PREF_EXPORT_LOG)?.bindDebouncedClick {
      lifecycleScope.launch {
        val logShareIntent = settingsViewModel.buildLogShareIntent().getOrElse { e ->
          Timber.e(e)
          Toasty.showShort(requireContext(), e.toString())
          recordPreferenceEvent(Constants.PREF_EXPORT_LOG)
          return@launch
        } ?: return@launch

        runCatching {
          startActivity(Intent.createChooser(logShareIntent, getString(R.string.export_log)))
        }.onFailure { e ->
          Timber.e(e)
          Toasty.showShort(requireContext(), e.toString())
        }
        recordPreferenceEvent(Constants.PREF_EXPORT_LOG)
      }
    }

    findPreference<Preference>(Constants.PREF_EXPORT_APPS)?.bindDebouncedClick {
      ExportAppsDialogFragment().show(
        childFragmentManager,
        ExportAppsDialogFragment::class.java.name
      )
      recordPreferenceEvent(Constants.PREF_EXPORT_APPS)
    }

    findPreference<Preference>(Constants.PREF_ABOUT)?.apply {
      summary = "${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})"
      bindClick { AboutPageBuilder.start(requireContext()) }
    }
    findPreference<Preference>(Constants.PREF_GET_UPDATES)?.bindDebouncedClick {
      GetUpdatesDialogFragment().show(
        childFragmentManager,
        GetUpdatesDialogFragment::class.java.name
      )
      recordPreferenceEvent(Constants.PREF_GET_UPDATES)
    }
    findPreference<Preference>(Constants.PREF_GITHUB_API_TOKEN)?.let(::bindGitHubTokenPreference)
    findPreference<Preference>(Constants.PREF_TRANSLATION)?.bindClick {
      requireContext().openUrlInBrowser(URLManager.CROWDIN_PAGE)
    }
    findPreference<Preference>(Constants.PREF_HELP)?.bindClick {
      requireContext().openUrlInBrowser(URLManager.DOCS_PAGE)
    }
    findPreference<Preference>(Constants.PREF_RATE)?.bindClick {
      try {
        startActivity(
          Intent(Intent.ACTION_VIEW).apply {
            data = URLManager.PLAY_STORE_DETAIL_PAGE.toUri()
          }
        )
        recordPreferenceEvent(Constants.PREF_RATE)
      } catch (e: ActivityNotFoundException) {
        Timber.e(e)
      }
    }
    findPreference<Preference>(Constants.PREF_TELEGRAM)?.bindClick {
      try {
        startActivity(
          Intent(Intent.ACTION_VIEW, URLManager.TELEGRAM_GROUP.toUri())
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        recordPreferenceEvent(Constants.PREF_TELEGRAM)
      } catch (e: ActivityNotFoundException) {
        Timber.e(e)
      }
    }
    findPreference<TwoStatePreference>(Constants.PREF_ANONYMOUS_ANALYTICS)?.isVisible =
      getBoolean(R.bool.is_foss).not()

    bindInlinePreferenceClickListeners()
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    if (savedInstanceState == null) prepareVisiblePreferenceRows()
    findPreference<Preference>(Constants.PREF_GET_UPDATES)?.let { preference ->
      settingsViewModel.updateBadgeVisible.onEach { visible ->
        isGetUpdatesBadgeVisible = visible
        rebindVisiblePreference(preference)
      }.launchIn(viewLifecycleOwner.lifecycleScope)
    }
  }

  private fun Preference.bindClick(action: () -> Unit) {
    setOnPreferenceClickListener { dispatchPreferenceClick(false, action) }
  }

  private fun Preference.bindDebouncedClick(action: () -> Unit) {
    setOnPreferenceClickListener {
      dispatchPreferenceClick(AntiShakeUtils.isInvalidClick(prefRecyclerView), action)
    }
  }

  private fun bindGitHubTokenPreference(preference: Preference) {
    updateGitHubTokenPreference(preference)
    preference.bindDebouncedClick {
      showGitHubTokenDialog(preference)
    }
  }

  private fun showGitHubTokenDialog(preference: Preference) {
    val tokenTextField = GitHubTokenTextFieldView(requireContext()).apply {
      layoutParams = FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      token = GlobalValues.githubApiToken
    }
    val container = FrameLayout(requireContext()).apply {
      setPadding(24.dp, 8.dp, 24.dp, 0)
      addView(tokenTextField)
    }

    BaseAlertDialogBuilder(requireContext())
      .setTitle(R.string.settings_github_token_dialog_title)
      .setMessage(R.string.settings_github_token_dialog_message)
      .setView(container)
      .setPositiveButton(android.R.string.ok) { _, _ ->
        GlobalValues.githubApiToken = tokenTextField.token
        updateGitHubTokenPreference(preference)
        recordPreferenceEvent(
          Constants.PREF_GITHUB_API_TOKEN,
          GlobalValues.githubApiToken.isNotEmpty()
        )
      }
      .setNegativeButton(android.R.string.cancel, null)
      .setNeutralButton(R.string.settings_github_token_clear) { _, _ ->
        GlobalValues.githubApiToken = String()
        updateGitHubTokenPreference(preference)
        recordPreferenceEvent(Constants.PREF_GITHUB_API_TOKEN, false)
      }
      .create()
      .show()
  }

  private fun updateGitHubTokenPreference(preference: Preference) {
    preference.summary = getString(
      if (GlobalValues.githubApiToken.isBlank()) {
        R.string.settings_github_token_summary_not_set
      } else {
        R.string.settings_github_token_summary_set
      }
    )
    rebindVisiblePreference(preference)
  }

  override fun onResume() {
    super.onResume()
    settingsViewModel.checkForUpdates()
  }
}
