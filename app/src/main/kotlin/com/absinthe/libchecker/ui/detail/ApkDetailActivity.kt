package com.absinthe.libchecker.ui.detail

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import coil.load
import com.absinthe.libchecker.BaseActivity
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.*
import com.absinthe.libchecker.bean.LibStringItem
import com.absinthe.libchecker.constant.librarymap.NativeLibMap
import com.absinthe.libchecker.databinding.ActivityAppDetailBinding
import com.absinthe.libchecker.extensions.setLongClickCopiedToClipboard
import com.absinthe.libchecker.ui.fragment.applist.ComponentsAnalysisFragment
import com.absinthe.libchecker.ui.fragment.applist.NativeAnalysisFragment
import com.absinthe.libchecker.ui.fragment.applist.Sortable
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.viewmodel.DetailViewModel
import com.blankj.utilcode.util.FileIOUtils
import com.google.android.material.tabs.TabLayoutMediator
import java.io.File

class ApkDetailActivity : BaseActivity() {

    private lateinit var binding: ActivityAppDetailBinding
    private var tempFile: File? = null
    private var currentItemsCount = -1

    private val viewModel by viewModels<DetailViewModel>()

    override fun setViewBinding(): ViewGroup {
        isPaddingToolbar = true
        binding = ActivityAppDetailBinding.inflate(layoutInflater)
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
        val libList = ArrayList<LibStringItem>()

        try {
            val inputStream = contentResolver.openInputStream(uri)

            FileIOUtils.writeFileFromIS(tempFile, inputStream)

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
                        ivAppIcon.load(it.applicationInfo.loadIcon(packageManager))
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
                        layoutAbi.ivAbiType.load(PackageUtils.getAbiBadgeResource(abi))
                    } catch (e: Exception) {
                        supportFinishAfterTransition()
                    }

                    ibSort.setOnClickListener {
                        Sortable.currentReference?.get()?.sort()
                    }
                }

                viewModel.itemsCountLiveData.observe(this, { count ->
                    if (currentItemsCount != count) {
                        binding.tsComponentCount.setText(count.toString())
                        currentItemsCount = count
                    }
                })

                viewModel.initComponentsData(path)
            } ?: finish()
        } catch (e: Exception) {
            Toasty.show(this, R.string.toast_use_another_file_manager)
            finish()
        }

        val types = listOf(
            NATIVE, SERVICE, ACTIVITY, RECEIVER, PROVIDER/*, DEX*/
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
                    types.indexOf(DEX) -> NativeAnalysisFragment.newInstance(path, DEX)
                    else -> ComponentsAnalysisFragment.newInstance(types[position])
                }
            }
        }

        val mediator = TabLayoutMediator(binding.tabLayout, binding.viewpager) { tab, position ->
            tab.text = tabTitles[position]
        }
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
