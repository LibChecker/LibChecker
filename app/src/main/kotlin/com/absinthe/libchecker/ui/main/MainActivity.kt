package com.absinthe.libchecker.ui.main

import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.absinthe.libchecker.BaseActivity
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.OnceTag
import com.absinthe.libchecker.databinding.ActivityMainBinding
import com.absinthe.libchecker.ui.fragment.SettingsFragment
import com.absinthe.libchecker.ui.fragment.applist.AppListFragment
import com.absinthe.libchecker.ui.fragment.classify.ClassifyFragment
import com.absinthe.libchecker.viewmodel.AppViewModel
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback
import jonathanfinerty.once.Once


class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel by lazy { ViewModelProvider(this).get(AppViewModel::class.java) }

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

    private fun initView() {
        setAppBar(binding.appbar, binding.toolbar)
        (binding.root as ViewGroup).bringChildToFront(binding.appbar)

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
                    R.id.navigation_app_list -> binding.viewpager.setCurrentItem(0, true)
                    R.id.navigation_classify -> binding.viewpager.setCurrentItem(1, true)
                    R.id.navigation_settings -> binding.viewpager.setCurrentItem(2, true)
                }
                true
            }
        }
    }
}
