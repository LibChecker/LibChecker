package com.absinthe.libchecker.ui.detail

import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ImageSpan
import android.view.MenuItem
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.*
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.databinding.ActivityAppDetailBinding
import com.absinthe.libchecker.extensions.setLongClickCopiedToClipboard
import com.absinthe.libchecker.ui.app.CheckPackageOnResumingActivity
import com.absinthe.libchecker.ui.fragment.detail.*
import com.absinthe.libchecker.ui.fragment.detail.impl.ComponentsAnalysisFragment
import com.absinthe.libchecker.ui.fragment.detail.impl.DexAnalysisFragment
import com.absinthe.libchecker.ui.fragment.detail.impl.NativeAnalysisFragment
import com.absinthe.libchecker.ui.main.EXTRA_REF_NAME
import com.absinthe.libchecker.ui.main.EXTRA_REF_TYPE
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.view.detail.CenterAlignImageSpan
import com.absinthe.libchecker.viewmodel.DetailViewModel
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.zhanghai.android.appiconloader.AppIconLoader
import timber.log.Timber
import java.lang.StringBuilder


const val EXTRA_PACKAGE_NAME = "android.intent.extra.PACKAGE_NAME"

class AppDetailActivity : CheckPackageOnResumingActivity(), IDetailContainer {

    private lateinit var binding: ActivityAppDetailBinding
    private val pkgName by lazy { intent.getStringExtra(EXTRA_PACKAGE_NAME) }
    private val refName by lazy { intent.getStringExtra(EXTRA_REF_NAME) }
    private val refType by lazy { intent.getIntExtra(EXTRA_REF_TYPE, ALL) }
    private val viewModel by viewModels<DetailViewModel>()

    override var detailFragmentManager: DetailFragmentManager = DetailFragmentManager()

    override fun requirePackageName() = pkgName

    override fun setViewBinding(): ViewGroup {
        isPaddingToolbar = true
        binding = ActivityAppDetailBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        resolveReferenceExtras()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setRootPadding()
    }

    private fun initView() {
        setRootPadding()
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        pkgName?.let { packageName ->
            viewModel.packageName = packageName
            supportActionBar?.apply {
                title = try {
                    PackageUtils.getPackageInfo(packageName).applicationInfo.loadLabel(packageManager).toString()
                } catch (e: PackageManager.NameNotFoundException) {
                    getString(R.string.detail_label)
                }
            }
            binding.apply {
                try {
                    val packageInfo = PackageUtils.getPackageInfo(packageName)
                    ivAppIcon.apply {
                        val appIconLoader = AppIconLoader(resources.getDimensionPixelSize(R.dimen.lib_detail_icon_size), false, this@AppDetailActivity)
                        load(appIconLoader.loadIcon(packageInfo.applicationInfo))
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
                        text = PackageUtils.getPackageInfo(packageName).applicationInfo.loadLabel(packageManager).toString()
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

                    val abi = PackageUtils.getAbi(packageInfo.applicationInfo, isApk = false)
                    viewModel.is32bit = PackageUtils.is32bit(abi)

                    val extraInfo = StringBuilder()
                        .append(PackageUtils.getAbiString(this@AppDetailActivity, abi, true))
                        .append(", ")
                        .append(PackageUtils.getTargetApiString(packageName))
                        .append(", ")
                        .append(PackageUtils.getMinSdkVersion(packageInfo))
                    packageInfo.sharedUserId?.let {
                        extraInfo.append("\nsharedUserId = $it")
                    }
                    val spanString: SpannableString
                    if (abi != Constants.OVERLAY && abi != Constants.ERROR) {
                        spanString = SpannableString("  $extraInfo")
                        ContextCompat.getDrawable(this@AppDetailActivity, PackageUtils.getAbiBadgeResource(abi))?.let {
                            it.setBounds(0, 0, it.intrinsicWidth, it.intrinsicHeight)
                            val span = CenterAlignImageSpan(it)
                            spanString.setSpan(span, 0, 1, ImageSpan.ALIGN_BOTTOM)
                        }
                        tvExtraInfo.text = spanString
                    } else {
                        tvExtraInfo.text = extraInfo
                    }

                    lifecycleScope.launch(Dispatchers.IO) {
                        val lcItem = Repositories.lcRepository.getItem(packageName)

                        val isSplitApk = lcItem?.isSplitApk ?: false
                        val isKotlinUsed = lcItem?.isKotlinUsed ?: false

                        withContext(Dispatchers.Main) {
                            if (isSplitApk || isKotlinUsed) {
                                chipGroup.isVisible = true
                                chipAppBundle.apply {
                                    isVisible = isSplitApk
                                    setOnClickListener {
                                        AppBundleBottomSheetDialogFragment().apply {
                                            arguments = Bundle().apply {
                                                putString(EXTRA_PACKAGE_NAME, pkgName)
                                            }
                                            show(supportFragmentManager, tag)
                                        }
                                    }
                                }
                                chipKotlinUsed.apply {
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
                                chipGroup.isVisible = false
                            }
                        }
                    }
                } catch (e: PackageManager.NameNotFoundException) {
                    ivAppIcon.load(R.drawable.ic_app_list)
                    ivAppIcon.imageTintList = ColorStateList.valueOf(getColor(R.color.textNormal))
                } catch (e: Exception) {
                    Timber.e(e)
                    finish()
                }

                ibSort.setOnClickListener {
                    lifecycleScope.launch {
                        detailFragmentManager.sortAll()
                        withContext(Dispatchers.Main) {
                            viewModel.sortMode = if (viewModel.sortMode == MODE_SORT_BY_LIB) {
                                MODE_SORT_BY_SIZE
                            } else {
                                MODE_SORT_BY_LIB
                            }
                            detailFragmentManager.changeSortMode(viewModel.sortMode)
                        }
                    }
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
                            types.indexOf(DEX) -> DexAnalysisFragment.newInstance(pkgName!!, DEX)
                            else -> ComponentsAnalysisFragment.newInstance(types[position])
                        }
                    }
                }
            }
            binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    val count = viewModel.itemsCountList[tab.position]
                    if (detailFragmentManager.currentItemsCount != count) {
                        binding.tsComponentCount.setText(count.toString())
                        detailFragmentManager.currentItemsCount = count
                    }
                    detailFragmentManager.selectedPosition = tab.position
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {}

                override fun onTabReselected(tab: TabLayout.Tab?) {}
            })

            val mediator = TabLayoutMediator(binding.tabLayout, binding.viewpager) { tab, position ->
                tab.text = tabTitles[position]
            }
            mediator.attach()

            viewModel.itemsCountLiveData.observe(this, {
                if (detailFragmentManager.currentItemsCount != it.count && binding.tabLayout.selectedTabPosition == it.locate) {
                    binding.tsComponentCount.setText(it.count.toString())
                    detailFragmentManager.currentItemsCount = it.count
                }
            })

            viewModel.initComponentsData(packageName)
        } ?: supportFinishAfterTransition()
    }

    private fun resolveReferenceExtras() {
        if (pkgName == null || refName == null || refType == ALL) {
            return
        }
        navigateToReferenceComponentPosition(pkgName!!, refName!!)
    }

    private fun navigateToReferenceComponentPosition(packageName: String, refName: String) {
        binding.viewpager.currentItem = refType
        detailFragmentManager.navigateToComponent(refType, refName.removePrefix(packageName))
    }
}
