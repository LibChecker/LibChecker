package com.absinthe.libchecker.ui.detail

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.absinthe.libchecker.BaseActivity
import com.absinthe.libchecker.R
import com.absinthe.libchecker.bean.ARMV5
import com.absinthe.libchecker.bean.ARMV7
import com.absinthe.libchecker.bean.ARMV8
import com.absinthe.libchecker.bean.LibStringItem
import com.absinthe.libchecker.constant.librarymap.NativeLibMap
import com.absinthe.libchecker.databinding.ActivityApkDetailBinding
import com.absinthe.libchecker.ui.fragment.applist.ComponentsAnalysisFragment
import com.absinthe.libchecker.ui.fragment.applist.SoAnalysisFragment
import com.absinthe.libchecker.utils.PackageUtils
import com.blankj.utilcode.util.BarUtils
import com.blankj.utilcode.util.FileIOUtils
import com.google.android.material.tabs.TabLayoutMediator
import java.io.File

class ApkDetailActivity : BaseActivity() {

    private lateinit var binding: ActivityApkDetailBinding
    private var tempFile: File? = null

    init {
        isPaddingToolbar = true
    }

    override fun setViewBinding(): View {
        binding = ActivityApkDetailBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent.data?.scheme?.let { scheme ->
            if (scheme == "content") {
                intent.data?.let {
                    initView(it)
                }
            }
        }
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

    @SuppressLint("SetTextI18n")
    private fun initView(uri: Uri) {
        setRootPadding()
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        initData(uri)
    }

    private fun setRootPadding() {
        val isLandScape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        binding.root.apply {
            fitsSystemWindows = isLandScape
            setPadding(0, if (isLandScape) 0 else BarUtils.getStatusBarHeight(), 0, 0)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initData(uri: Uri) {
        val libList = ArrayList<LibStringItem>()
        val inputStream = contentResolver.openInputStream(uri)
        tempFile = File(externalCacheDir, "temp.apk")

        FileIOUtils.writeFileFromIS(tempFile, inputStream)

        val path = tempFile!!.path
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
                    ivAppIcon.apply {
                        setImageDrawable(it.applicationInfo.loadIcon(packageManager))
                    }
                    tvAppName.text = it.applicationInfo.loadLabel(packageManager)
                    tvPackageName.text = packageInfo.packageName
                    tvVersion.text = "${it.versionName}(${it.versionCode})"
                    tvTargetApi.text = "API ${it.applicationInfo.targetSdkVersion}"

                    val abi = PackageUtils.getAbi(
                        it.applicationInfo.sourceDir,
                        it.applicationInfo.nativeLibraryDir
                    )

                    layoutAbi.tvAbi.text = PackageUtils.getAbiString(abi)
                    layoutAbi.ivAbiType.setImageResource(
                        when (abi) {
                            ARMV8 -> R.drawable.ic_64bit
                            ARMV7, ARMV5 -> R.drawable.ic_32bit
                            else -> R.drawable.ic_no_libs
                        }
                    )
                } catch (e: Exception) {
                    supportFinishAfterTransition()
                }
            }
        } ?: finish()

        binding.viewpager.adapter = object : FragmentStateAdapter(this@ApkDetailActivity) {
            override fun getItemCount(): Int {
                return 2
            }

            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> SoAnalysisFragment.newInstance(tempFile!!.path)
                    else -> ComponentsAnalysisFragment.newInstance(tempFile!!.path)
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

        if (libList.isEmpty()) {
            libList.add(LibStringItem(getString(R.string.empty_list), 0))
        } else {
            libList.sortByDescending {
                NativeLibMap.contains(it.name)
            }
        }
    }
}
