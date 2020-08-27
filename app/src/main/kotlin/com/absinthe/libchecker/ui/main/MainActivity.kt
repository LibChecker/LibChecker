package com.absinthe.libchecker.ui.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.absinthe.libchecker.BaseActivity
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.librarymap.*
import com.absinthe.libchecker.database.AppItemRepository
import com.absinthe.libchecker.databinding.ActivityMainBinding
import com.absinthe.libchecker.extensions.setCurrentItem
import com.absinthe.libchecker.ui.fragment.SettingsFragment
import com.absinthe.libchecker.ui.fragment.applist.AppListFragment
import com.absinthe.libchecker.ui.fragment.snapshot.SnapshotFragment
import com.absinthe.libchecker.ui.fragment.statistics.StatisticsFragment
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.viewmodel.AppViewModel
import com.absinthe.libchecker.viewmodel.GET_INSTALL_APPS_RETRY_PERIOD
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.analytics.EventProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val PAGE_TRANSFORM_DURATION = 300L

class MainActivity : BaseActivity() {

    var hasRequestChanges = false
    private lateinit var binding: ActivityMainBinding
    private var clickBottomItemFlag = false
    private val appViewModel by viewModels<AppViewModel>()
    private val requestPackageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_PACKAGE_ADDED ||
                intent.action == Intent.ACTION_PACKAGE_REMOVED ||
                intent.action == Intent.ACTION_PACKAGE_REPLACED
            ) {
                appViewModel.requestChange(packageManager)
            }
        }
    }
    private val viewModel by viewModels<AppViewModel>()

    override fun setViewBinding(): ViewGroup {
        binding = ActivityMainBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.apply {
            requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
            sharedElementsUseOverlay = false
        }
        setExitSharedElementCallback(MaterialContainerTransformSharedElementCallback())
        super.onCreate(savedInstanceState)

        initView()
        registerPackageBroadcast()
        handleIntentFromShortcuts(intent)
        initAllApplicationInfoItems()
        initObserver()
        initMap()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntentFromShortcuts(intent)
    }

    override fun onResume() {
        super.onResume()
        if (GlobalValues.shouldRequestChange.value == true && hasRequestChanges) {
            appViewModel.requestChange(packageManager)
        }
    }

    override fun onDestroy() {
        unregisterPackageBroadcast()
        super.onDestroy()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    private fun initView() {
        setAppBar(binding.appbar, binding.toolbar)
        (binding.root as ViewGroup).bringChildToFront(binding.appbar)

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
                when (it.itemId) {
                    R.id.navigation_app_list -> {
                        if (binding.viewpager.currentItem != 0) {
                            binding.viewpager.setCurrentItem(0, PAGE_TRANSFORM_DURATION)
                        } else {
                            if (!clickBottomItemFlag) {
                                clickBottomItemFlag = true

                                lifecycleScope.launch(Dispatchers.IO) {
                                    delay(200)
                                    clickBottomItemFlag = false
                                }
                            } else {
                                appViewModel.clickBottomItemFlag.value = true
                            }
                        }
                    }
                    R.id.navigation_classify -> {
                        if (binding.viewpager.currentItem != 1) {
                            binding.viewpager.setCurrentItem(1, PAGE_TRANSFORM_DURATION)
                        } else {
                            if (!clickBottomItemFlag) {
                                clickBottomItemFlag = true

                                lifecycleScope.launch(Dispatchers.IO) {
                                    delay(200)
                                    clickBottomItemFlag = false
                                }
                            } else {
                                appViewModel.clickBottomItemFlag.value = true
                            }
                        }
                    }
                    R.id.navigation_snapshot -> binding.viewpager.setCurrentItem(2, PAGE_TRANSFORM_DURATION)
                    R.id.navigation_settings -> binding.viewpager.setCurrentItem(3, PAGE_TRANSFORM_DURATION)
                }
                true
            }
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
        lifecycleScope.launch(Dispatchers.IO) {
            var appList: List<ApplicationInfo>?

            do {
                appList = try {
                    PackageUtils.getInstallApplications()
                } catch (e: Exception) {
                    delay(GET_INSTALL_APPS_RETRY_PERIOD)
                    null
                }
            } while (appList == null)

            withContext(Dispatchers.Main) {
                AppItemRepository.allApplicationInfoItems.value = appList
            }
        }
    }

    private fun initObserver() {
        viewModel.reloadAppsFlag.observe(this, {
            if (it) {
                binding.viewpager.setCurrentItem(0, true)
            }
        })
    }

    private fun initMap() = lifecycleScope.launch(Dispatchers.IO) {
        NativeLibMap
        ServiceLibMap
        ActivityLibMap
        ReceiverLibMap
        ProviderLibMap
        DexLibMap
    }
}
