package com.absinthe.libchecker.ui.detail

import android.content.ClipData
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.text.style.StrikethroughSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.core.text.HtmlCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import coil.load
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.DEX
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.annotation.STATIC
import com.absinthe.libchecker.base.BaseAlertDialogBuilder
import com.absinthe.libchecker.bean.AppDetailToolbarItem
import com.absinthe.libchecker.bean.DetailExtraBean
import com.absinthe.libchecker.bean.FeatureItem
import com.absinthe.libchecker.compat.VersionCompat
import com.absinthe.libchecker.constant.AbilityType
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.databinding.ActivityAppDetailBinding
import com.absinthe.libchecker.recyclerview.adapter.detail.AppDetailToolbarAdapter
import com.absinthe.libchecker.recyclerview.adapter.detail.FeatureAdapter
import com.absinthe.libchecker.recyclerview.adapter.detail.ProcessBarAdapter
import com.absinthe.libchecker.ui.app.CheckPackageOnResumingActivity
import com.absinthe.libchecker.ui.fragment.detail.AppBundleBottomSheetDialogFragment
import com.absinthe.libchecker.ui.fragment.detail.AppInfoBottomSheetDialogFragment
import com.absinthe.libchecker.ui.fragment.detail.DetailFragmentManager
import com.absinthe.libchecker.ui.fragment.detail.MODE_SORT_BY_LIB
import com.absinthe.libchecker.ui.fragment.detail.MODE_SORT_BY_SIZE
import com.absinthe.libchecker.ui.fragment.detail.impl.AbilityAnalysisFragment
import com.absinthe.libchecker.ui.fragment.detail.impl.ComponentsAnalysisFragment
import com.absinthe.libchecker.ui.fragment.detail.impl.DexAnalysisFragment
import com.absinthe.libchecker.ui.fragment.detail.impl.MetaDataAnalysisFragment
import com.absinthe.libchecker.ui.fragment.detail.impl.NativeAnalysisFragment
import com.absinthe.libchecker.ui.fragment.detail.impl.PermissionAnalysisFragment
import com.absinthe.libchecker.ui.fragment.detail.impl.StaticAnalysisFragment
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.PackageUtils.isKotlinUsed
import com.absinthe.libchecker.utils.PackageUtils.isOverlay
import com.absinthe.libchecker.utils.PackageUtils.isPWA
import com.absinthe.libchecker.utils.PackageUtils.isPlayAppSigning
import com.absinthe.libchecker.utils.PackageUtils.isSplitsApk
import com.absinthe.libchecker.utils.PackageUtils.isUseJetpackCompose
import com.absinthe.libchecker.utils.PackageUtils.isXposedModule
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.utils.doOnMainThreadIdle
import com.absinthe.libchecker.utils.extensions.getDrawable
import com.absinthe.libchecker.utils.extensions.isOrientationPortrait
import com.absinthe.libchecker.utils.extensions.setLongClickCopiedToClipboard
import com.absinthe.libchecker.utils.extensions.unsafeLazy
import com.absinthe.libchecker.utils.harmony.ApplicationDelegate
import com.absinthe.libchecker.view.detail.AppBarStateChangeListener
import com.absinthe.libchecker.view.detail.CenterAlignImageSpan
import com.absinthe.libchecker.view.detail.ProcessBarView
import com.absinthe.libchecker.viewmodel.DetailViewModel
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.analytics.EventProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.zhanghai.android.appiconloader.AppIconLoader
import ohos.bundle.IBundleManager
import rikka.core.util.ClipboardUtils
import timber.log.Timber
import java.io.File
import kotlin.math.abs

