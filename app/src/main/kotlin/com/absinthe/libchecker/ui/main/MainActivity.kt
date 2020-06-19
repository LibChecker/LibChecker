package com.absinthe.libchecker.ui.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.OnceTag
import com.absinthe.libchecker.databinding.ActivityMainBinding
import com.absinthe.libchecker.ui.fragment.SettingsFragment
import com.absinthe.libchecker.ui.fragment.applist.AppListFragment
import com.absinthe.libchecker.ui.fragment.snapshot.SnapshotFragment
import com.absinthe.libchecker.ui.fragment.statistics.StatisticsFragment
import com.absinthe.libchecker.viewmodel.AppViewModel
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback
import jonathanfinerty.once.Once
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private var clickBottomItemFlag = false
    private val viewModel by viewModels<AppViewModel>()
    private val requestPackageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_PACKAGE_ADDED ||
                intent.action == Intent.ACTION_PACKAGE_REMOVED ||
                intent.action == Intent.ACTION_PACKAGE_REPLACED
            ) {
                GlobalValues.shouldRequestChange = true
                viewModel.requestChange(this@MainActivity)
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

        initView()

        registerPackageBroadcast()
        viewModel.requestConfiguration()

        if (!Once.beenDone(Once.THIS_APP_INSTALL, OnceTag.HAS_COLLECT_LIB)) {
            viewModel.collectPopularLibraries(this)
            Once.markDone(OnceTag.HAS_COLLECT_LIB)
        }
    }

    override fun onResume() {
        super.onResume()
        if (GlobalValues.shouldRequestChange) {
            viewModel.requestChange(this)
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
            }

            // 当ViewPager切换页面时，改变ViewPager的显示
            navView.setOnNavigationItemSelectedListener {
                when (it.itemId) {
                    R.id.navigation_app_list -> {
                        if (binding.viewpager.currentItem != 0) {
                            binding.viewpager.setCurrentItem(0, true)
                        } else {
                            if (!clickBottomItemFlag) {
                                clickBottomItemFlag = true
                                lifecycleScope.launch(Dispatchers.IO) {
                                    delay(200)
                                    clickBottomItemFlag = false
                                }
                            } else {
                                viewModel.clickBottomItemFlag.value = true
                            }
                        }
                    }
                    R.id.navigation_classify -> {
                        if (binding.viewpager.currentItem != 0) {
                            binding.viewpager.setCurrentItem(1, true)
                        } else {
                            if (!clickBottomItemFlag) {
                                clickBottomItemFlag = true
                                lifecycleScope.launch(Dispatchers.IO) {
                                    delay(200)
                                    clickBottomItemFlag = false
                                }
                            } else {
                                viewModel.clickBottomItemFlag.value = true
                            }
                        }
                    }
                    R.id.navigation_snapshot -> binding.viewpager.setCurrentItem(2, true)
                    R.id.navigation_settings -> binding.viewpager.setCurrentItem(3, true)
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
}
