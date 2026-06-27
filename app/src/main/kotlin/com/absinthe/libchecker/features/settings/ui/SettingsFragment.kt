package com.absinthe.libchecker.features.settings.ui

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatDelegate
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceGroupAdapter
import androidx.preference.SwitchPreferenceCompat
import androidx.preference.TwoStatePreference
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.URLManager
import com.absinthe.libchecker.domain.settings.LocalePreferenceSummary
import com.absinthe.libchecker.features.about.AboutPageBuilder
import com.absinthe.libchecker.features.home.HomeViewModel
import com.absinthe.libchecker.features.settings.SettingsViewModel
import com.absinthe.libchecker.ui.base.BaseAlertDialogBuilder
import com.absinthe.libchecker.ui.base.IAppBarContainer
import com.absinthe.libchecker.ui.base.IListController
import com.absinthe.libchecker.ui.base.IListControllerHost
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.Telemetry
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.utils.extensions.applySystemBarsPadding
import com.absinthe.libchecker.utils.extensions.doOnMainThreadIdle
import com.absinthe.libchecker.utils.extensions.setBottomPaddingSpace
import com.absinthe.libraries.utils.extensions.getBoolean
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import com.google.android.material.card.MaterialCardView
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
    val NAVIGATION_PREFERENCE_KEYS = setOf(
      Constants.PREF_ABOUT,
      Constants.PREF_TRANSLATION,
      Constants.PREF_HELP,
      Constants.PREF_RATE,
      Constants.PREF_TELEGRAM
    )
  }

  private lateinit var borderViewDelegate: BorderViewDelegate
  private lateinit var prefRecyclerView: RecyclerView
  private val homeViewModel: HomeViewModel by activityViewModels()
  private val settingsViewModel: SettingsViewModel by viewModel()

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    setPreferencesFromResource(R.xml.settings, null)
    preferenceScreen.applyM3eLayoutResources()

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
      setOnPreferenceChangeListener { _, newValue ->
        AppCompatDelegate.setDefaultNightMode(
          settingsViewModel.selectDarkMode(newValue.toString())
        )
        activity?.recreate()
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
    findPreference<Preference>(Constants.PREF_LIB_REF_THRESHOLD)?.apply {
      setOnPreferenceClickListener {
        if (AntiShakeUtils.isInvalidClick(prefRecyclerView)) {
          false
        } else {
          LibThresholdDialogFragment
            .newInstance(settingsViewModel.getLibReferenceThreshold())
            .apply {
              setOnThresholdSelectedListener(settingsViewModel::setLibReferenceThreshold)
            }
            .show(
              requireActivity().supportFragmentManager,
              LibThresholdDialogFragment::class.java.name
            )
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
    findPreference<TwoStatePreference>(Constants.PREF_ANONYMOUS_ANALYTICS)?.apply {
      isVisible = getBoolean(R.bool.is_foss).not()
    }

    bindLocalePreference(languagePreference)
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
    recyclerView.applySystemBarsPadding(bottom = true)

    doOnMainThreadIdle {
      recyclerView.setBottomPaddingSpace()
    }

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
    recyclerView.addOnChildAttachStateChangeListener(
      object : RecyclerView.OnChildAttachStateChangeListener {
        override fun onChildViewAttachedToWindow(view: View) {
          styleSettingsPreferenceItem(recyclerView, view)
        }

        override fun onChildViewDetachedFromWindow(view: View) = Unit
      }
    )
    return recyclerView
  }

  private fun scheduleAppbarRaisingStatus(isLifted: Boolean) {
    (activity as? IAppBarContainer)?.scheduleAppbarLiftingStatus(isLifted)
  }

  override fun onDetach() {
    super.onDetach()
    (activity as? IListControllerHost)?.clearListController(this)
  }

  override fun onReturnTop() {
    // Do nothing
  }

  override fun getBorderViewDelegate(): BorderViewDelegate = borderViewDelegate
  override fun isAllowRefreshing(): Boolean = true
  override fun getSuitableLayoutManager(): RecyclerView.LayoutManager? = null

  private fun Preference.applyM3eLayoutResources() {
    when (this) {
      is PreferenceCategory -> {
        layoutResource = R.layout.preference_category_m3e
        isIconSpaceReserved = false
      }

      is SwitchPreferenceCompat -> {
        layoutResource = R.layout.preference_m3e
        widgetLayoutResource = R.layout.preference_widget_material_switch
        isIconSpaceReserved = true
      }

      else -> {
        layoutResource = R.layout.preference_m3e
        isIconSpaceReserved = true
      }
    }

    if (this is PreferenceGroup) {
      for (index in 0 until preferenceCount) {
        getPreference(index).applyM3eLayoutResources()
      }
    }
  }

  @SuppressLint("RestrictedApi")
  private fun styleSettingsPreferenceItem(recyclerView: RecyclerView, itemView: View) {
    val adapter = recyclerView.adapter as? PreferenceGroupAdapter ?: return
    val position = recyclerView.getChildAdapterPosition(itemView)
    if (position == RecyclerView.NO_POSITION) return

    val preference = adapter.getItem(position)
    val card = itemView as? MaterialCardView ?: return
    if (!preference.isSettingsRowPreference()) return

    val previous = if (position > 0) adapter.getItem(position - 1) else null
    val next = if (position < adapter.itemCount - 1) adapter.getItem(position + 1) else null
    val isFirstInGroup = !previous.isSettingsRowPreference()
    val isLastInGroup = !next.isSettingsRowPreference()
    val outerRadius = resources.getDimension(R.dimen.settings_preference_corner_radius)
    val innerRadius = resources.getDimension(R.dimen.settings_preference_inner_corner_radius)
    val topRadius = if (isFirstInGroup) outerRadius else innerRadius
    val bottomRadius = if (isLastInGroup) outerRadius else innerRadius

    card.shapeAppearanceModel = card.shapeAppearanceModel.toBuilder()
      .setTopLeftCornerSize(topRadius)
      .setTopRightCornerSize(topRadius)
      .setBottomLeftCornerSize(bottomRadius)
      .setBottomRightCornerSize(bottomRadius)
      .build()

    itemView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
      topMargin = if (isFirstInGroup) {
        0
      } else {
        resources.getDimensionPixelSize(R.dimen.settings_preference_card_spacing)
      }
      bottomMargin = 0
    }

    val hasSwitch = preference is TwoStatePreference
    itemView.findViewById<View>(android.R.id.widget_frame)?.isVisible = hasSwitch
    itemView.findViewById<View>(R.id.settings_preference_chevron)?.isVisible =
      preference?.key in NAVIGATION_PREFERENCE_KEYS
    itemView.findViewById<View>(android.R.id.title)?.importantForAccessibility =
      View.IMPORTANT_FOR_ACCESSIBILITY_NO
    itemView.findViewById<View>(android.R.id.summary)?.importantForAccessibility =
      View.IMPORTANT_FOR_ACCESSIBILITY_NO
    itemView.contentDescription = buildPreferenceDescription(preference)
  }

  private fun Preference?.isSettingsRowPreference(): Boolean {
    return this != null && this !is PreferenceCategory
  }

  private fun buildPreferenceDescription(preference: Preference?): String {
    val parts = mutableListOf<CharSequence?>(
      preference?.title,
      preference?.summary
    )
    if (preference is TwoStatePreference) {
      parts += getString(
        if (preference.isChecked) {
          R.string.array_dark_mode_on
        } else {
          R.string.array_dark_mode_off
        }
      )
    }
    return parts
      .mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotEmpty) }
      .joinToString()
  }

  private fun recordPreferenceEvent(key: String, value: Any = "") {
    Telemetry.recordEvent(
      Constants.Event.SETTINGS,
      mapOf(Telemetry.Param.CONTENT to key, Telemetry.Param.VALUE to value)
    )
  }
}