abstract class BaseAppDetailActivity :
  CheckPackageOnResumingActivity<ActivityAppDetailBinding>(),
  IDetailContainer,
  SearchView.OnQueryTextListener {

  protected val viewModel: DetailViewModel by viewModels()
  protected var isListReady = false
  protected var menu: Menu? = null
  protected var typeList = mutableListOf<Int>()

  override var detailFragmentManager: DetailFragmentManager = DetailFragmentManager()

  abstract val apkAnalyticsMode: Boolean
  abstract fun getToolbar(): Toolbar

  private val bundleManager by unsafeLazy { ApplicationDelegate(this).iBundleManager }
  private val featureAdapter by unsafeLazy { FeatureAdapter() }
  private val toolbarAdapter by unsafeLazy { AppDetailToolbarAdapter() }

  private var isHarmonyMode = false
  private var isToolbarCollapsed = false
  private var hasReloadVariant = false
  private var featureListView: RecyclerView? = null
  private var processBarView: ProcessBarView? = null
  private var easterEggTabA = -1
  private var easterEggTabB = -1
  private var easterEggCount = 0

  override fun onCreate(savedInstanceState: Bundle?) {
    binding = ActivityAppDetailBinding.inflate(layoutInflater)
    super.onCreate(savedInstanceState)
    setSupportActionBar(getToolbar())
    supportActionBar?.apply {
      setDisplayHomeAsUpEnabled(true)
      setDisplayShowHomeEnabled(true)
    }
  }

  protected fun onPackageInfoAvailable(packageInfo: PackageInfo, extraBean: DetailExtraBean?) {
    viewModel.packageInfo = packageInfo
    viewModel.packageInfoLiveData.postValue(packageInfo)
    binding.apply {
      try {
        supportActionBar?.title = null
        collapsingToolbar.also {
          it.setOnApplyWindowInsetsListener(null)
          it.title = runCatching {
            packageInfo.applicationInfo.loadLabel(packageManager).toString()
          }.getOrDefault(getString(R.string.detail_label))
        }
        headerLayout.addOnOffsetChangedListener(object : AppBarStateChangeListener() {
          override fun onStateChanged(appBarLayout: AppBarLayout, state: State) {
            collapsingToolbar.isTitleEnabled = state == State.COLLAPSED
          }
        })
        detailsTitle.apply {
          iconView.apply {
            val appIconLoader = AppIconLoader(
              resources.getDimensionPixelSize(R.dimen.lib_detail_icon_size),
              false,
              this@BaseAppDetailActivity
            )
            load(appIconLoader.loadIcon(packageInfo.applicationInfo))
            if (!apkAnalyticsMode || PackageUtils.isAppInstalled(packageInfo.packageName)) {
              setOnClickListener {
                if (AntiShakeUtils.isInvalidClick(it)) {
                  return@setOnClickListener
                }
                AppInfoBottomSheetDialogFragment().apply {
                  arguments = bundleOf(
                    EXTRA_PACKAGE_NAME to packageInfo.packageName
                  )
                  show(supportFragmentManager, tag)
                }
              }
            }
            setOnLongClickListener {
              (drawable as? BitmapDrawable)?.bitmap?.let { bitmap ->
                val iconFile = File(externalCacheDir, Constants.TEMP_ICON)
                if (!iconFile.exists()) {
                  iconFile.createNewFile()
                }
                iconFile.outputStream().use {
                  bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
                val uri = FileProvider.getUriForFile(
                  this@BaseAppDetailActivity,
                  BuildConfig.APPLICATION_ID + ".fileprovider",
                  iconFile
                )
                if (ClipboardUtils.put(
                    this@BaseAppDetailActivity,
                    ClipData.newUri(contentResolver, Constants.TEMP_ICON, uri)
                  )
                ) {
                  VersionCompat.showCopiedOnClipboardToast(this@BaseAppDetailActivity)
                }
              }
              true
            }
          }
          appNameView.apply {
            text = packageInfo.applicationInfo.loadLabel(packageManager).toString()
            setLongClickCopiedToClipboard(text)
          }
          packageNameView.apply {
            text = packageInfo.packageName
            setLongClickCopiedToClipboard(text)
          }
          versionInfoView.apply {
            text = PackageUtils.getVersionString(packageInfo)
            setLongClickCopiedToClipboard(text)
          }
        }

        val extraInfo = SpannableStringBuilder()
        val file = File(packageInfo.applicationInfo.sourceDir)
        val overlay = packageInfo.isOverlay()
        var abiSet = PackageUtils.getAbiSet(
          file,
          packageInfo.applicationInfo,
          isApk = apkAnalyticsMode,
          overlay = overlay,
          ignoreArch = true
        ).toSet()
        val abi = PackageUtils.getAbi(packageInfo, isApk = apkAnalyticsMode, abiSet = abiSet)
        abiSet = abiSet.sortedByDescending { it == abi }.toSet()

        extraInfo.apply {
          if (abi >= Constants.MULTI_ARCH) {
            append(getString(R.string.multiArch))
            append(", ")
          }
          if (!isHarmonyMode) {
            append(PackageUtils.getTargetApiString(packageInfo))
            append(", ").append(PackageUtils.getMinSdkVersionString(packageInfo))
            packageInfo.sharedUserId?.let {
              appendLine().append("sharedUserId = $it")
            }
          } else {
            if (extraBean?.variant == Constants.VARIANT_HAP) {
              bundleManager?.let {
                val hapBundle = it.getBundleInfo(
                  packageInfo.packageName,
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
          appendLine()
        }
        if (abiSet.isNotEmpty() && !abiSet.contains(Constants.OVERLAY) && !abiSet.contains(Constants.ERROR)) {
          val spanStringBuilder = SpannableStringBuilder()
          var spanString: SpannableString
          var firstLoop = true
          var itemCount = 0
          abiSet.forEach {
            itemCount++
            if (firstLoop) {
              firstLoop = false
            }
            PackageUtils.getAbiString(
              this@BaseAppDetailActivity,
              it,
              false
            ).let { str ->
              spanString = SpannableString("  $str")
            }
            PackageUtils.getAbiBadgeResource(it)
              .getDrawable(this@BaseAppDetailActivity)?.mutate()?.let { drawable ->
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
            if (!apkAnalyticsMode && it != abi % Constants.MULTI_ARCH) {
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
            if (itemCount == 3 && abiSet.size > 3 && isOrientationPortrait) {
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
        extraInfo.append(advanced)
        detailsTitle.extraInfoView.apply {
          text = extraInfo
          setLongClickCopiedToClipboard(text)
        }

        if (featureListView == null) {
          lifecycleScope.launch(Dispatchers.IO) {
            val isSplitApk = extraBean?.isSplitApk ?: packageInfo.isSplitsApk()
            val isKotlinUsed = extraBean?.isKotlinUsed ?: packageInfo.isKotlinUsed()

            if (isSplitApk) {
              withContext(Dispatchers.Main) {
                initFeatureListView()
                featureAdapter.addData(
                  FeatureItem(R.drawable.ic_aab) {
                    AppBundleBottomSheetDialogFragment().apply {
                      arguments = bundleOf(
                        EXTRA_PACKAGE_NAME to packageInfo.packageName
                      )
                      show(supportFragmentManager, tag)
                    }
                  }
                )
              }
            }
            if (isKotlinUsed) {
              withContext(Dispatchers.Main) {
                initFeatureListView()
                withContext(Dispatchers.IO) {
                  val kotlinPluginVersion = PackageUtils.getKotlinPluginVersion(packageInfo)
                  withContext(Dispatchers.Main) {
                    showKotlinUsedLabel(kotlinPluginVersion)
                  }
                }
              }
            }

            initMoreFeatures(packageInfo)
          }
        }
      } catch (e: Exception) {
        Timber.e(e)
        Toasty.showLong(this@BaseAppDetailActivity, e.toString())
        finish()
      }

      if (hasReloadVariant) {
        hasReloadVariant = false
        return
      }

      toolbarAdapter.addData(
        AppDetailToolbarItem(R.drawable.ic_lib_sort, R.string.menu_sort) {
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
      )
      if (extraBean?.variant == Constants.VARIANT_HAP) {
        toolbarAdapter.addData(
          AppDetailToolbarItem(R.drawable.ic_harmonyos_logo, R.string.ability) {
            if (!hasReloadVariant) {
              isHarmonyMode = !isHarmonyMode
              hasReloadVariant = true
              onPackageInfoAvailable(packageInfo, extraBean)
            }
          }
        )
      }
      if (GlobalValues.processMode && processBarView == null) {
        processBarView = ProcessBarView(this@BaseAppDetailActivity).also {
          it.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
          )
        }.also {
          it.setOnItemClickListener { isSelected, process ->
            if (isSelected) {
              viewModel.queriedProcess = process
            } else {
              viewModel.queriedProcess = null
            }
            detailFragmentManager.deliverFilterProcesses(viewModel.queriedProcess)
          }
        }
        binding.detailToolbarContainer.addView(processBarView)
      }

      rvToolbar.apply {
        adapter = toolbarAdapter
        layoutManager =
          LinearLayoutManager(this@BaseAppDetailActivity, RecyclerView.HORIZONTAL, false)
      }

      headerLayout.addOnOffsetChangedListener(
        AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
          isToolbarCollapsed = if (abs(verticalOffset) - appBarLayout.totalScrollRange == 0) {
            // Collapsed
            if (!isToolbarCollapsed && !toolbarAdapter.data.contains(toolbarQuicklyLaunchItem)) {
              toolbarAdapter.addData(toolbarQuicklyLaunchItem)
            }
            true
          } else {
            // Expanded
            if (isToolbarCollapsed && toolbarAdapter.data.contains(toolbarQuicklyLaunchItem)) {
              toolbarAdapter.remove(toolbarQuicklyLaunchItem)
            }
            false
          }
        }
      )
    }

    typeList = if (!isHarmonyMode) {
      mutableListOf(
        NATIVE, SERVICE, ACTIVITY, RECEIVER, PROVIDER, PERMISSION, METADATA, DEX
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
        getText(R.string.ref_category_perm),
        getText(R.string.ref_category_metadata),
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

    if (packageInfo.applicationInfo.sharedLibraryFiles?.isNotEmpty() == true) {
      lifecycleScope.launch(Dispatchers.IO) {
        try {
          val libs =
            PackageUtils.getStaticLibs(PackageUtils.getPackageInfo(packageInfo.packageName))
          if (libs.isNotEmpty()) {
            withContext(Dispatchers.Main) {
              typeList.add(1, STATIC)
              tabTitles.add(1, getText(R.string.ref_category_static))
              binding.tabLayout.addTab(
                binding.tabLayout.newTab()
                  .also { it.text = getText(R.string.ref_category_static) },
                1
              )
              onStaticLibsAvailable()
            }
          }
        } catch (e: PackageManager.NameNotFoundException) {
          Timber.e(e)
        }
      }
    }

    binding.viewpager.apply {
      adapter = object : FragmentStateAdapter(this@BaseAppDetailActivity) {
        override fun getItemCount(): Int {
          return typeList.size
        }

        override fun createFragment(position: Int): Fragment {
          return when (val type = typeList[position]) {
            NATIVE -> NativeAnalysisFragment.newInstance(packageInfo.packageName)
            STATIC -> StaticAnalysisFragment.newInstance(packageInfo.packageName)
            PERMISSION -> PermissionAnalysisFragment.newInstance(packageInfo.packageName)
            METADATA -> MetaDataAnalysisFragment.newInstance(packageInfo.packageName)
            DEX -> DexAnalysisFragment.newInstance(packageInfo.packageName)
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
          val count = viewModel.itemsCountList[typeList[tab.position]]
          if (detailFragmentManager.currentItemsCount != count) {
            binding.tsComponentCount.setText(count.toString())
            detailFragmentManager.currentItemsCount = count
          }
          detailFragmentManager.selectedPosition = tab.position

          if ((easterEggCount % 2) == 0) {
            if (tab.position == easterEggTabA || easterEggTabA == -1) {
              easterEggCount++
              easterEggTabA = tab.position
            } else {
              easterEggCount = 0
              easterEggTabA = -1
              easterEggTabB = -1
            }
          } else {
            if (tab.position == easterEggTabB || easterEggTabB == -1) {
              easterEggCount++
              easterEggTabB = tab.position
            } else {
              easterEggCount = 0
              easterEggTabA = -1
              easterEggTabB = -1
            }
          }
          if (easterEggCount >= 10) {
            easterEggCount = 0
            Toasty.showLong(this@BaseAppDetailActivity, "What are you doing?\uD83E\uDD14")
            Analytics.trackEvent(
              Constants.Event.EASTER_EGG,
              EventProperties().set("EASTER_EGG", "Detail page Repeated sliding")
            )
          }
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
      if (detailFragmentManager.currentItemsCount != it.count && typeList[binding.tabLayout.selectedTabPosition] == it.locate) {
        binding.tsComponentCount.setText(it.count.toString())
        detailFragmentManager.currentItemsCount = it.count
      }
    }
    viewModel.processToolIconVisibilityLiveData.observe(this) { visible ->
      if (visible) {
        if (!toolbarAdapter.data.contains(toolbarProcessItem)) {
          toolbarAdapter.addData(toolbarProcessItem)
        }
        processBarView?.isVisible = true
      } else {
        if (toolbarAdapter.data.contains(toolbarProcessItem)) {
          toolbarAdapter.remove(toolbarProcessItem)
        }
        processBarView?.isGone = true
      }
    }
    viewModel.processMapLiveData.observe(this) {
      processBarView?.setData(
        it.map { mapItem ->
          ProcessBarAdapter.ProcessBarItem(
            mapItem.key,
            mapItem.value
          )
        }
      )
    }

    if (!isHarmonyMode) {
      viewModel.initComponentsData()
    } else {
      viewModel.initAbilities(packageInfo.packageName)
    }

    onPostPackageInfoAvailable()
  }

  protected open fun onPostPackageInfoAvailable() {}

  protected open fun onStaticLibsAvailable() {}

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      finish()
    }
    return super.onOptionsItemSelected(item)
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.app_detail_menu, menu)
    this.menu = menu

    val searchView = SearchView(this).apply {
      setIconifiedByDefault(false)
      setOnQueryTextListener(this@BaseAppDetailActivity)
      queryHint = getText(R.string.search_hint)
      isQueryRefinementEnabled = true

      findViewById<View>(androidx.appcompat.R.id.search_plate).apply {
        setBackgroundColor(Color.TRANSPARENT)
      }
    }

    menu.findItem(R.id.search).apply {
      setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW or MenuItem.SHOW_AS_ACTION_IF_ROOM)
      actionView = searchView

      // if (!isListReady) {
      //   isVisible = false
      // }
    }
    return super.onCreateOptionsMenu(menu)
  }

  override fun onQueryTextSubmit(query: String?): Boolean {
    return false
  }

  override fun onQueryTextChange(newText: String): Boolean {
    viewModel.queriedText = newText
    detailFragmentManager.deliverFilterItems(newText)
    return false
  }

  private fun showKotlinUsedLabel(kotlinPluginVersion: String?) {
    initFeatureListView()
    val title = StringBuilder(getString(R.string.kotlin_string))
    kotlinPluginVersion?.let {
      title.append(" ").append(it)
    }

    featureAdapter.addData(
      FeatureItem(R.drawable.ic_kotlin_logo) {
        BaseAlertDialogBuilder(this)
          .setIcon(R.drawable.ic_kotlin_logo)
          .setTitle(title)
          .setMessage(R.string.kotlin_details)
          .setPositiveButton(android.R.string.ok, null)
          .show()
      }
    )
  }

  private fun initFeatureListView(): Boolean {
    if (featureListView != null) {
      return false
    }

    featureListView = RecyclerView(this).also {
      it.layoutParams = ViewGroup.MarginLayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      it.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
      it.adapter = featureAdapter
      it.clipChildren = false
      it.overScrollMode = View.OVER_SCROLL_NEVER
    }
    binding.headerContentLayout.addView(featureListView)
    return true
  }

  private suspend fun initMoreFeatures(packageInfo: PackageInfo) {
    PackageUtils.getAGPVersion(packageInfo)?.let {
      withContext(Dispatchers.Main) {
        initFeatureListView()
        featureAdapter.addData(
          FeatureItem(R.drawable.ic_gradle) {
            BaseAlertDialogBuilder(this@BaseAppDetailActivity)
              .setIcon(R.drawable.ic_gradle)
              .setTitle(
                HtmlCompat.fromHtml(
                  "${getString(R.string.agp)} <b>$it</b>",
                  HtmlCompat.FROM_HTML_MODE_COMPACT
                )
              )
              .setMessage(R.string.agp_details)
              .setPositiveButton(android.R.string.ok, null)
              .show()
          }
        )
      }
    }

    if (packageInfo.isXposedModule()) {
      withContext(Dispatchers.Main) {
        initFeatureListView()
        featureAdapter.addData(
          FeatureItem(R.drawable.ic_xposed) {
            BaseAlertDialogBuilder(this@BaseAppDetailActivity)
              .setIcon(R.drawable.ic_xposed)
              .setTitle(R.string.xposed_module)
              .setMessage(R.string.xposed_module_details)
              .setPositiveButton(android.R.string.ok, null)
              .show()
          }
        )
      }
    }

    if (packageInfo.isPlayAppSigning()) {
      withContext(Dispatchers.Main) {
        initFeatureListView()
        featureAdapter.addData(
          FeatureItem(R.drawable.ic_lib_play_store) {
            BaseAlertDialogBuilder(this@BaseAppDetailActivity)
              .setIcon(R.drawable.ic_lib_play_store)
              .setTitle(R.string.play_app_signing)
              .setMessage(R.string.play_app_signing_details)
              .setPositiveButton(android.R.string.ok, null)
              .show()
          }
        )
      }
    }

    if (packageInfo.isPWA()) {
      withContext(Dispatchers.Main) {
        initFeatureListView()
        featureAdapter.addData(
          FeatureItem(R.drawable.ic_pwa) {
            BaseAlertDialogBuilder(this@BaseAppDetailActivity)
              .setIcon(R.drawable.ic_pwa)
              .setTitle(R.string.pwa)
              .setMessage(R.string.pwa_details)
              .setPositiveButton(android.R.string.ok, null)
              .show()
          }
        )
      }
    }

    if (packageInfo.isUseJetpackCompose()) {
      withContext(Dispatchers.Main) {
        initFeatureListView()
        val title = PackageUtils.getJetpackComposeVersion(packageInfo)?.let {
          HtmlCompat.fromHtml(
            "${getString(R.string.jetpack_compose)} <b>$it</b>",
            HtmlCompat.FROM_HTML_MODE_COMPACT
          )
        } ?: getString(R.string.jetpack_compose)
        featureAdapter.addData(
          FeatureItem(com.absinthe.lc.rulesbundle.R.drawable.ic_lib_jetpack_compose) {
            BaseAlertDialogBuilder(this@BaseAppDetailActivity)
              .setIcon(com.absinthe.lc.rulesbundle.R.drawable.ic_lib_jetpack_compose)
              .setTitle(title)
              .setMessage(R.string.jetpack_compose_details)
              .setPositiveButton(android.R.string.ok, null)
              .show()
          }
        )
      }
    }
  }

  private val toolbarQuicklyLaunchItem by unsafeLazy {
    AppDetailToolbarItem(R.drawable.ic_launch, R.string.further_operation) {
      if (viewModel.isPackageInfoAvailable()) {
        AppInfoBottomSheetDialogFragment().apply {
          arguments = bundleOf(
            EXTRA_PACKAGE_NAME to viewModel.packageInfo.packageName
          )
          show(supportFragmentManager, tag)
        }
      }
    }
  }
  private val toolbarProcessItem by unsafeLazy {
    AppDetailToolbarItem(R.drawable.ic_processes, R.string.menu_process) {
      detailFragmentManager.deliverSwitchProcessMode()
      viewModel.processMode = !viewModel.processMode
      GlobalValues.processMode = viewModel.processMode

      if (processBarView == null) {
        processBarView = ProcessBarView(this@BaseAppDetailActivity).also {
          it.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
          )
          it.setData(
            viewModel.processesMap.map { mapItem ->
              ProcessBarAdapter.ProcessBarItem(
                mapItem.key,
                mapItem.value
              )
            }
          )
          it.setOnItemClickListener { isSelected, process ->
            if (isSelected) {
              viewModel.queriedProcess = process
            } else {
              viewModel.queriedProcess = null
            }
            detailFragmentManager.deliverFilterProcesses(viewModel.queriedProcess)
          }
        }
        binding.detailToolbarContainer.addView(processBarView)
      } else {
        binding.detailToolbarContainer.removeView(processBarView)
        processBarView = null

        doOnMainThreadIdle {
          viewModel.queriedProcess = null
          detailFragmentManager.deliverFilterProcesses(null)
        }
      }
    }
  }
}
