package com.absinthe.libchecker.ui.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.absinthe.libchecker.BaseActivity
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.OnceTag
import com.absinthe.libchecker.database.AppItemRepository
import com.absinthe.libchecker.databinding.ActivityMainBinding
import com.absinthe.libchecker.exception.MiuiOpsException
import com.absinthe.libchecker.extensions.setCurrentItem
import com.absinthe.libchecker.ui.fragment.applist.AppListFragment
import com.absinthe.libchecker.ui.fragment.settings.SettingsFragment
import com.absinthe.libchecker.ui.fragment.snapshot.SnapshotFragment
import com.absinthe.libchecker.ui.fragment.statistics.StatisticsFragment
import com.absinthe.libchecker.utils.FileUtils
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.view.applist.AppListRejectView
import com.absinthe.libchecker.viewmodel.GET_INSTALL_APPS_RETRY_PERIOD
import com.absinthe.libchecker.viewmodel.HomeViewModel
import com.absinthe.libraries.utils.utils.XiaomiUtilities
import com.google.android.material.animation.AnimationUtils
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.analytics.EventProperties
import jonathanfinerty.once.Once
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

const val PAGE_TRANSFORM_DURATION = 300L

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private var clickBottomItemFlag = false
    private val appViewModel by viewModels<HomeViewModel>()
    private val mask by lazy {
        AppListRejectView(this).apply {
            layoutParams = CoordinatorLayout.LayoutParams(
                CoordinatorLayout.LayoutParams.MATCH_PARENT,
                CoordinatorLayout.LayoutParams.MATCH_PARENT
            )
        }
    }

    override fun setViewBinding(): ViewGroup {
        binding = ActivityMainBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initView()
        handleIntentFromShortcuts(intent)
        initAllApplicationInfoItems()
        initObserver()
        clearApkCache()
        appViewModel.initRegexRules()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntentFromShortcuts(intent)
    }

    override fun onStart() {
        super.onStart()
        registerPackageBroadcast()
    }

    override fun onResume() {
        super.onResume()
        if (GlobalValues.shouldRequestChange.value == true) {
            appViewModel.requestChange(true)
        }
        if (XiaomiUtilities.isMIUI()) {
            addOrRemoveMiuiAppsListMask()
        }
    }

    override fun onStop() {
        super.onStop()
        unregisterPackageBroadcast()
    }

    fun showNavigationView() {
        binding.navView
            .animate()
            .translationY(0F)
            .setDuration(225)
            .interpolator = AnimationUtils.LINEAR_OUT_SLOW_IN_INTERPOLATOR
    }

    private fun initView() {
        setAppBar(binding.appbar, binding.toolbar)
        (binding.root as ViewGroup).bringChildToFront(binding.appbar)
        supportActionBar?.title = LCAppUtils.setTitle()

        binding.apply {
            viewpager.apply {
                adapter = object : FragmentStateAdapter(this@MainActivity) {
                    override fun getItemCount(): Int {
                        return 4
                    }

                    override fun createFragment(position: Int): Fragment {
                        return when (position) {
                            0 -> AppListFragment()
                            1 -> StatisticsFragment()
                            2 -> SnapshotFragment()
                            else -> SettingsFragment()
                        }
                    }
                }

                // 当ViewPager切换页面时，改变底部导航栏的状态
                registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        super.onPageSelected(position)
                        binding.navView.menu.getItem(position).isChecked = true
                    }
                })

                //禁止左右滑动
                isUserInputEnabled = false
                offscreenPageLimit = 3
            }

            // 当 ViewPager 切换页面时，改变 ViewPager 的显示
            navView.setOnNavigationItemSelectedListener {

                fun performClickNavigationItem(index: Int) {
                    if (binding.viewpager.currentItem != index) {
                        if (!binding.viewpager.isFakeDragging) {
                            binding.viewpager.setCurrentItem(index, PAGE_TRANSFORM_DURATION)
                        }
                    } else {
                        if (!clickBottomItemFlag) {
                            clickBottomItemFlag = true

                            lifecycleScope.launch {
                                delay(200)
                                clickBottomItemFlag = false
                            }
                        } else {
                            appViewModel.controller?.onReturnTop()
                        }
                    }
                }

                when (it.itemId) {
                    R.id.navigation_app_list -> performClickNavigationItem(0)
                    R.id.navigation_classify -> performClickNavigationItem(1)
                    R.id.navigation_snapshot -> performClickNavigationItem(2)
                    R.id.navigation_settings -> performClickNavigationItem(3)
                }
                true
            }
            navView.setOnClickListener { /*Do nothing*/ }
        }
    }

    private val requestPackageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            appViewModel.packageChangedLiveData.postValue(intent.data?.encodedSchemeSpecificPart)
        }
    }

    private fun registerPackageBroadcast() {
        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }

        registerReceiver(requestPackageReceiver, intentFilter)
    }

    private fun unregisterPackageBroadcast() {
        unregisterReceiver(requestPackageReceiver)
    }

    private fun handleIntentFromShortcuts(intent: Intent) {
        when(intent.action) {
            Constants.ACTION_APP_LIST -> binding.viewpager.setCurrentItem(0, false)
            Constants.ACTION_STATISTICS -> binding.viewpager.setCurrentItem(1, false)
            Constants.ACTION_SNAPSHOT -> binding.viewpager.setCurrentItem(2, false)
        }
        Analytics.trackEvent(Constants.Event.LAUNCH_ACTION, EventProperties().set("Action", intent.action))
    }

    private fun initAllApplicationInfoItems() {
        lifecycleScope.launch {
            var appList: List<ApplicationInfo>?

            do {
                appList = try {
                    PackageUtils.getInstallApplications()
                } catch (e: MiuiOpsException) {
                    emptyList()
                } catch (e: Exception) {
                    delay(GET_INSTALL_APPS_RETRY_PERIOD)
                    null
                }
            } while (appList == null)

            AppItemRepository.allApplicationInfoItems.postValue(appList)
        }
    }

    private fun initObserver() {
        appViewModel.apply {
            if (!Once.beenDone(Once.THIS_APP_VERSION, OnceTag.FIRST_INSERT_RULES)) {
                insertPreinstallRules(this@MainActivity)
                Once.markDone(OnceTag.FIRST_INSERT_RULES)
            }

            if (!Once.beenDone(Once.THIS_APP_INSTALL, OnceTag.FIRST_LAUNCH)) {
                initItems()
            }

            reloadAppsFlag.observe(this@MainActivity, {
                if (it) {
                    binding.viewpager.setCurrentItem(0, true)
                }
            })
        }
    }

    private fun clearApkCache() {
        FileUtils.delete(File(externalCacheDir, "temp.apk"))
    }

    private fun addOrRemoveMiuiAppsListMask() {
        if (!XiaomiUtilities.isCustomPermissionGranted(XiaomiUtilities.OP_GET_INSTALLED_APPS)) {
            if (mask.parent == null) {
                binding.root.addView(mask)
                binding.navView.isVisible = false
            }
        } else {
            mask.parent?.let {
                binding.root.removeView(mask)
                binding.navView.isVisible = true
            }
        }
    }
}
