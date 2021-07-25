package com.absinthe.libchecker.ui.detail

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.text.style.StrikethroughSpan
import android.view.MenuItem
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.ALL
import com.absinthe.libchecker.annotation.DEX
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.annotation.STATIC
import com.absinthe.libchecker.bean.DetailExtraBean
import com.absinthe.libchecker.constant.AbilityType
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.databinding.ActivityAppDetailBinding
import com.absinthe.libchecker.ui.app.CheckPackageOnResumingActivity
import com.absinthe.libchecker.ui.fragment.detail.AppBundleBottomSheetDialogFragment
import com.absinthe.libchecker.ui.fragment.detail.AppInfoBottomSheetDialogFragment
import com.absinthe.libchecker.ui.fragment.detail.DetailFragmentManager
import com.absinthe.libchecker.ui.fragment.detail.MODE_SORT_BY_LIB
import com.absinthe.libchecker.ui.fragment.detail.MODE_SORT_BY_SIZE
import com.absinthe.libchecker.ui.fragment.detail.impl.AbilityAnalysisFragment
import com.absinthe.libchecker.ui.fragment.detail.impl.ComponentsAnalysisFragment
import com.absinthe.libchecker.ui.fragment.detail.impl.DexAnalysisFragment
import com.absinthe.libchecker.ui.fragment.detail.impl.NativeAnalysisFragment
import com.absinthe.libchecker.ui.fragment.detail.impl.StaticAnalysisFragment
import com.absinthe.libchecker.ui.main.EXTRA_REF_NAME
import com.absinthe.libchecker.ui.main.EXTRA_REF_TYPE
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.isOrientationPortrait
import com.absinthe.libchecker.utils.extensions.setLongClickCopiedToClipboard
import com.absinthe.libchecker.utils.extensions.unsafeLazy
import com.absinthe.libchecker.utils.harmony.ApplicationDelegate
import com.absinthe.libchecker.utils.manifest.ManifestReader
import com.absinthe.libchecker.view.detail.CenterAlignImageSpan
import com.absinthe.libchecker.viewmodel.DetailViewModel
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import me.zhanghai.android.appiconloader.AppIconLoader
import ohos.bundle.IBundleManager
import timber.log.Timber
import java.io.File

const val EXTRA_PACKAGE_NAME = "android.intent.extra.PACKAGE_NAME"
const val EXTRA_DETAIL_BEAN = "EXTRA_DETAIL_BEAN"

class AppDetailActivity : CheckPackageOnResumingActivity(), IDetailContainer {

    private lateinit var binding: ActivityAppDetailBinding
    private val pkgName by unsafeLazy { intent.getStringExtra(EXTRA_PACKAGE_NAME) }
    private val refName by unsafeLazy { intent.getStringExtra(EXTRA_REF_NAME) }
    private val refType by unsafeLazy { intent.getIntExtra(EXTRA_REF_TYPE, ALL) }
    private val extraBean by unsafeLazy { intent.getParcelableExtra(EXTRA_DETAIL_BEAN) as? DetailExtraBean }
    private val bundleManager by unsafeLazy { ApplicationDelegate(this).iBundleManager }
    private val viewModel: DetailViewModel by viewModels()

    private var isHarmonyMode = false

    override var detailFragmentManager: DetailFragmentManager = DetailFragmentManager()

    override fun requirePackageName() = pkgName

    override fun setViewBinding(): ViewGroup {
        binding = ActivityAppDetailBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        resolveReferenceExtras()
    }

    override fun onStart() {
        super.onStart()
        registerPackageBroadcast()
    }

