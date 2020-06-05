package com.absinthe.libchecker.ui.main

import android.content.ActivityNotFoundException
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.Window
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.absinthe.libchecker.BaseActivity
import com.absinthe.libchecker.R
import com.absinthe.libchecker.databinding.ActivityAppDetailBinding
import com.absinthe.libchecker.ui.fragment.applist.ComponentsAnalysisFragment
import com.absinthe.libchecker.ui.fragment.applist.SoAnalysisFragment
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.view.EXTRA_PKG_NAME
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.IntentUtils
import com.blankj.utilcode.util.ToastUtils
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback

class AppDetailActivity : BaseActivity() {

    private lateinit var binding: ActivityAppDetailBinding
    private var pkgName = ""

    init {
        isPaddingToolbar = true
    }

    override fun setViewBinding() {
        binding = ActivityAppDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun setRoot() {
        root = binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        initTransition()

        super.onCreate(savedInstanceState)
        initView()

        intent.extras?.let {
            it.getString(EXTRA_PKG_NAME)?.let { packageName ->
                pkgName = packageName
                supportActionBar?.apply {
                    title = AppUtils.getAppName(packageName)
                }
                binding.apply {
                    ivAppIcon.setImageDrawable(AppUtils.getAppIcon(packageName))
                    tvAppName.text = AppUtils.getAppName(packageName)
                    tvPackageName.text = packageName
                    tvVersion.text = PackageUtils.getVersionString(packageName)
                    tvTargetApi.text = PackageUtils.getTargetApiString(packageName)
                    chipSplit.isGone = !PackageUtils.isSplitsApk(packageName)
                }
            } ?: finish()
        } ?: finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initTransition() {
        window.apply {
            requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
            sharedElementEnterTransition = MaterialContainerTransform().apply {
                addTarget(android.R.id.content)
                duration = 300L
            }
            sharedElementReturnTransition = MaterialContainerTransform().apply {
                addTarget(android.R.id.content)
                duration = 300L
            }
        }
        findViewById<View>(android.R.id.content).transitionName = "app_card_container"
        setEnterSharedElementCallback(MaterialContainerTransformSharedElementCallback())
    }

    private fun initView() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        binding.viewpager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int {
                return 2
            }

            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> SoAnalysisFragment.newInstance(pkgName)
                    else -> ComponentsAnalysisFragment.newInstance(pkgName)
                }
            }
        }

        val mediator = TabLayoutMediator(binding.tabLayout, binding.viewpager,
            TabLayoutMediator.TabConfigurationStrategy { tab, position ->
                when (position) {
                    0 -> tab.text = getText(R.string.tab_so_analysis)
                    else -> tab.text = getText(R.string.tab_components_analysis)
                }
            })
        mediator.attach()

        binding.ivAppIcon.setOnClickListener {
            try {
                startActivity(IntentUtils.getLaunchAppIntent(pkgName))
            } catch (e: ActivityNotFoundException) {
                ToastUtils.showShort("Can\'t open this app")
            }
        }
    }
}
