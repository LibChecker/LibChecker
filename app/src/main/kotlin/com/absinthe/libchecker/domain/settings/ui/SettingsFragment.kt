package com.absinthe.libchecker.domain.settings.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnAttach
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroupAdapter
import androidx.preference.PreferenceScreen
import androidx.preference.PreferenceViewHolder
import androidx.preference.TwoStatePreference
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.URLManager
import com.absinthe.libchecker.domain.about.ui.AboutPageBuilder
import com.absinthe.libchecker.domain.home.presentation.HomeViewModel
import com.absinthe.libchecker.domain.settings.model.LocalePreferenceSummary
import com.absinthe.libchecker.domain.settings.presentation.SettingsViewModel
import com.absinthe.libchecker.ui.base.BaseAlertDialogBuilder
import com.absinthe.libchecker.ui.base.IAppBarContainer
import com.absinthe.libchecker.ui.base.IListController
import com.absinthe.libchecker.ui.base.IListControllerHost
import com.absinthe.libchecker.ui.base.ThemeTransitionController
import com.absinthe.libchecker.ui.preference.applyM3eLayoutResources
import com.absinthe.libchecker.ui.preference.buildPreferenceItemRenderState
import com.absinthe.libchecker.ui.preference.findPreferencePosition
import com.absinthe.libchecker.ui.preference.model.PreferenceInlineControl
import com.absinthe.libchecker.ui.preference.view.PreferenceItemView
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.Telemetry
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libraries.utils.extensions.getBoolean
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import rikka.recyclerview.fixEdgeEffect
import rikka.widget.borderview.BorderRecyclerView
import rikka.widget.borderview.BorderView
import rikka.widget.borderview.BorderViewDelegate
import timber.log.Timber

