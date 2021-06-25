package com.absinthe.libchecker.ui.detail

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ImageSpan
import android.view.MenuItem
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import coil.load
import com.absinthe.libchecker.BaseActivity
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.*
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.databinding.ActivityAppDetailBinding
import com.absinthe.libchecker.extensions.setLongClickCopiedToClipboard
import com.absinthe.libchecker.ui.fragment.detail.*
import com.absinthe.libchecker.ui.fragment.detail.impl.ComponentsAnalysisFragment
import com.absinthe.libchecker.ui.fragment.detail.impl.DexAnalysisFragment
import com.absinthe.libchecker.ui.fragment.detail.impl.NativeAnalysisFragment
import com.absinthe.libchecker.utils.FileUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.view.detail.CenterAlignImageSpan
import com.absinthe.libchecker.viewmodel.DetailViewModel
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.zhanghai.android.appiconloader.AppIconLoader
import timber.log.Timber
import java.io.File
import java.io.InputStream

class ApkDetailActivity : BaseActivity(), IDetailContainer {

    private lateinit var binding: ActivityAppDetailBinding
    private var tempFile: File? = null

    private val viewModel by viewModels<DetailViewModel>()

    override var detailFragmentManager: DetailFragmentManager = DetailFragmentManager()

    override fun setViewBinding(): ViewGroup {
        isPaddingToolbar = true
        binding = ActivityAppDetailBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.isApk = true
        intent.data?.scheme?.let { scheme ->
            if (scheme == "content") {
                intent.data?.let {
                    initView(it)
                }
            }
        }
    }

    override fun onDestroy() {
        tempFile?.delete()
        super.onDestroy()
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

    private fun initView(uri: Uri) {
        setRootPadding()
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        initData(uri)
    }

    @SuppressLint("SetTextI18n")
    private fun initData(uri: Uri) {
        tempFile = File(externalCacheDir, "temp.apk")

        val path = tempFile!!.path
        var inputStream: InputStream? = null

        try {
            inputStream = contentResolver.openInputStream(uri)

            FileUtils.writeFileFromIS(tempFile!!, inputStream)

            val packageInfo = packageManager.getPackageArchiveInfo(path, 0)
            packageInfo?.let {
                //Refer to https://juejin.im/post/5cb41f7b6fb9a0688b574228
                it.applicationInfo.sourceDir = path
                it.applicationInfo.publicSourceDir = path

                supportActionBar?.apply {
                    title = it.applicationInfo.loadLabel(packageManager)
                }
                binding.apply {
                    try {
                        val appIconLoader = AppIconLoader(resources.getDimensionPixelSize(R.dimen.lib_detail_icon_size), false, this@ApkDetailActivity)
                        ivAppIcon.load(appIconLoader.loadIcon(it.applicationInfo))
                        tvAppName.apply {
                            text = it.applicationInfo.loadLabel(packageManager)
                            setLongClickCopiedToClipboard(text.toString())
                        }
                        tvPackageName.apply {
                            text = packageInfo.packageName
                            setLongClickCopiedToClipboard(text.toString())
                        }
                        tvVersion.apply {
                            text = PackageUtils.getVersionString(it)
                            setLongClickCopiedToClipboard(text.toString())
                        }

                        val abi = PackageUtils.getAbi(it.applicationInfo, isApk = true)
                        val str = "${PackageUtils.getAbiString(this@ApkDetailActivity, abi, true)}, ${PackageUtils.getTargetApiString(packageInfo)}, ${PackageUtils.getMinSdkVersion(packageInfo)}"
                        val spanString: SpannableString
                        if (abi != Constants.OVERLAY) {
                            spanString = SpannableString("  $str")
                            ContextCompat.getDrawable(this@ApkDetailActivity, PackageUtils.getAbiBadgeResource(abi))?.let { drawable ->
                                drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
                                val span = CenterAlignImageSpan(drawable)
                                spanString.setSpan(span, 0, 1, ImageSpan.ALIGN_BOTTOM)
                            }
                            tvAbiAndApi.text = spanString
                        } else {
                            tvAbiAndApi.text = str
                        }
                    } catch (e: Exception) {
                        Timber.e(e)
                        finish()
                    }

                    ibSort.setOnClickListener {
                        lifecycleScope.launch {
                            detailFragmentManager.sortAll()
                            withContext(Dispatchers.Main) {
                                viewModel.sortMode = if (viewModel.sortMode == MODE_SORT_BY_LIB) { MODE_SORT_BY_SIZE } else { MODE_SORT_BY_LIB }
                                detailFragmentManager.changeSortMode(viewModel.sortMode)
                            }
                        }
                    }
                }

                viewModel.itemsCountLiveData.observe(this, { locatedCount ->
                    if (detailFragmentManager.currentItemsCount != locatedCount.count && binding.tabLayout.selectedTabPosition == locatedCount.locate) {
                        binding.tsComponentCount.setText(locatedCount.count.toString())
                        detailFragmentManager.currentItemsCount = locatedCount.count
                    }
                })

                viewModel.initComponentsData(path)
            } ?: run {
                Timber.e("PackageInfo is null")
                finish()
            }
        } catch (e: Exception) {
            Toasty.show(this, R.string.toast_use_another_file_manager)
            finish()
        } finally {
            inputStream?.close()
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

        binding.viewpager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int {
                return types.size
            }

            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    types.indexOf(NATIVE) -> NativeAnalysisFragment.newInstance(path, NATIVE)
                    types.indexOf(DEX) -> DexAnalysisFragment.newInstance(path, DEX)
                    else -> ComponentsAnalysisFragment.newInstance(types[position])
                }
            }
        }
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener{
            override fun onTabSelected(tab: TabLayout.Tab) {
                val count = viewModel.itemsCountList[tab.position]
                if (detailFragmentManager.currentItemsCount != count) {
                    binding.tsComponentCount.setText(count.toString())
                    detailFragmentManager.currentItemsCount = count
                }
                detailFragmentManager.selectedPosition = tab.position
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) { }

            override fun onTabReselected(tab: TabLayout.Tab?) { }
        })

        val mediator = TabLayoutMediator(binding.tabLayout, binding.viewpager) { tab, position ->
            tab.text = tabTitles[position]
        }
        mediator.attach()
    }
}
