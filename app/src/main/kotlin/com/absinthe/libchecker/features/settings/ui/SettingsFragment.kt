package com.absinthe.libchecker.features.settings.ui

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.RemoteException
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatDelegate
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.FileProvider
import androidx.core.content.edit
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
import com.absinthe.libchecker.app.SystemServices
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.URLManager
import com.absinthe.libchecker.features.about.AboutPageBuilder
import com.absinthe.libchecker.features.applist.detail.ui.ApkDetailActivity
import com.absinthe.libchecker.features.home.HomeViewModel
import com.absinthe.libchecker.ui.base.BaseAlertDialogBuilder
import com.absinthe.libchecker.ui.base.IAppBarContainer
import com.absinthe.libchecker.ui.base.IListController
import com.absinthe.libchecker.utils.LocaleUtils
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.Telemetry
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.extensions.applySystemBarsPadding
import com.absinthe.libchecker.utils.extensions.doOnMainThreadIdle
import com.absinthe.libchecker.utils.extensions.setBottomPaddingSpace
import com.absinthe.libraries.utils.extensions.getBoolean
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import com.absinthe.rulesbundle.LCRemoteRepo
import com.absinthe.rulesbundle.LCRules
import com.google.android.material.card.MaterialCardView
import java.io.File
import java.util.Locale
import kotlinx.coroutines.launch
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
  private val viewModel: HomeViewModel by activityViewModels()

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    setPreferencesFromResource(R.xml.settings, null)
    preferenceScreen.applyM3eLayoutResources()

    findPreference<TwoStatePreference>(Constants.PREF_APK_ANALYTICS)?.apply {
      setOnPreferenceChangeListener { _, newValue ->
        val flag = if (newValue as Boolean) {
          PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
          PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }

        try {
          SystemServices.packageManager.setComponentEnabledSetting(
            ComponentName(BuildConfig.APPLICATION_ID, ApkDetailActivity::class.java.name),
            flag,
            PackageManager.DONT_KILL_APP
          )
        } catch (e: RemoteException) {
          Timber.e(e)
          Toasty.showShort(requireContext(), e.toString())
        }
        recordPreferenceEvent(Constants.PREF_APK_ANALYTICS, newValue)
        true
      }
    }
    findPreference<TwoStatePreference>(Constants.PREF_COLORFUL_ICON)?.apply {
      setOnPreferenceChangeListener { pref, newValue ->
        emitPrefChange(pref.key, newValue)
        recordPreferenceEvent(Constants.PREF_COLORFUL_ICON, newValue)
        true
      }
    }
    findPreference<ListPreference>(Constants.PREF_RULES_REPO)?.apply {
      summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
      setOnPreferenceChangeListener { _, newValue ->
        GlobalValues.repo = newValue as String
        LCRules.setRemoteRepo(
          if (GlobalValues.repo == Constants.REPO_GITHUB) {
            LCRemoteRepo.Github
          } else {
            LCRemoteRepo.Gitlab
          }
        )
        recordPreferenceEvent(Constants.PREF_RULES_REPO, newValue)
        true
      }
    }
    val languagePreference =
      findPreference<ListPreference>(Constants.PREF_LOCALE)?.apply {
        isVisible = !OsUtils.atLeastT()
        setOnPreferenceChangeListener { _, newValue ->
          if (newValue is String) {
            val locale: Locale = if ("SYSTEM" == newValue) {
              LocaleUtils.systemLocale
            } else {
              Locale.forLanguageTag(newValue)
            }
            preferenceManager.sharedPreferences?.edit {
              putString(Constants.PREF_LOCALE, newValue)
            }
            Timber.d("Locale = $locale")
            activity?.recreate()
          }
          true
        }
      }!!
    findPreference<ListPreference>(Constants.PREF_SNAPSHOT_KEEP)?.apply {
      summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
      setOnPreferenceChangeListener { _, newValue ->
        GlobalValues.snapshotKeep = newValue.toString()
        true
      }
    }
    findPreference<ListPreference>(Constants.PREF_DARK_MODE)?.apply {
      summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
      setOnPreferenceChangeListener { _, newValue ->
        GlobalValues.darkMode = newValue.toString()
        AppCompatDelegate.setDefaultNightMode(UiUtils.getNightMode())
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
          LibThresholdDialogFragment().show(
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
              viewModel.reloadApps()
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
          val logDir = File(requireContext().cacheDir, "logs")
          if (!logDir.exists() || !logDir.isDirectory) {
            return@setOnPreferenceClickListener true
          }

          val latestLogFile = logDir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".log") }
            ?.maxByOrNull { it.lastModified() }

          if (latestLogFile == null) {
            return@setOnPreferenceClickListener true
          }

          Timber.d("Latest log file: ${latestLogFile.absolutePath}")
          try {
            val uri = FileProvider.getUriForFile(
              requireContext(),
              "${BuildConfig.APPLICATION_ID}.fileprovider",
              latestLogFile
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
              type = "text/plain"
              putExtra(Intent.EXTRA_STREAM, uri)
              addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, getString(R.string.export_log)))
          } catch (e: Exception) {
            Timber.e(e)
            Toasty.showShort(requireContext(), e.toString())
          }
          true

          recordPreferenceEvent(Constants.PREF_EXPORT_LOG)
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

    val tag = languagePreference.value
    val index = listOf(*languagePreference.entryValues).indexOf(tag)
    Timber.d("Locale = $tag, index = $index, entries = ${listOf(*languagePreference.entryValues)}")
    val localeName: MutableList<String> = ArrayList()
    val localeNameUser: MutableList<String> = ArrayList()
    val userLocale = GlobalValues.locale
    for (i in 1 until languagePreference.entries.size) {
      val locale = Locale.forLanguageTag(languagePreference.entries[i].toString())
      localeName.add(
        if (!TextUtils.isEmpty(locale.script)) {
          locale.getDisplayScript(locale)
        } else {
          locale.getDisplayName(locale)
        }
      )
      localeNameUser.add(
        if (!TextUtils.isEmpty(locale.script)) {
          locale.getDisplayScript(userLocale)
        } else {
          locale.getDisplayName(userLocale)
        }
      )
    }

    for (i in 1 until languagePreference.entries.size) {
      if (index != i) {
        languagePreference.entries[i] = HtmlCompat.fromHtml(
          String.format(
            "%s - %s",
            localeName[i - 1],
            localeNameUser[i - 1]
          ),
          HtmlCompat.FROM_HTML_MODE_LEGACY
        )
      } else {
        languagePreference.entries[i] = localeNameUser[i - 1]
      }
    }

    if (TextUtils.isEmpty(tag) || "SYSTEM" == tag) {
      languagePreference.summary = getString(rikka.core.R.string.follow_system)
    } else if (index != -1) {
      val name = localeNameUser[index - 1]
      languagePreference.summary = name
    }
  }

  override fun onResume() {
    super.onResume()
    val container = (activity as? IAppBarContainer) ?: return
    if (this != viewModel.controller) {
      viewModel.controller = this
    }
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
    if (this == viewModel.controller) {
      viewModel.controller = null
    }
  }

  override fun onReturnTop() {
    // Do nothing
  }

  override fun getBorderViewDelegate(): BorderViewDelegate = borderViewDelegate
  override fun isAllowRefreshing(): Boolean = true
  override fun getSuitableLayoutManager(): RecyclerView.LayoutManager? = null

  private fun emitPrefChange(key: String, value: Any) {
    lifecycleScope.launch {
      GlobalValues.preferencesFlow.emit(key to value)
    }
  }

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
  }

  private fun Preference?.isSettingsRowPreference(): Boolean {
    return this != null && this !is PreferenceCategory
  }

  private fun recordPreferenceEvent(key: String, value: Any = "") {
    Telemetry.recordEvent(
      Constants.Event.SETTINGS,
      mapOf(Telemetry.Param.CONTENT to key, Telemetry.Param.VALUE to value)
    )
  }
}