    override fun onStop() {
        super.onStop()
        unregisterPackageBroadcast()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initView() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        pkgName?.let { packageName ->
            viewModel.packageName = packageName
            binding.apply {
                try {
                    val packageInfo = PackageUtils.getPackageInfo(packageName)
                    supportActionBar?.apply {
                        title = try {
                            packageInfo.applicationInfo.loadLabel(packageManager).toString()
                        } catch (e: PackageManager.NameNotFoundException) {
                            getString(R.string.detail_label)
                        }
                    }
                    ivAppIcon.apply {
                        val appIconLoader = AppIconLoader(
                            resources.getDimensionPixelSize(R.dimen.lib_detail_icon_size),
                            false,
                            this@AppDetailActivity
                        )
                        load(appIconLoader.loadIcon(packageInfo.applicationInfo))
                        setOnClickListener {
                            AppInfoBottomSheetDialogFragment().apply {
                                arguments = bundleOf(
                                    EXTRA_PACKAGE_NAME to pkgName
                                )
                                show(supportFragmentManager, tag)
                            }
                        }
                    }
                    tvAppName.apply {
                        text = packageInfo.applicationInfo.loadLabel(packageManager).toString()
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

                    val extraInfo = SpannableStringBuilder()
                    val file = File(packageInfo.applicationInfo.sourceDir)
                    val demands = ManifestReader.getManifestProperties(
                        file,
                        listOf(
                            PackageUtils.use32bitAbiString,
                            PackageUtils.multiArchString,
                            PackageUtils.overlayString
                        ).toTypedArray()
                    )
                    val overlay = demands[PackageUtils.overlayString] as? Boolean ?: false
                    val abiSet = PackageUtils.getAbiSet(
                        file,
                        packageInfo.applicationInfo,
                        isApk = false,
                        overlay = overlay,
                        ignoreArch = true
                    )
                    val abi = PackageUtils.getAbi(abiSet, demands)
                    viewModel.is32bit = PackageUtils.is32bit(abi)

                    if (abiSet.isNotEmpty() && !abiSet.contains(Constants.OVERLAY) && !abiSet.contains(
                            Constants.ERROR
                        )
                    ) {
                        val spanStringBuilder = SpannableStringBuilder()
                        var spanString: SpannableString
                        var firstLoop = true
                        var itemCount = 0
                        abiSet.forEach {
                            itemCount++
                            if (firstLoop) {
                                firstLoop = false
                            }
                            spanString = SpannableString(
                                "  ${
                                PackageUtils.getAbiString(
                                    this@AppDetailActivity,
                                    it,
                                    false
                                )
                                }"
                            )
                            ContextCompat.getDrawable(
                                this@AppDetailActivity,
                                PackageUtils.getAbiBadgeResource(it)
                            )?.mutate()?.let { drawable ->
                                drawable.setBounds(
                                    0,
                                    0,
                                    drawable.intrinsicWidth,
                                    drawable.intrinsicHeight
                                )
                                if (it != abi % Constants.MULTI_ARCH) {
                                    drawable.alpha = 128
                                } else {
                                    drawable.alpha = 255
                                }
                                val span = CenterAlignImageSpan(drawable)
                                spanString.setSpan(span, 0, 1, ImageSpan.ALIGN_BOTTOM)
                            }
                            if (it != abi % Constants.MULTI_ARCH) {
                                spanString.setSpan(
                                    StrikethroughSpan(),
                                    2,
                                    spanString.length,
                                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE
                                )
                            }
                            spanStringBuilder.append(spanString)
                            if (itemCount < abiSet.size) {
                                spanStringBuilder.append(", ")
                            }
                            if (itemCount == 3 && isOrientationPortrait) {
                                spanStringBuilder.appendLine()
                            }
                        }
                        extraInfo.append(spanStringBuilder).appendLine()
                    }

                    val advanced = when (abi) {
                        Constants.ERROR -> getString(R.string.cannot_read)
                        Constants.OVERLAY -> Constants.OVERLAY_STRING
                        else -> ""
                    }
                    extraInfo.apply {
                        append(advanced)
                        if (abi >= Constants.MULTI_ARCH) {
                            if (advanced.isNotEmpty()) {
                                append(", ")
                            }
                            append(getString(R.string.multiArch))
                            append(", ")
                        }
                        if (!isHarmonyMode) {
                            append(PackageUtils.getTargetApiString(packageName))
                            append(", ").append(PackageUtils.getMinSdkVersion(packageInfo))
                            packageInfo.sharedUserId?.let {
                                appendLine().append("sharedUserId = $it")
                            }
                        } else {
                            if (extraBean != null && extraBean!!.variant == Constants.VARIANT_HAP) {
                                bundleManager?.let {
                                    val hapBundle = it.getBundleInfo(
                                        packageName,
                                        IBundleManager.GET_BUNDLE_DEFAULT
                                    )
                                    append("targetVersion ${hapBundle.targetVersion}")
                                    append(", ").append("minSdkVersion ${hapBundle.minSdkVersion}")
                                    if (!hapBundle.jointUserId.isNullOrEmpty()) {
                                        appendLine().append("jointUserId = ${hapBundle.jointUserId}")
                                    }
                                }
                            }
                        }
                    }
                    tvExtraInfo.text = extraInfo

                    extraBean?.let {
                        if (it.isSplitApk || it.isKotlinUsed) {
                            chipGroup.isVisible = true
                            chipAppBundle.apply {
                                isVisible = it.isSplitApk
                                setOnClickListener {
                                    AppBundleBottomSheetDialogFragment().apply {
                                        arguments = bundleOf(
                                            EXTRA_PACKAGE_NAME to pkgName
                                        )
                                        show(supportFragmentManager, tag)
                                    }
                                }
                            }
                            chipKotlinUsed.apply {
                                isVisible = it.isKotlinUsed
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
                        if (it.variant == Constants.VARIANT_HAP) {
                            ibHarmonyBadge.isVisible = true
                            ibHarmonyBadge.setImageResource(R.drawable.ic_harmonyos_logo)
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
                        viewModel.sortMode = if (viewModel.sortMode == MODE_SORT_BY_LIB) {
                            MODE_SORT_BY_SIZE
                        } else {
                            MODE_SORT_BY_LIB
                        }
                        detailFragmentManager.changeSortMode(viewModel.sortMode)
                    }
                }

                if (ibHarmonyBadge.isVisible) {
                    ibHarmonyBadge.setOnClickListener {
                        isHarmonyMode = !isHarmonyMode
                        initView()
                    }
                }
            }

            val types = if (!isHarmonyMode) {
                mutableListOf(
                    NATIVE, SERVICE, ACTIVITY, RECEIVER, PROVIDER, DEX
                )
            } else {
                mutableListOf(
                    NATIVE,
                    AbilityType.PAGE,
                    AbilityType.SERVICE,
                    AbilityType.WEB,
                    AbilityType.DATA,
                    DEX
                )
            }
            val tabTitles = if (!isHarmonyMode) {
                mutableListOf(
                    getText(R.string.ref_category_native),
                    getText(R.string.ref_category_service),
                    getText(R.string.ref_category_activity),
                    getText(R.string.ref_category_br),
                    getText(R.string.ref_category_cp),
                    getText(R.string.ref_category_dex)
                )
            } else {
                mutableListOf(
                    getText(R.string.ref_category_native),
                    getText(R.string.ability_page),
                    getText(R.string.ability_service),
                    getText(R.string.ability_web),
                    getText(R.string.ability_data),
                    getText(R.string.ref_category_dex)
                )
            }
            try {
                if (PackageUtils.getStaticLibs(PackageUtils.getPackageInfo(packageName))
                    .isNotEmpty()
                ) {
                    types.add(1, STATIC)
                    tabTitles.add(1, getText(R.string.ref_category_static))
                }
            } catch (e: PackageManager.NameNotFoundException) {
                Timber.e(e)
            }

            binding.viewpager.apply {
                adapter = object : FragmentStateAdapter(this@AppDetailActivity) {
                    override fun getItemCount(): Int {
                        return types.size
                    }

                    override fun createFragment(position: Int): Fragment {
                        return when (val type = types[position]) {
                            NATIVE -> NativeAnalysisFragment.newInstance(packageName, NATIVE)
                            STATIC -> StaticAnalysisFragment.newInstance(packageName, STATIC)
                            DEX -> DexAnalysisFragment.newInstance(packageName, DEX)
                            else -> if (!isHarmonyMode) {
                                ComponentsAnalysisFragment.newInstance(type)
                            } else {
                                AbilityAnalysisFragment.newInstance(type)
                            }
                        }
                    }
                }
            }
            binding.tabLayout.apply {
                removeAllTabs()
                tabTitles.forEach {
                    addTab(newTab().apply { text = it })
                }
                addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                    override fun onTabSelected(tab: TabLayout.Tab) {
                        val count = viewModel.itemsCountList[types[tab.position]]
                        if (detailFragmentManager.currentItemsCount != count) {
                            binding.tsComponentCount.setText(count.toString())
                            detailFragmentManager.currentItemsCount = count
                        }
                        detailFragmentManager.selectedPosition = tab.position
                    }

                    override fun onTabUnselected(tab: TabLayout.Tab?) {}

                    override fun onTabReselected(tab: TabLayout.Tab?) {}
                })
            }

            val mediator =
                TabLayoutMediator(binding.tabLayout, binding.viewpager) { tab, position ->
                    tab.text = tabTitles[position]
                }
            mediator.attach()

            viewModel.itemsCountLiveData.observe(this) {
                if (detailFragmentManager.currentItemsCount != it.count && types[binding.tabLayout.selectedTabPosition] == it.locate) {
                    binding.tsComponentCount.setText(it.count.toString())
                    detailFragmentManager.currentItemsCount = it.count
                }
            }

            if (!isHarmonyMode) {
                viewModel.initComponentsData(packageName)
            } else {
                viewModel.initAbilities(packageName)
            }
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

    private val requestPackageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val pkg = intent.data?.schemeSpecificPart.orEmpty()
            if (pkg == pkgName) {
                GlobalValues.shouldRequestChange.postValue(true)
                recreate()
            }
        }
    }

    private fun registerPackageBroadcast() {
        val intentFilter = IntentFilter().apply {
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
