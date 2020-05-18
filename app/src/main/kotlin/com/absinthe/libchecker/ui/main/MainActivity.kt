package com.absinthe.libchecker.ui.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.Window
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.absinthe.libchecker.BaseActivity
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.OnceTag
import com.absinthe.libchecker.databinding.ActivityMainBinding
import com.absinthe.libchecker.ui.fragment.classify.ClassifyFragment
import com.absinthe.libchecker.ui.fragment.SettingsFragment
import com.absinthe.libchecker.ui.fragment.applist.AppListFragment
import com.absinthe.libchecker.viewmodel.AppViewModel
import com.google.android.material.transition.MaterialContainerTransformSharedElementCallback
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import jonathanfinerty.once.Once

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel by lazy { ViewModelProvider(this).get(AppViewModel::class.java) }
    private val pbc = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_PACKAGE_ADDED ||
                intent.action == Intent.ACTION_PACKAGE_REMOVED ||
                intent.action == Intent.ACTION_PACKAGE_REPLACED
            ) {
                viewModel.requestChange(context)
            }
        }
    }

    override fun setViewBinding() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun setRoot() {
        root = binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.apply {
            requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
            sharedElementsUseOverlay = false
        }
        setExitSharedElementCallback(MaterialContainerTransformSharedElementCallback())

        super.onCreate(savedInstanceState)

        if (!BuildConfig.DEBUG) {
            AppCenter.start(
                application, "5f11b856-0a27-4438-a038-9e18e4797133",
                Analytics::class.java, Crashes::class.java
            )
        }

        initView()
        registerPackageBroadcast()
        viewModel.requestConfiguration()

        if (!Once.beenDone(Once.THIS_APP_INSTALL, OnceTag.HAS_COLLECT_LIB)) {
            viewModel.collectPopularLibraries(this)
            Once.markDone(OnceTag.HAS_COLLECT_LIB)
        }
    }

    override fun onDestroy() {
        unregisterPackageBroadcast()
        super.onDestroy()
    }

    private fun initView() {
        setSupportActionBar(binding.toolbar)

        binding.apply {
            viewpager.apply {
                adapter = object : FragmentStateAdapter(this@MainActivity) {
                    override fun getItemCount(): Int {
                        return 3
                    }

                    override fun createFragment(position: Int): Fragment {
                        return when (position) {
                            0 -> AppListFragment()
                            1 -> ClassifyFragment()
                            2 -> SettingsFragment()
                            else -> AppListFragment()
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
            }

            // 当ViewPager切换页面时，改变ViewPager的显示
            navView.setOnNavigationItemSelectedListener {
                when (it.itemId) {
                    R.id.navigation_app_list -> binding.viewpager.setCurrentItem(0, true)
                    R.id.navigation_classify -> binding.viewpager.setCurrentItem(1, true)
                    R.id.navigation_settings -> binding.viewpager.setCurrentItem(2, true)
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

        registerReceiver(pbc, intentFilter)
    }

    private fun unregisterPackageBroadcast() {
        unregisterReceiver(pbc)
    }
}
