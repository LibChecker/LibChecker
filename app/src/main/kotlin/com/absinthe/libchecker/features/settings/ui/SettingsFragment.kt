package com.absinthe.libchecker.features.settings.ui

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.RemoteException
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatDelegate
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.TwoStatePreference
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.R
import com.absinthe.libchecker.app.SystemServices
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.URLManager
import com.absinthe.libchecker.features.about.ui.AboutActivity
import com.absinthe.libchecker.features.applist.detail.ui.ApkDetailActivity
import com.absinthe.libchecker.features.home.HomeViewModel
import com.absinthe.libchecker.ui.base.BaseAlertDialogBuilder
import com.absinthe.libchecker.ui.base.IAppBarContainer
import com.absinthe.libchecker.ui.base.IListController
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.extensions.doOnMainThreadIdle
import com.absinthe.libchecker.utils.extensions.setBottomPaddingSpace
import com.absinthe.libraries.utils.extensions.getBoolean
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import com.absinthe.rulesbundle.LCRemoteRepo
import com.absinthe.rulesbundle.LCRules
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.analytics.EventProperties
import java.util.Locale
import kotlinx.coroutines.launch
import rikka.material.app.LocaleDelegate
import rikka.preference.SimpleMenuPreference
import rikka.recyclerview.fixEdgeEffect
import rikka.widget.borderview.BorderRecyclerView
import rikka.widget.borderview.BorderView
import rikka.widget.borderview.BorderViewDelegate
import timber.log.Timber

class SettingsFragment :
  PreferenceFragmentCompat(),
  IListController {

  private lateinit var borderViewDelegate: BorderViewDelegate
  private lateinit var prefRecyclerView: RecyclerView
  private val viewModel: HomeViewModel by activityViewModels()

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    setPreferencesFromResource(R.xml.settings, null)

    findPreference<TwoStatePreference>(Constants.PREF_SHOW_SYSTEM_APPS)?.apply {
      setOnPreferenceChangeListener { pref, newValue ->
        emitPrefChange(pref.key, newValue)
        Analytics.trackEvent(
          Constants.Event.SETTINGS,
          EventProperties().set("PREF_SHOW_SYSTEM_APPS", newValue as Boolean)
        )
        true
      }
    }
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
        Analytics.trackEvent(
          Constants.Event.SETTINGS,
          EventProperties().set("PREF_APK_ANALYTICS", newValue)
        )
        true
      }
    }
    findPreference<TwoStatePreference>(Constants.PREF_COLORFUL_ICON)?.apply {
      setOnPreferenceChangeListener { pref, newValue ->
        emitPrefChange(pref.key, newValue)
        Analytics.trackEvent(
          Constants.Event.SETTINGS,
          EventProperties().set("PREF_COLORFUL_ICON", newValue as Boolean)
        )
        true
      }
    }
    findPreference<SimpleMenuPreference>(Constants.PREF_RULES_REPO)?.apply {
      setOnPreferenceChangeListener { _, newValue ->
        GlobalValues.repo = newValue as String
        LCRules.setRemoteRepo(
          if (GlobalValues.repo == Constants.REPO_GITHUB) {
            LCRemoteRepo.Github
          } else {
            LCRemoteRepo.Gitlab
          }
        )
        Analytics.trackEvent(
          Constants.Event.SETTINGS,
          EventProperties().set("PREF_RULES_REPO", newValue)
        )
        true
      }
    }
    val languagePreference =
      findPreference<ListPreference>(Constants.PREF_LOCALE)?.apply {
        setOnPreferenceChangeListener { _, newValue ->
          if (newValue is String) {
            val locale: Locale = if ("SYSTEM" == newValue) {
              LocaleDelegate.systemLocale
            } else {
              Locale.forLanguageTag(newValue)
            }
            LocaleDelegate.defaultLocale = locale
            Timber.d("Locale = $locale")
            activity?.recreate()
          }
          true
        }
      }!!
    findPreference<SimpleMenuPreference>(Constants.PREF_SNAPSHOT_KEEP)?.apply {
      setOnPreferenceChangeListener { _, newValue ->
        GlobalValues.snapshotKeep = newValue.toString()
        true
      }
    }
    findPreference<SimpleMenuPreference>(Constants.PREF_DARK_MODE)?.apply {
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
              Analytics.trackEvent(
                Constants.Event.SETTINGS,
                EventProperties().set("PREF_RELOAD_APPS", "Ok")
              )
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
            .show()
          true
        }
      }
    }

    findPreference<Preference>(Constants.PREF_ABOUT)?.apply {
      summary = "${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})"
      setOnPreferenceClickListener {
        startActivity(Intent(requireContext(), AboutActivity::class.java))
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
              data = Uri.parse(URLManager.PLAY_STORE_DETAIL_PAGE)
            }
          )
          Analytics.trackEvent(
            Constants.Event.SETTINGS,
            EventProperties().set("PREF_RATE", "Clicked")
          )
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

    doOnMainThreadIdle {
      recyclerView.setBottomPaddingSpace()
    }

    val lp = recyclerView.layoutParams
    if (lp is FrameLayout.LayoutParams) {
      lp.rightMargin =
        recyclerView.context.resources.getDimension(rikka.material.R.dimen.rd_activity_horizontal_margin)
          .toInt()
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
}
