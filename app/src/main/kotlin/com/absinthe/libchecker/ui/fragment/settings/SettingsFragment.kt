package com.absinthe.libchecker.ui.fragment.settings

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import androidx.fragment.app.activityViewModels
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.URLManager
import com.absinthe.libchecker.database.AppItemRepository
import com.absinthe.libchecker.extensions.addPaddingTop
import com.absinthe.libchecker.ui.detail.ApkDetailActivity
import com.absinthe.libchecker.ui.fragment.IListController
import com.absinthe.libchecker.ui.main.MainActivity
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.viewmodel.HomeViewModel
import com.absinthe.libraries.utils.utils.UiUtils
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.analytics.EventProperties
import rikka.material.app.LocaleDelegate
import rikka.preference.SimpleMenuPreference
import rikka.recyclerview.addVerticalPadding
import rikka.recyclerview.fixEdgeEffect
import rikka.widget.borderview.BorderRecyclerView
import rikka.widget.borderview.BorderView
import rikka.widget.borderview.BorderViewDelegate
import timber.log.Timber
import java.util.*

class SettingsFragment : PreferenceFragmentCompat(), IListController {

    private lateinit var borderViewDelegate: BorderViewDelegate
    private val viewModel by activityViewModels<HomeViewModel>()

    companion object {
        init {
            SimpleMenuPreference.setLightFixEnabled(true)
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)

        (findPreference<SwitchPreference>(Constants.PREF_SHOW_SYSTEM_APPS))?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                GlobalValues.isShowSystemApps.value = newValue as Boolean
                Analytics.trackEvent(Constants.Event.SETTINGS, EventProperties().set("PREF_SHOW_SYSTEM_APPS", newValue))
                true
            }
        }
        (findPreference<SwitchPreference>(Constants.PREF_APK_ANALYTICS))?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                val flag = if (newValue as Boolean) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                }

                requireActivity().packageManager.setComponentEnabledSetting(
                    ComponentName(requireActivity(), ApkDetailActivity::class.java),
                    flag, PackageManager.DONT_KILL_APP
                )
                Analytics.trackEvent(Constants.Event.SETTINGS, EventProperties().set("PREF_APK_ANALYTICS", newValue))
                true
            }
        }
        (findPreference<SwitchPreference>(Constants.PREF_COLORFUL_ICON))?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                GlobalValues.isColorfulIcon.value = newValue as Boolean
                AppItemRepository.allApplicationInfoItems.value = AppItemRepository.allApplicationInfoItems.value
                Analytics.trackEvent(Constants.Event.SETTINGS, EventProperties().set("PREF_COLORFUL_ICON", newValue))
                true
            }
        }
        (findPreference<SimpleMenuPreference>(Constants.PREF_RULES_REPO))?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                GlobalValues.repo = newValue as String
                Analytics.trackEvent(Constants.Event.SETTINGS, EventProperties().set("PREF_RULES_REPO", newValue))
                true
            }
        }
        val languagePreference = (findPreference<SimpleMenuPreference>(Constants.PREF_LOCALE))?.apply {
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
        findPreference<Preference>(Constants.PREF_CLOUD_RULES)?.apply {
            setOnPreferenceClickListener {
                CloudRulesDialogFragment().show(childFragmentManager, tag)
                true
            }
        }
        findPreference<Preference>(Constants.PREF_LIB_REF_THRESHOLD)?.apply {
            setOnPreferenceClickListener {
                LibThresholdDialogFragment().show(requireActivity().supportFragmentManager, tag)
                true
            }
        }
        findPreference<Preference>(Constants.PREF_RELOAD_APPS)?.apply {
            setOnPreferenceClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.dialog_title_reload_apps)
                    .setMessage(R.string.dialog_subtitle_reload_apps)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        viewModel.reloadAppsFlag.value = true
                        Analytics.trackEvent(Constants.Event.SETTINGS, EventProperties().set("PREF_RELOAD_APPS", "Ok"))
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .create()
                    .show()
                true
            }
        }

        findPreference<Preference>(Constants.PREF_ABOUT)?.apply {
            summary = "${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})"
        }
        findPreference<Preference>(Constants.PREF_HELP)?.apply {
            setOnPreferenceClickListener {
                try {
                    CustomTabsIntent.Builder().build().apply {
                        launchUrl(requireActivity(), URLManager.DOCS_PAGE.toUri())
                    }
                } catch (e: ActivityNotFoundException) {
                    Timber.e(e)
                    try {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = URLManager.DOCS_PAGE.toUri()
                        }
                        requireActivity().startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        Timber.e(e)
                    }
                }
                true
            }
        }
        findPreference<Preference>(Constants.PREF_RATE)?.apply {
            setOnPreferenceClickListener {
                val hasInstallCoolApk = PackageUtils.isAppInstalled(Constants.PACKAGE_NAME_COOLAPK)
                val marketUrl = if (hasInstallCoolApk) {
                    URLManager.COOLAPK_APP_PAGE
                } else {
                    URLManager.MARKET_PAGE
                }

                try {
                    startActivity(Intent.parseUri(marketUrl, 0))
                    Analytics.trackEvent(Constants.Event.SETTINGS, EventProperties().set("PREF_RATE", "Clicked"))
                } catch (e: ActivityNotFoundException) {
                    Timber.e(e)
                }
                true
            }
        }
        findPreference<Preference>(Constants.PREF_TELEGRAM)?.apply {
            setOnPreferenceClickListener {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, URLManager.TELEGRAM_GROUP.toUri()).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                } catch (e: ActivityNotFoundException) {

                    Timber.e(e)
                }
                true
            }
        }
        (findPreference<SwitchPreference>(Constants.PREF_ANONYMOUS_ANALYTICS))?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                GlobalValues.isAnonymousAnalyticsEnabled.value = newValue as Boolean
                true
            }
        }

        val tag = languagePreference.value
        val index = listOf(*languagePreference.entryValues).indexOf(tag)
        val localeName: MutableList<String> = ArrayList()
        val localeNameUser: MutableList<String> = ArrayList()
        val userLocale = GlobalValues.locale
        for (i in 1 until languagePreference.entries.size) {
            val locale = Locale.forLanguageTag(languagePreference.entries[i].toString())
            localeName.add(if (!TextUtils.isEmpty(locale.script)) locale.getDisplayScript(locale) else locale.getDisplayName(locale))
            localeNameUser.add(if (!TextUtils.isEmpty(locale.script)) locale.getDisplayScript(userLocale) else locale.getDisplayName(userLocale))
        }

        for (i in 1 until languagePreference.entries.size) {
            if (index != i) {
                languagePreference.entries[i] = HtmlCompat.fromHtml(
                    String.format(
                        "%s - %s",
                        localeName[i - 1],
                        localeNameUser[i - 1]
                    ), HtmlCompat.FROM_HTML_MODE_LEGACY
                )
            } else {
                languagePreference.entries[i] = localeNameUser[i - 1]
            }
        }

        if (TextUtils.isEmpty(tag) || "SYSTEM" == tag) {
            languagePreference.summary = getString(R.string.follow_system)
        } else if (index != -1) {
            val name = localeNameUser[index - 1]
            languagePreference.summary = name
        }
    }

    override fun onResume() {
        super.onResume()
        if (this != viewModel.controller) {
            viewModel.controller = this
            requireActivity().invalidateOptionsMenu()
        }
        scheduleAppbarRaisingStatus()
    }

    override fun onCreateRecyclerView(inflater: LayoutInflater, parent: ViewGroup, savedInstanceState: Bundle?): RecyclerView {
        val recyclerView = super.onCreateRecyclerView(inflater, parent, savedInstanceState) as BorderRecyclerView
        recyclerView.fixEdgeEffect()
        recyclerView.addVerticalPadding(0, 8)
        recyclerView.addPaddingTop(UiUtils.getStatusBarHeight())
        recyclerView.overScrollMode = RecyclerView.OVER_SCROLL_NEVER
        recyclerView.isVerticalScrollBarEnabled = false

        val lp = recyclerView.layoutParams
        if (lp is FrameLayout.LayoutParams) {
            lp.rightMargin = recyclerView.context.resources.getDimension(R.dimen.rd_activity_horizontal_margin).toInt()
            lp.leftMargin = lp.rightMargin
        }

        borderViewDelegate = recyclerView.borderViewDelegate
        borderViewDelegate.borderVisibilityChangedListener = BorderView.OnBorderVisibilityChangedListener { top: Boolean, _: Boolean, _: Boolean, _: Boolean -> (activity as MainActivity?)?.appBar?.setRaised(!top) }

        return recyclerView
    }

    override fun onDetach() {
        super.onDetach()
        if (this == viewModel.controller) {
            viewModel.controller = null
        }
    }

    override fun onReturnTop() {
        //Do nothing
    }

    override fun getAppBar() = (activity as MainActivity?)?.appBar

    override fun getBorderViewDelegate() = borderViewDelegate

    override fun scheduleAppbarRaisingStatus() {
        getAppBar()?.setRaised(!getBorderViewDelegate().isShowingTopBorder)
    }
}
