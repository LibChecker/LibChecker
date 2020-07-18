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
import com.absinthe.libchecker.bean.LibStringItem
import com.absinthe.libchecker.constant.librarymap.NativeLibMap
import com.absinthe.libchecker.databinding.ActivityApkDetailBinding
import com.absinthe.libchecker.ktx.setLongClickCopiedToClipboard
import com.absinthe.libchecker.recyclerview.adapter.LibStringAdapter
import com.absinthe.libchecker.ui.fragment.applist.ComponentsAnalysisFragment
import com.absinthe.libchecker.ui.fragment.applist.NativeAnalysisFragment
import com.absinthe.libchecker.utils.PackageUtils
import com.blankj.utilcode.util.BarUtils
import com.blankj.utilcode.util.FileIOUtils
import com.blankj.utilcode.util.ToastUtils
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

        try {
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
                        tvAppName.apply {
                            text = it.applicationInfo.loadLabel(packageManager)
                            setLongClickCopiedToClipboard(text.toString())
                        }
                        tvPackageName.apply {
                            text = packageInfo.packageName
                            setLongClickCopiedToClipboard(text.toString())
                        }
                        tvVersion.apply {
                            text = "${it.versionName}(${it.versionCode})"
                            setLongClickCopiedToClipboard(text.toString())
                        }
                        tvTargetApi.text = "API ${it.applicationInfo.targetSdkVersion}"

                        val abi = PackageUtils.getAbi(
                            it.applicationInfo.sourceDir,
                            it.applicationInfo.nativeLibraryDir,
                            isApk = true
                        )

                        layoutAbi.tvAbi.text = PackageUtils.getAbiString(abi)
                        layoutAbi.ivAbiType.setImageResource(PackageUtils.getAbiBadgeResource(abi))
                    } catch (e: Exception) {
                        supportFinishAfterTransition()
                    }
                }
            } ?: finish()
        } catch (e: Exception) {
            ToastUtils.showShort("Please use another File Manager to open the APK")
            finish()
        }

        binding.viewpager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int {
                return 5
            }

            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> NativeAnalysisFragment.newInstance(tempFile!!.path)
                    1 -> ComponentsAnalysisFragment.newInstance(tempFile!!.path, LibStringAdapter.Mode.SERVICE)
                    2 -> ComponentsAnalysisFragment.newInstance(tempFile!!.path, LibStringAdapter.Mode.ACTIVITY)
                    3 -> ComponentsAnalysisFragment.newInstance(tempFile!!.path, LibStringAdapter.Mode.RECEIVER)
                    else -> ComponentsAnalysisFragment.newInstance(tempFile!!.path, LibStringAdapter.Mode.PROVIDER)
                }
            }
        }

        val mediator = TabLayoutMediator(binding.tabLayout, binding.viewpager,
            TabLayoutMediator.TabConfigurationStrategy { tab, position ->
                when (position) {
                    0 -> tab.text = getText(R.string.ref_category_native)
                    1 -> tab.text = getText(R.string.ref_category_service)
                    2 -> tab.text = getText(R.string.ref_category_activity)
                    3 -> tab.text = getText(R.string.ref_category_br)
                    else -> tab.text = getText(R.string.ref_category_cp)
                }
            })
        mediator.attach()

        if (libList.isEmpty()) {
            libList.add(LibStringItem(getString(R.string.empty_list)))
        } else {
            libList.sortByDescending {
                NativeLibMap.contains(it.name)
            }
        }
    }
}
