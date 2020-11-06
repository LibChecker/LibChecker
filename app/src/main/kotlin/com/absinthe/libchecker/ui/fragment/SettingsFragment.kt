package com.absinthe.libchecker.ui.fragment

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.URLManager
import com.absinthe.libchecker.database.AppItemRepository
import com.absinthe.libchecker.ui.detail.ApkDetailActivity
import com.absinthe.libchecker.ui.main.IListContainer
import com.absinthe.libchecker.ui.main.MainActivity
import com.absinthe.libchecker.view.dialogfragment.LibThresholdDialogFragment
import com.absinthe.libchecker.viewmodel.AppViewModel
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.analytics.EventProperties
import moe.shizuku.preference.ListPreference
import moe.shizuku.preference.PreferenceFragment
import moe.shizuku.preference.SwitchPreference
import rikka.material.widget.BorderRecyclerView
import rikka.material.widget.BorderView
import rikka.recyclerview.fixEdgeEffect

class SettingsFragment : PreferenceFragment(), IListController {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)

        (findPreference(Constants.PREF_SHOW_SYSTEM_APPS) as SwitchPreference).apply {
            setOnPreferenceChangeListener { _, newValue ->
                GlobalValues.isShowSystemApps.value = newValue as Boolean
                Analytics.trackEvent(Constants.Event.SETTINGS, EventProperties().set("PREF_SHOW_SYSTEM_APPS", newValue))
                true
            }
        }
        (findPreference(Constants.PREF_ENTRY_ANIMATION) as SwitchPreference).apply {
            setOnPreferenceChangeListener { _, newValue ->
                GlobalValues.isShowEntryAnimation.value = newValue as Boolean
                Analytics.trackEvent(Constants.Event.SETTINGS, EventProperties().set("PREF_ENTRY_ANIMATION", newValue))
                true
            }
        }
        (findPreference(Constants.PREF_APK_ANALYTICS) as SwitchPreference).apply {
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
        (findPreference(Constants.PREF_COLORFUL_ICON) as SwitchPreference).apply {
            setOnPreferenceChangeListener { _, newValue ->
                GlobalValues.isColorfulIcon.value = newValue as Boolean
                AppItemRepository.allApplicationInfoItems.value = AppItemRepository.allApplicationInfoItems.value
                Analytics.trackEvent(Constants.Event.SETTINGS, EventProperties().set("PREF_COLORFUL_ICON", newValue))
                true
            }
        }
        (findPreference(Constants.PREF_RULES_REPO) as ListPreference).apply {
            setOnPreferenceChangeListener { _, newValue ->
                GlobalValues.repo = newValue as String
                Analytics.trackEvent(Constants.Event.SETTINGS, EventProperties().set("PREF_RULES_REPO", newValue))
                true
            }
        }
        findPreference(Constants.PREF_LIB_REF_THRESHOLD)?.apply {
            setOnPreferenceClickListener {
                LibThresholdDialogFragment().show(requireActivity().supportFragmentManager, tag)
                true
            }
        }
        findPreference(Constants.PREF_RELOAD_APPS)?.apply {
            setOnPreferenceClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.dialog_title_reload_apps)
                    .setMessage(R.string.dialog_subtitle_reload_apps)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        val viewModel by activityViewModels<AppViewModel>()
                        viewModel.reloadAppsFlag.value = true
                        Analytics.trackEvent(Constants.Event.SETTINGS, EventProperties().set("PREF_RELOAD_APPS", "Ok"))
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .create()
                    .show()
                true
            }
        }

        findPreference(Constants.PREF_ABOUT)?.apply {
            summary = "${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})"
        }
        findPreference(Constants.PREF_HELP)?.apply {
            setOnPreferenceClickListener {
                try {
                    CustomTabsIntent.Builder().build().apply {
                        launchUrl(requireActivity(), URLManager.DOCS_PAGE.toUri())
                    }
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = URLManager.DOCS_PAGE.toUri()
                    }
                    requireActivity().startActivity(intent)
                }
                true
            }
        }
        findPreference(Constants.PREF_RATE)?.apply {
            setOnPreferenceClickListener {
                val hasInstallCoolApk = com.blankj.utilcode.util.AppUtils.isAppInstalled("com.coolapk.market")
                val marketUrl = if (hasInstallCoolApk) {
                    URLManager.COOLAPK_APP_PAGE
                } else {
                    URLManager.MARKET_PAGE
                }

                try {
                    startActivity(Intent.parseUri(marketUrl, 0))
                    Analytics.trackEvent(Constants.Event.SETTINGS, EventProperties().set("PREF_RATE", "Clicked"))
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
                true
            }
        }
        findPreference("tg")?.apply {
            setOnPreferenceClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, "https://t.me/libcheckerr".toUri()).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
                true
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ((requireActivity() as IListContainer).controller as? Fragment)?.setHasOptionsMenu(false)
        (requireActivity() as IListContainer).controller = this
    }

    override fun onCreateItemDecoration(): DividerDecoration? {
        return CategoryDivideDividerDecoration()
    }

    override fun onCreateRecyclerView(inflater: LayoutInflater, parent: ViewGroup, savedInstanceState: Bundle?): RecyclerView {
        val recyclerView = super.onCreateRecyclerView(inflater, parent, savedInstanceState) as BorderRecyclerView
        recyclerView.fixEdgeEffect()
        recyclerView.overScrollMode = RecyclerView.OVER_SCROLL_NEVER
        recyclerView.scrollBarStyle = RecyclerView.SCROLL_AXIS_NONE
        recyclerView.scrollToPosition(0)

        val lp = recyclerView.layoutParams
        if (lp is FrameLayout.LayoutParams) {
            lp.rightMargin = recyclerView.context.resources.getDimension(R.dimen.rd_activity_horizontal_margin).toInt()
            lp.leftMargin = lp.rightMargin
        }

        recyclerView.borderViewDelegate.borderVisibilityChangedListener = BorderView.OnBorderVisibilityChangedListener { top: Boolean, _: Boolean, _: Boolean, _: Boolean -> (activity as MainActivity?)?.appBar?.setRaised(!top) }
        return recyclerView
    }

    override fun onReturnTop() {
        //Do nothing
    }
}
