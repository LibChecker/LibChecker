package com.absinthe.libchecker.ui.detail

import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import coil.load
import com.absinthe.libchecker.BaseActivity
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.*
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.database.LCDatabase
import com.absinthe.libchecker.database.LCRepository
import com.absinthe.libchecker.databinding.ActivityAppDetailBinding
import com.absinthe.libchecker.extensions.finishCompat
import com.absinthe.libchecker.extensions.setLongClickCopiedToClipboard
import com.absinthe.libchecker.extensions.valueUnsafe
import com.absinthe.libchecker.ui.fragment.applist.ComponentsAnalysisFragment
import com.absinthe.libchecker.ui.fragment.applist.NativeAnalysisFragment
import com.absinthe.libchecker.ui.fragment.applist.Sortable
import com.absinthe.libchecker.ui.fragment.detail.AppInfoBottomShellDialogFragment
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.viewmodel.DetailViewModel
import com.blankj.utilcode.util.AppUtils
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val EXTRA_PACKAGE_NAME = "android.intent.extra.PACKAGE_NAME"

class AppDetailActivity : BaseActivity(), IDetailContainer {

    private lateinit var binding: ActivityAppDetailBinding
    private val pkgName by lazy { intent.getStringExtra(EXTRA_PACKAGE_NAME) }
    private val viewModel by viewModels<DetailViewModel>()
    private var currentItemsCount = -1

    override var currentFragment: Sortable? = null

    override fun setViewBinding(): ViewGroup {
        isPaddingToolbar = true
        binding = ActivityAppDetailBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        initTransition()
        super.onCreate(savedInstanceState)
        initView()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finishCompat()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (GlobalValues.isShowEntryAnimation.valueUnsafe) {
            supportFinishAfterTransition()
        } else {
            super.onBackPressed()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setRootPadding()
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
                duration = 250L
            }
        }
        findViewById<View>(android.R.id.content).transitionName = pkgName
        setEnterSharedElementCallback(MaterialContainerTransformSharedElementCallback())
    }

    private fun initView() {
        setRootPadding()
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        pkgName?.let { packageName ->
            supportActionBar?.apply {
                title = AppUtils.getAppName(packageName)
            }
            binding.apply {
                try {
                    val packageInfo = PackageUtils.getPackageInfo(packageName)
                    ivAppIcon.apply {
                        load(LCAppUtils.getAppIcon(packageName))
                        setOnClickListener {
                            AppInfoBottomShellDialogFragment().apply {
                                arguments = Bundle().apply {
                                    putString(EXTRA_PACKAGE_NAME, pkgName)
                                }
                                show(supportFragmentManager, tag)
                            }
                        }
                    }
                    tvAppName.apply {
                        text = AppUtils.getAppName(packageName)
                        setLongClickCopiedToClipboard(text.toString())
                    }
                    tvPackageName.apply {
                        text = packageName
                        setLongClickCopiedToClipboard(text.toString())
                    }
                    tvVersion.apply {
                        text = PackageUtils.getVersionString(packageInfo)
                        setLongClickCopiedToClipboard(text.toString())
                    }
                    tvTargetApi.text = PackageUtils.getTargetApiString(packageInfo)

                    val abi = PackageUtils.getAbi(
                        packageInfo.applicationInfo.sourceDir,
                        packageInfo.applicationInfo.nativeLibraryDir,
                        isApk = false
                    )

                    layoutAbi.tvAbi.text = PackageUtils.getAbiString(abi)
                    layoutAbi.ivAbiType.load(PackageUtils.getAbiBadgeResource(abi))

                    lifecycleScope.launch(Dispatchers.IO) {
                        val lcDao = LCDatabase.getDatabase(application).lcDao()
                        val repository = LCRepository(lcDao)
                        val lcItem = repository.getItem(packageName)

                        val isSplitApk = lcItem?.isSplitApk ?: false
                        val isKotlinUsed = lcItem?.isKotlinUsed ?: false

                        withContext(Dispatchers.Main) {
                            if (isSplitApk || isKotlinUsed) {
                                binding.chipGroup.isVisible = true
                                binding.chipAppBundle.apply {
                                    isVisible = isSplitApk
                                    setOnClickListener {
                                        AlertDialog.Builder(this@AppDetailActivity)
                                            .setIcon(R.drawable.ic_aab)
                                            .setTitle(R.string.app_bundle)
                                            .setMessage(R.string.app_bundle_details)
                                            .setPositiveButton(android.R.string.ok, null)
                                            .show()
                                    }
                                }
                                binding.chipKotlinUsed.apply {
                                    isVisible = isKotlinUsed
                                    setOnClickListener {
                                        AlertDialog.Builder(this@AppDetailActivity)
                                            .setIcon(R.drawable.ic_kotlin_logo)
                                            .setTitle(R.string.kotlin_string)
                                            .setMessage(R.string.kotlin_details)
                                            .setPositiveButton(android.R.string.ok, null)
                                            .show()
                                    }
                                }
                            } else {
                                binding.chipGroup.isVisible = false
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    supportFinishAfterTransition()
                }

                ibSort.setOnClickListener {
                    currentFragment?.sort()
                }
            }

            val types = listOf(
                NATIVE, SERVICE, ACTIVITY, RECEIVER, PROVIDER, DEX
            )
            val tabTitles = listOf(
                getText(R.string.ref_category_native),
                getText(R.string.ref_category_service),
                getText(R.string.ref_category_activity),
                getText(R.string.ref_category_br),
                getText(R.string.ref_category_cp),
                getText(R.string.ref_category_dex)
            )

            binding.viewpager.apply {
                adapter = object : FragmentStateAdapter(this@AppDetailActivity) {
                    override fun getItemCount(): Int {
                        return types.size
                    }

                    override fun createFragment(position: Int): Fragment {
                        return when (position) {
                            types.indexOf(NATIVE) -> NativeAnalysisFragment.newInstance(pkgName!!, NATIVE)
                            types.indexOf(DEX) -> NativeAnalysisFragment.newInstance(pkgName!!, DEX)
                            else -> ComponentsAnalysisFragment.newInstance(types[position])
                        }
                    }
                }
            }

            val mediator = TabLayoutMediator(binding.tabLayout, binding.viewpager) { tab, position ->
                tab.text = tabTitles[position]
            }
            mediator.attach()

            viewModel.itemsCountLiveData.observe(this, {
                if (currentItemsCount != it) {
                    binding.tsComponentCount.setText(it.toString())
                    currentItemsCount = it
                }
            })

            viewModel.initComponentsData(packageName)
        } ?: supportFinishAfterTransition()
    }
}