class SettingsFragment :
  PreferenceFragmentCompat(),
  IListController {

  private companion object {
    const val STATE_EXPANDED_PREFERENCE_KEY = "expanded_preference_key"

    val NAVIGATION_PREFERENCE_KEYS = setOf(
      Constants.PREF_ABOUT,
      Constants.PREF_TRANSLATION,
      Constants.PREF_HELP,
      Constants.PREF_RATE,
      Constants.PREF_TELEGRAM
    )

    val INLINE_CHOICE_PREFERENCE_KEYS = setOf(
      Constants.PREF_DARK_MODE,
      Constants.PREF_SNAPSHOT_KEEP,
      Constants.PREF_RULES_REPO
    )

    val INLINE_PREFERENCE_KEYS =
      INLINE_CHOICE_PREFERENCE_KEYS + Constants.PREF_LIB_REF_THRESHOLD

    val DRAGGABLE_CHOICE_PREFERENCE_KEYS = setOf(
      Constants.PREF_DARK_MODE,
      Constants.PREF_SNAPSHOT_KEEP,
      Constants.PREF_RULES_REPO
    )
  }

  private lateinit var borderViewDelegate: BorderViewDelegate
  private lateinit var prefRecyclerView: RecyclerView
  private val homeViewModel: HomeViewModel by activityViewModels()
  private val settingsViewModel: SettingsViewModel by viewModel()
  private var isGetUpdatesBadgeVisible = false
  private var expandedPreferenceKey: String? = null
  private var libReferenceThreshold = LIB_REFERENCE_THRESHOLD_MIN
  private var navigationView: View? = null
  private var navigationLayoutChangeListener: View.OnLayoutChangeListener? = null

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    expandedPreferenceKey = savedInstanceState?.getString(STATE_EXPANDED_PREFERENCE_KEY)
    setPreferencesFromResource(R.xml.settings, null)
    preferenceScreen.applyM3eLayoutResources()
    libReferenceThreshold = normalizeLibReferenceThreshold(
      settingsViewModel.getLibReferenceThreshold()
    )

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
    findPreference<TwoStatePreference>(Constants.PREF_COLORFUL_ICON)?.apply {
      setOnPreferenceChangeListener { _, newValue ->
        settingsViewModel.setColorfulRuleIcon(newValue as Boolean)
        recordPreferenceEvent(Constants.PREF_COLORFUL_ICON, newValue)
        true
      }
    }
    findPreference<TwoStatePreference>(Constants.PREF_BLUR_DESIGN)?.apply {
      isVisible = OsUtils.atLeastT()
      setOnPreferenceChangeListener { preference, newValue ->
        val enabled = newValue as Boolean
        val blurPreference = preference as TwoStatePreference
        GlobalValues.isBlurDesign = enabled
        val hostActivity = activity
        if (hostActivity is AppCompatActivity) {
          ThemeTransitionController.recreateWithTransition(hostActivity)
        } else {
          blurPreference.isChecked = enabled
          (hostActivity as? IAppBarContainer)?.setBlurDesignEnabled(enabled)
        }
        recordPreferenceEvent(Constants.PREF_BLUR_DESIGN, enabled)
        false
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
      }!!
    findPreference<ListPreference>(Constants.PREF_SNAPSHOT_KEEP)?.apply {
      summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
      setOnPreferenceChangeListener { _, newValue ->
        settingsViewModel.setSnapshotKeepRule(newValue.toString())
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
    findPreference<Preference>(Constants.PREF_CLOUD_RULES)?.apply {
      setOnPreferenceClickListener {
        if (AntiShakeUtils.isInvalidClick(prefRecyclerView)) {
          false
        } else {
          CloudRulesDialogFragment().show(
            childFragmentManager,
            CloudRulesDialogFragment::class.java.name
          )
          recordPreferenceEvent(Constants.PREF_CLOUD_RULES)
          true
        }
      }
    }
    findPreference<Preference>(Constants.PREF_RELOAD_APPS)?.apply {
      setOnPreferenceClickListener {
        if (AntiShakeUtils.isInvalidClick(prefRecyclerView)) {
          false
        } else {
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
          true
        }
      }
    }

    findPreference<Preference>(Constants.PREF_EXPORT_LOG)?.apply {
      setOnPreferenceClickListener {
        if (AntiShakeUtils.isInvalidClick(prefRecyclerView)) {
          false
        } else {
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
          true
        }
      }
    }

    findPreference<Preference>(Constants.PREF_EXPORT_APPS)?.apply {
      setOnPreferenceClickListener {
        if (AntiShakeUtils.isInvalidClick(prefRecyclerView)) {
          false
        } else {
          ExportAppsDialogFragment().show(
            childFragmentManager,
            ExportAppsDialogFragment::class.java.name
          )
          recordPreferenceEvent(Constants.PREF_EXPORT_APPS)
          true
        }
      }
    }

    findPreference<Preference>(Constants.PREF_ABOUT)?.apply {
      summary = "${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})"
      setOnPreferenceClickListener {
        AboutPageBuilder.start(requireContext())
        true
      }
    }
    findPreference<Preference>(Constants.PREF_GET_UPDATES)?.apply {
      setOnPreferenceClickListener {
        if (AntiShakeUtils.isInvalidClick(prefRecyclerView)) {
          false
        } else {
          GetUpdatesDialogFragment().show(
            childFragmentManager,
            GetUpdatesDialogFragment::class.java.name
          )
          recordPreferenceEvent(Constants.PREF_GET_UPDATES)
          true
        }
      }
    }
    findPreference<Preference>(Constants.PREF_GITHUB_API_TOKEN)?.let(::bindGitHubTokenPreference)
    findPreference<Preference>(Constants.PREF_TRANSLATION)?.apply {
      setOnPreferenceClickListener {
        runCatching {
          CustomTabsIntent.Builder().build().apply {
            launchUrl(requireActivity(), URLManager.CROWDIN_PAGE.toUri())
          }
        }.onFailure {
          Timber.e(it)
          runCatching {
            val intent = Intent(Intent.ACTION_VIEW)
              .setData(URLManager.CROWDIN_PAGE.toUri())
            requireActivity().startActivity(intent)
          }.onFailure { inner ->
            Timber.e(inner)
            Toasty.showShort(requireActivity(), "No browser application")
          }
        }
        true
      }
    }
    findPreference<Preference>(Constants.PREF_HELP)?.apply {
      setOnPreferenceClickListener {
        runCatching {
          CustomTabsIntent.Builder().build().apply {
            launchUrl(requireActivity(), URLManager.DOCS_PAGE.toUri())
          }
        }.onFailure {
          Timber.e(it)
          runCatching {
            val intent = Intent(Intent.ACTION_VIEW)
              .setData(URLManager.DOCS_PAGE.toUri())
            requireActivity().startActivity(intent)
          }.onFailure { inner ->
            Timber.e(inner)
            Toasty.showShort(requireActivity(), "No browser application")
          }
        }
        true
      }
    }
    findPreference<Preference>(Constants.PREF_RATE)?.apply {
      setOnPreferenceClickListener {
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
        true
      }
    }
    findPreference<Preference>(Constants.PREF_TELEGRAM)?.apply {
      setOnPreferenceClickListener {
        try {
          startActivity(
            Intent(Intent.ACTION_VIEW, URLManager.TELEGRAM_GROUP.toUri())
              .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          )
          recordPreferenceEvent(Constants.PREF_TELEGRAM)
        } catch (e: ActivityNotFoundException) {
          Timber.e(e)
        }
        true
      }
    }
    findPreference<TwoStatePreference>(Constants.PREF_ANONYMOUS_ANALYTICS)?.isVisible =
      getBoolean(R.bool.is_foss).not()

    bindInlinePreferenceClickListeners()
    bindLocalePreference(languagePreference)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    expandedPreferenceKey?.let {
      outState.putString(STATE_EXPANDED_PREFERENCE_KEY, it)
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    findPreference<Preference>(Constants.PREF_GET_UPDATES)?.let { preference ->
      settingsViewModel.updateBadgeVisible.onEach { visible ->
        isGetUpdatesBadgeVisible = visible
        rebindVisiblePreference(preference)
      }.launchIn(viewLifecycleOwner.lifecycleScope)
    }
  }

  private fun bindGitHubTokenPreference(preference: Preference) {
    updateGitHubTokenPreference(preference)
    preference.setOnPreferenceClickListener {
      if (AntiShakeUtils.isInvalidClick(prefRecyclerView)) {
        false
      } else {
        showGitHubTokenDialog(preference)
        true
      }
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

  private fun rebindVisiblePreference(preference: Preference) {
    rebindVisiblePreference(preference, animateExpansion = false)
  }

  private fun rebindVisiblePreference(
    preference: Preference,
    animateExpansion: Boolean
  ) {
    if (!::prefRecyclerView.isInitialized) {
      return
    }

    prefRecyclerView.post {
      val adapter = prefRecyclerView.adapter as? PreferenceGroupAdapter ?: return@post
      val position = adapter.findPreferencePosition(preference) ?: return@post
      val itemView = prefRecyclerView.findViewHolderForAdapterPosition(position)?.itemView
        as? PreferenceItemView ?: return@post
      bindSettingsPreferenceItem(adapter, position, itemView, animateExpansion)
    }
  }

  private fun bindInlinePreferenceClickListeners() {
    INLINE_PREFERENCE_KEYS.forEach { key ->
      findPreference<Preference>(key)?.setOnPreferenceClickListener { preference ->
        toggleInlinePreference(preference)
        true
      }
    }
  }

  private fun toggleInlinePreference(preference: Preference) {
    val previousKey = expandedPreferenceKey
    val nextKey = preference.key.takeUnless { it == previousKey }
    expandedPreferenceKey = nextKey

    previousKey
      ?.takeUnless { it == nextKey }
      ?.let { findPreference<Preference>(it) }
      ?.let { rebindVisiblePreference(it, animateExpansion = true) }
    nextKey
      ?.takeUnless { it == previousKey }
      ?.let { findPreference<Preference>(it) }
      ?.let { rebindVisiblePreference(it, animateExpansion = true) }
  }

  private fun buildInlineControl(preference: Preference): PreferenceInlineControl? {
    if (preference is ListPreference && preference.key in INLINE_CHOICE_PREFERENCE_KEYS) {
      val entries = preference.entries.map(CharSequence::toString)
      val entryValues = preference.entryValues.map(CharSequence::toString)
      if (preference.key == Constants.PREF_DARK_MODE) {
        return PreferenceInlineControl.IconSegmentedChoice(
          accessibilityLabels = entries,
          entryValues = entryValues,
          iconResIds = entryValues.map(::darkModePreferenceIconRes),
          selectedValue = preference.value,
          deferSelectionUntilAnimationEnd = true
        )
      }
      if (preference.key == Constants.PREF_SNAPSHOT_KEEP) {
        return PreferenceInlineControl.DraggableChoice(
          entries = entries,
          entryValues = entryValues,
          selectedValue = preference.value
        )
      }
      if (preference.key == Constants.PREF_RULES_REPO) {
        return PreferenceInlineControl.IconSegmentedChoice(
          accessibilityLabels = entries,
          entryValues = entryValues,
          iconResIds = entryValues.map {
            when (it) {
              Constants.REPO_GITHUB -> R.drawable.ic_github
              Constants.REPO_GITLAB -> R.drawable.ic_gitlab
              else -> R.drawable.ic_repository
            }
          },
          selectedValue = preference.value
        )
      }
      return null
    }
    if (preference.key == Constants.PREF_LIB_REF_THRESHOLD) {
      return PreferenceInlineControl.Range(
        value = libReferenceThreshold,
        valueFrom = LIB_REFERENCE_THRESHOLD_MIN,
        valueTo = LIB_REFERENCE_THRESHOLD_MAX
      )
    }
    return null
  }

  private fun selectInlineChoice(
    preferenceKey: String,
    value: String
  ) {
    val preference = findPreference<ListPreference>(preferenceKey) ?: return
    if (preference.value == value || !preference.callChangeListener(value)) {
      return
    }
    if (preferenceKey == Constants.PREF_DARK_MODE) {
      return
    }
    preference.value = value
    if (preferenceKey in DRAGGABLE_CHOICE_PREFERENCE_KEYS) {
      return
    }
    rebindVisiblePreference(preference)
  }

  private fun selectLibReferenceThreshold(value: Int) {
    val normalizedValue = normalizeLibReferenceThreshold(value)
    if (normalizedValue == libReferenceThreshold) {
      return
    }
    libReferenceThreshold = normalizedValue
    settingsViewModel.setLibReferenceThreshold(normalizedValue)
    recordPreferenceEvent(Constants.PREF_LIB_REF_THRESHOLD, normalizedValue.toLong())
    findPreference<Preference>(Constants.PREF_LIB_REF_THRESHOLD)?.let(::rebindVisiblePreference)
  }

  private fun bindLocalePreference(languagePreference: ListPreference) {
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

  override fun onResume() {
    super.onResume()
    settingsViewModel.checkForUpdates()
    val container = (activity as? IAppBarContainer) ?: return
    (activity as? IListControllerHost)?.setListController(this)
    scheduleAppbarRaisingStatus(!getBorderViewDelegate().isShowingTopBorder)
    container.setLiftOnScrollTargetView(prefRecyclerView)
  }

  override fun onCreateRecyclerView(
    inflater: LayoutInflater,
    parent: ViewGroup,
    savedInstanceState: Bundle?
  ): RecyclerView {
    val recyclerView =
      super.onCreateRecyclerView(inflater, parent, savedInstanceState) as BorderRecyclerView
    recyclerView.id = android.R.id.list
    recyclerView.fixEdgeEffect()
    recyclerView.overScrollMode = RecyclerView.OVER_SCROLL_NEVER
    recyclerView.isVerticalScrollBarEnabled = false
    (activity as? IAppBarContainer)?.prepareAppbarContentInset(recyclerView)
    recyclerView.applySettingsBottomPadding()

    val lp = recyclerView.layoutParams
    if (lp is FrameLayout.LayoutParams) {
      lp.rightMargin = recyclerView.context.resources.getDimension(R.dimen.normal_padding).toInt()
      lp.leftMargin = lp.rightMargin
    }

    borderViewDelegate = recyclerView.borderViewDelegate
    borderViewDelegate.borderVisibilityChangedListener =
      BorderView.OnBorderVisibilityChangedListener { top: Boolean, _: Boolean, _: Boolean, _: Boolean ->
        scheduleAppbarRaisingStatus(!top)
      }

    prefRecyclerView = recyclerView
    return recyclerView
  }

  override fun onCreateAdapter(preferenceScreen: PreferenceScreen): RecyclerView.Adapter<*> {
    return object : PreferenceGroupAdapter(preferenceScreen) {
      override fun onBindViewHolder(
        holder: PreferenceViewHolder,
        position: Int
      ) {
        super.onBindViewHolder(holder, position)
        (holder.itemView as? PreferenceItemView)?.let {
          bindSettingsPreferenceItem(this, position, it)
        }
      }
    }
  }

  private fun scheduleAppbarRaisingStatus(isLifted: Boolean) {
    val host = activity as? IListControllerHost ?: return
    if (host.isCurrentListController(this)) {
      (activity as? IAppBarContainer)?.scheduleAppbarLiftingStatus(isLifted)
    }
  }

  override fun onDetach() {
    super.onDetach()
    (activity as? IListControllerHost)?.clearListController(this)
  }

  override fun onDestroyView() {
    navigationLayoutChangeListener?.let { listener ->
      navigationView?.removeOnLayoutChangeListener(listener)
    }
    navigationLayoutChangeListener = null
    navigationView = null
    super.onDestroyView()
  }

  override fun onReturnTop() {
    // Do nothing
  }

  override fun getBorderViewDelegate(): BorderViewDelegate = borderViewDelegate
  override fun isAllowRefreshing(): Boolean = true
  override fun getSuitableLayoutManager(): RecyclerView.LayoutManager? = null

  private fun bindSettingsPreferenceItem(
    adapter: PreferenceGroupAdapter,
    position: Int,
    itemView: PreferenceItemView,
    animateExpansion: Boolean = false
  ) {
    val state = adapter.buildPreferenceItemRenderState(
      position = position,
      showChevron = { it.key in NAVIGATION_PREFERENCE_KEYS },
      badgeDescription = {
        if (it.key == Constants.PREF_GET_UPDATES && isGetUpdatesBadgeVisible) {
          getString(R.string.settings_update_available)
        } else {
          null
        }
      },
      inlineControl = ::buildInlineControl,
      expanded = { it.key == expandedPreferenceKey }
    ) ?: return
    itemView.bind(
      state = state,
      animateExpansion = animateExpansion,
      onChoiceSelected = { value ->
        state.preferenceKey?.let { selectInlineChoice(it, value) }
      },
      onRangeValueChangeFinished = ::selectLibReferenceThreshold
    )
  }

  private fun RecyclerView.applySettingsBottomPadding() {
    val basePadding = resources.getDimensionPixelSize(R.dimen.settings_list_vertical_padding)
    val appNavigationView = activity?.findViewById<View>(R.id.nav_view)
    var systemBarBottomInset = 0

    fun updateBottomPadding() {
      val bottomNavigationHeight = (appNavigationView as? BottomNavigationView)
        ?.height
        ?.takeIf { it > 0 }
      updatePadding(
        bottom = calculateSettingsBottomPadding(
          basePadding = basePadding,
          systemBarBottomInset = systemBarBottomInset,
          bottomNavigationHeight = bottomNavigationHeight
        )
      )
    }

    ViewCompat.setOnApplyWindowInsetsListener(this) { _, windowInsets ->
      systemBarBottomInset =
        windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
      updateBottomPadding()
      windowInsets
    }

    val listener =
      View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updateBottomPadding() }
    appNavigationView?.addOnLayoutChangeListener(listener)
    navigationView = appNavigationView
    navigationLayoutChangeListener = listener

    doOnAttach {
      ViewCompat.requestApplyInsets(it)
      updateBottomPadding()
    }
  }

  private fun recordPreferenceEvent(key: String, value: Any = "") {
    Telemetry.recordEvent(
      Constants.Event.SETTINGS,
      mapOf(Telemetry.Param.CONTENT to key, Telemetry.Param.VALUE to value)
    )
  }
}
