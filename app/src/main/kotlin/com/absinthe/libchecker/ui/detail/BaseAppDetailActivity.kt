package com.absinthe.libchecker.ui.detail

import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.format.Formatter
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.core.text.buildSpannedString
import androidx.core.text.scale
import androidx.core.view.MenuProvider
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.DEX
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.annotation.SIGNATURES
import com.absinthe.libchecker.annotation.STATIC
import com.absinthe.libchecker.constant.AbilityType
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.database.entity.Features
import com.absinthe.libchecker.databinding.ActivityAppDetailBinding
import com.absinthe.libchecker.model.AppDetailToolbarItem
import com.absinthe.libchecker.model.DetailExtraBean
import com.absinthe.libchecker.model.FeatureItem
import com.absinthe.libchecker.model.SnapshotDiffItem
import com.absinthe.libchecker.recyclerview.adapter.detail.AppDetailToolbarAdapter
import com.absinthe.libchecker.recyclerview.adapter.detail.FeatureAdapter
import com.absinthe.libchecker.recyclerview.adapter.detail.ProcessBarAdapter
import com.absinthe.libchecker.recyclerview.adapter.detail.node.AbiLabelNode
import com.absinthe.libchecker.ui.app.CheckPackageOnResumingActivity
import com.absinthe.libchecker.ui.fragment.detail.AppInfoBottomSheetDialogFragment
import com.absinthe.libchecker.ui.fragment.detail.DetailFragmentManager
import com.absinthe.libchecker.ui.fragment.detail.MODE_SORT_BY_LIB
import com.absinthe.libchecker.ui.fragment.detail.MODE_SORT_BY_SIZE
import com.absinthe.libchecker.ui.fragment.detail.XposedInfoDialogFragment
import com.absinthe.libchecker.ui.fragment.detail.impl.AbilityAnalysisFragment
import com.absinthe.libchecker.ui.fragment.detail.impl.ComponentsAnalysisFragment
import com.absinthe.libchecker.ui.fragment.detail.impl.DexAnalysisFragment
import com.absinthe.libchecker.ui.fragment.detail.impl.MetaDataAnalysisFragment
import com.absinthe.libchecker.ui.fragment.detail.impl.NativeAnalysisFragment
import com.absinthe.libchecker.ui.fragment.detail.impl.PermissionAnalysisFragment
import com.absinthe.libchecker.ui.fragment.detail.impl.SignaturesAnalysisFragment
import com.absinthe.libchecker.ui.fragment.detail.impl.StaticAnalysisFragment
import com.absinthe.libchecker.utils.FileUtils
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.utils.extensions.addBackStateHandler
import com.absinthe.libchecker.utils.extensions.copyToClipboard
import com.absinthe.libchecker.utils.extensions.doOnMainThreadIdle
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getAppName
import com.absinthe.libchecker.utils.extensions.getCompileSdkVersion
import com.absinthe.libchecker.utils.extensions.getPackageSize
import com.absinthe.libchecker.utils.extensions.getPermissionsList
import com.absinthe.libchecker.utils.extensions.getTargetApiString
import com.absinthe.libchecker.utils.extensions.getVersionCode
import com.absinthe.libchecker.utils.extensions.getVersionString
import com.absinthe.libchecker.utils.extensions.setLongClickCopiedToClipboard
import com.absinthe.libchecker.utils.extensions.unsafeLazy
import com.absinthe.libchecker.utils.harmony.ApplicationDelegate
import com.absinthe.libchecker.utils.toJson
import com.absinthe.libchecker.view.detail.AppBarStateChangeListener
import com.absinthe.libchecker.view.detail.ProcessBarView
import com.absinthe.libchecker.viewmodel.DetailViewModel
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.zhanghai.android.appiconloader.AppIconLoader
import ohos.bundle.IBundleManager
import timber.log.Timber

abstract class BaseAppDetailActivity :
  CheckPackageOnResumingActivity<ActivityAppDetailBinding>(),
  IDetailContainer,
  SearchView.OnQueryTextListener,
  MenuProvider {

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

  override fun onCreate(savedInstanceState: Bundle?) {
    binding = ActivityAppDetailBinding.inflate(layoutInflater)
    super.onCreate(savedInstanceState)
    addMenuProvider(this, this, Lifecycle.State.STARTED)
    setSupportActionBar(getToolbar())
    supportActionBar?.apply {
      setDisplayHomeAsUpEnabled(true)
      setDisplayShowHomeEnabled(true)
    }
    onBackPressedDispatcher.addBackStateHandler(
      lifecycleOwner = this,
      enabledState = { binding.toolbar.hasExpandedActionView() },
      handler = { binding.toolbar.collapseActionView() }
    )
  }

  protected fun onPackageInfoAvailable(packageInfo: PackageInfo, extraBean: DetailExtraBean?) {
    viewModel.packageInfo = packageInfo
    viewModel.packageInfoLiveData.postValue(packageInfo)
    binding.apply {
      try {
        supportActionBar?.title = null
        collapsingToolbar.also {
          it.setOnApplyWindowInsetsListener(null)
          it.title = packageInfo.getAppName() ?: getString(R.string.detail_label)
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
                  show(supportFragmentManager, AppInfoBottomSheetDialogFragment::class.java.name)
                }
              }
            }
            setOnLongClickListener {
              copyToClipboard()
              true
            }
          }
          appNameView.apply {
            text = packageInfo.getAppName()
            setLongClickCopiedToClipboard(text)
          }
          packageNameView.apply {
            text = packageInfo.packageName
            setLongClickCopiedToClipboard(text)
          }
          versionInfoView.apply {
            text = packageInfo.getVersionString()
            setLongClickCopiedToClipboard(text)
          }
        }

        val extraInfo = SpannableStringBuilder()
        val versionInfo = buildSpannedString {
          if (!isHarmonyMode) {
            scale(0.8f) {
              append("Target: ")
            }
            append(packageInfo.getTargetApiString())
            scale(0.8f) {
              append(" Min: ")
            }
            append(packageInfo.applicationInfo.minSdkVersion.toString())
            scale(0.8f) {
              append(" Compile: ")
            }
            append(packageInfo.getCompileSdkVersion())
            scale(0.8f) {
              append(" Size: ")
            }
            val apkSize = FileUtils.getFileSize(packageInfo.applicationInfo.sourceDir)
            append(Formatter.formatFileSize(this@BaseAppDetailActivity, apkSize))

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
                scale(0.8f) {
                  append("Target: ")
                }
                append(hapBundle.targetVersion.toString())
                scale(0.8f) {
                  append("Min: ")
                }
                append(hapBundle.minSdkVersion.toString())

                if (!hapBundle.jointUserId.isNullOrEmpty()) {
                  appendLine().append("jointUserId = ${hapBundle.jointUserId}")
                }
              }
            }
          }
        }
        extraInfo.append(versionInfo)

        detailsTitle.extraInfoView.apply {
          text = extraInfo
          setLongClickCopiedToClipboard(text)
        }
        viewModel.initAbiInfo(packageInfo, apkAnalyticsMode)
      } catch (e: Exception) {
        Timber.e(e)
        Toasty.showLong(this@BaseAppDetailActivity, e.toString())
        finish()
        return
      }

      if (hasReloadVariant) {
        hasReloadVariant = false
        return@apply
      }

      toolbarAdapter.data.clear()
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
        initProcessBarView()
      }
      if (this@BaseAppDetailActivity is ApkDetailActivity && PackageUtils.isAppInstalled(packageInfo.packageName)) {
        toolbarAdapter.addData(
          AppDetailToolbarItem(R.drawable.ic_compare, R.string.compare_with_current) {
            runCatching {
              val basePackage = PackageUtils.getPackageInfo(
                viewModel.packageInfo.packageName,
                PackageManager.GET_ACTIVITIES
                  or PackageManager.GET_RECEIVERS
                  or PackageManager.GET_SERVICES
                  or PackageManager.GET_PROVIDERS
                  or PackageManager.GET_META_DATA
                  or PackageManager.GET_PERMISSIONS
              )
              navigateToSnapshotDetailPage(basePackage, viewModel.packageInfo)
            }.onFailure {
              Toasty.showLong(this@BaseAppDetailActivity, it.toString())
              Timber.e(it)
            }
          }
        )
      }

      rvToolbar.apply {
        adapter = toolbarAdapter
        layoutManager =
          LinearLayoutManager(this@BaseAppDetailActivity, RecyclerView.HORIZONTAL, false)
      }

      headerLayout.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
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
    }

    typeList = if (!isHarmonyMode) {
      mutableListOf(
        NATIVE,
        SERVICE,
        ACTIVITY,
        RECEIVER,
        PROVIDER,
        PERMISSION,
        METADATA,
        DEX,
        SIGNATURES
      )
    } else {
      mutableListOf(
        NATIVE,
        AbilityType.PAGE,
        AbilityType.SERVICE,
        AbilityType.WEB,
        AbilityType.DATA,
        DEX,
        SIGNATURES
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
        getText(R.string.ref_category_dex),
        getText(R.string.ref_category_signatures)
      )
    } else {
      mutableListOf(
        getText(R.string.ref_category_native),
        getText(R.string.ability_page),
        getText(R.string.ability_service),
        getText(R.string.ability_web),
        getText(R.string.ability_data),
        getText(R.string.ref_category_dex),
        getText(R.string.ref_category_signatures)
      )
    }

    if (packageInfo.applicationInfo.sharedLibraryFiles?.isNotEmpty() == true) {
      lifecycleScope.launch(Dispatchers.IO) {
        try {
          val libs = runCatching {
            PackageUtils.getStaticLibs(PackageUtils.getPackageInfo(packageInfo.packageName))
          }.getOrDefault(emptyList())
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
            SIGNATURES -> SignaturesAnalysisFragment.newInstance(packageInfo.packageName)
            else -> if (!isHarmonyMode) {
              ComponentsAnalysisFragment.newInstance(type)
            } else {
              AbilityAnalysisFragment.newInstance(type)
            }
          }
        }
      }
      registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
          super.onPageSelected(position)
          if (typeList[position] == NATIVE) {
            toolbarProcessItem.tooltipTextRes = R.string.menu_split
          } else {
            toolbarProcessItem.tooltipTextRes = R.string.menu_process
          }
        }
      })
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
          detailFragmentManager.selectedPosition = typeList[tab.position]
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

    viewModel.also {
      it.itemsCountLiveData.observe(this) { live ->
        if (detailFragmentManager.currentItemsCount != live.count && typeList[binding.tabLayout.selectedTabPosition] == live.locate) {
          binding.tsComponentCount.setText(live.count.toString())
          detailFragmentManager.currentItemsCount = live.count
        }
      }
      it.processToolIconVisibilityLiveData.observe(this) { visible ->
        if (visible) {
          if (detailFragmentManager.currentFragment?.isComponentFragment() == true) {
            if (!toolbarAdapter.data.contains(toolbarProcessItem)) {
              toolbarAdapter.addData(toolbarProcessItem)
            }
          }
          if (detailFragmentManager.currentFragment?.isNativeSourceAvailable() == true) {
            if (!toolbarAdapter.data.contains(toolbarProcessItem)) {
              toolbarAdapter.addData(toolbarProcessItem)
            }
          }
          if (GlobalValues.processMode || detailFragmentManager.currentFragment is PermissionAnalysisFragment) {
            if (processBarView == null) {
              initProcessBarView()
            }
            processBarView?.isVisible = true
          } else {
            processBarView?.isGone = true
          }
        } else {
          if (toolbarAdapter.data.contains(toolbarProcessItem)) {
            toolbarAdapter.remove(toolbarProcessItem)
          }
          if (detailFragmentManager.currentFragment !is PermissionAnalysisFragment) {
            processBarView?.isGone = true
          }
        }
      }
      it.processMapLiveData.observe(this) { map ->
        if (processBarView == null) {
          initProcessBarView()
        }
        processBarView?.setData(
          map.map { mapItem ->
            ProcessBarAdapter.ProcessBarItem(
              mapItem.key,
              mapItem.value
            )
          }
        )
        showProcessBarView()
      }
      it.featuresFlow.onEach { feat ->
        initFeatureListView()

        when (feat.featureType) {
          Features.SPLIT_APKS -> {
            featureAdapter.addData(
              FeatureItem(R.drawable.ic_aab) {
                FeaturesDialog.showSplitApksDialog(this, packageInfo.packageName)
              }
            )
          }

          Features.KOTLIN_USED -> {
            featureAdapter.addData(
              FeatureItem(R.drawable.ic_kotlin_logo) {
                FeaturesDialog.showKotlinDialog(this, feat.version)
              }
            )
          }

          Features.RX_JAVA -> {
            featureAdapter.addData(
              FeatureItem(R.drawable.ic_reactivex) {
                FeaturesDialog.showRxJavaDialog(this, feat.version)
              }
            )
          }

          Features.RX_KOTLIN -> {
            featureAdapter.addData(
              FeatureItem(R.drawable.ic_rxkotlin) {
                FeaturesDialog.showRxKotlinDialog(this, feat.version)
              }
            )
          }

          Features.RX_ANDROID -> {
            featureAdapter.addData(
              FeatureItem(R.drawable.ic_rxandroid) {
                FeaturesDialog.showRxAndroidDialog(this, feat.version)
              }
            )
          }

          Features.AGP -> {
            featureAdapter.addData(
              FeatureItem(R.drawable.ic_gradle) {
                FeaturesDialog.showAGPDialog(this, feat.version)
              }
            )
          }

          Features.XPOSED_MODULE -> {
            featureAdapter.addData(
              FeatureItem(R.drawable.ic_xposed) {
                XposedInfoDialogFragment.newInstance(packageInfo.packageName)
                  .show(supportFragmentManager, XposedInfoDialogFragment::class.java.name)
              }
            )
          }

          Features.PLAY_SIGNING -> {
            featureAdapter.addData(
              FeatureItem(com.absinthe.lc.rulesbundle.R.drawable.ic_lib_play_store) {
                FeaturesDialog.showPlayAppSigningDialog(this)
              }
            )
          }

          Features.PWA -> {
            featureAdapter.addData(
              FeatureItem(R.drawable.ic_pwa) {
                FeaturesDialog.showPWADialog(this)
              }
            )
          }

          Features.JETPACK_COMPOSE -> {
            featureAdapter.addData(
              FeatureItem(com.absinthe.lc.rulesbundle.R.drawable.ic_lib_jetpack_compose) {
                FeaturesDialog.showJetpackComposeDialog(this, feat.version)
              }
            )
          }

          Features.Ext.APPLICATION_PROP -> {
            featureAdapter.addData(
              FeatureItem(R.drawable.ic_app_prop) {
                FeaturesDialog.showAppPropDialog(this, packageInfo)
              }
            )
          }

          Features.Ext.APPLICATION_INSTALL_SOURCE -> {
            if (OsUtils.atLeastR() && !apkAnalyticsMode) {
              featureAdapter.addData(
                FeatureItem(R.drawable.ic_install_source) {
                  FeaturesDialog.showAppInstallSourceDialog(this, packageInfo.packageName)
                }
              )
            }
          }
        }
      }.launchIn(lifecycleScope)
      it.abiBundle.observe(this) { bundle ->
        if (bundle != null) {
          initAbiView(bundle.abi, bundle.abiSet)
        }
      }
    }

    if (featureListView == null) {
      viewModel.initFeatures(packageInfo, extraBean?.features ?: -1)
    }

    if (!isHarmonyMode) {
      viewModel.initComponentsData()
    } else {
      viewModel.initAbilities(this, packageInfo.packageName)
    }

    // To ensure onPostPackageInfoAvailable() is executed at the end of ui thread
    lifecycleScope.launch(Dispatchers.IO) {
      withContext(Dispatchers.Main) {
        delay(1L)
        onPostPackageInfoAvailable()
      }
    }
  }

  protected open fun onPostPackageInfoAvailable() {}

  protected open fun onStaticLibsAvailable() {}

  override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
    if (menuItem.itemId == android.R.id.home) {
      finish()
    }
    return true
  }

  override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
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
    }
  }

  override fun onQueryTextSubmit(query: String?): Boolean {
    return false
  }

  override fun onQueryTextChange(newText: String): Boolean {
    viewModel.queriedText = newText
    detailFragmentManager.deliverFilterItemsByText(newText)
    return false
  }

  private fun initFeatureListView(): Boolean {
    if (featureListView != null) {
      return false
    }

    featureListView = RecyclerView(this).also {
      it.layoutParams = ViewGroup.MarginLayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      ).also { lp ->
        lp.topMargin = 4.dp
      }
      it.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
      it.adapter = featureAdapter
      it.clipChildren = false
      it.overScrollMode = View.OVER_SCROLL_NEVER
    }

    doOnMainThreadIdle {
      val oldContainerHeight = binding.headerContentLayout.height
      val newContainerHeight = oldContainerHeight + 40.dp
      val params = binding.headerContentLayout.layoutParams

      binding.headerContentLayout.addView(featureListView)
      ValueAnimator.ofInt(oldContainerHeight, newContainerHeight).also {
        it.addUpdateListener { valueAnimator ->
          val height = valueAnimator.animatedValue as Int

          if (valueAnimator.animatedFraction == 1f) {
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT
          } else {
            params.height = height
          }
          binding.headerContentLayout.layoutParams = params
        }
        it.duration = 250
        it.start()
      }
    }

    return true
  }

  private val toolbarQuicklyLaunchItem by unsafeLazy {
    AppDetailToolbarItem(R.drawable.ic_launch, R.string.further_operation) {
      if (viewModel.isPackageInfoAvailable()) {
        AppInfoBottomSheetDialogFragment().apply {
          arguments = bundleOf(
            EXTRA_PACKAGE_NAME to viewModel.packageInfo.packageName
          )
          show(supportFragmentManager, AppInfoBottomSheetDialogFragment::class.java.name)
        }
      }
    }
  }
  private val toolbarProcessItem by unsafeLazy {
    AppDetailToolbarItem(R.drawable.ic_processes, R.string.menu_process) {
      detailFragmentManager.deliverSwitchProcessMode()
      viewModel.processMode = !viewModel.processMode

      if (viewModel.processMode) {
        if (processBarView == null) {
          initProcessBarView()
        }
        viewModel.processMapLiveData.value?.let {
          processBarView?.setData(
            it.map { mapItem ->
              ProcessBarAdapter.ProcessBarItem(
                mapItem.key,
                mapItem.value
              )
            }
          )
          processBarView?.isVisible = true
        }
      } else {
        binding.detailToolbarContainer.removeView(processBarView)
        processBarView = null

        doOnMainThreadIdle {
          viewModel.queriedProcess = null
          detailFragmentManager.deliverFilterItems(null)
        }
      }
      GlobalValues.processMode = viewModel.processMode
    }
  }

  private fun navigateToSnapshotDetailPage(basePackage: PackageInfo, analysisPackage: PackageInfo) {
    val diff = SnapshotDiffItem(
      packageName = basePackage.packageName,
      updateTime = basePackage.lastUpdateTime,
      labelDiff = SnapshotDiffItem.DiffNode(
        basePackage.getAppName() ?: "null",
        analysisPackage.getAppName() ?: "null"
      ),
      versionNameDiff = SnapshotDiffItem.DiffNode(
        basePackage.versionName,
        analysisPackage.versionName
      ),
      versionCodeDiff = SnapshotDiffItem.DiffNode(
        basePackage.getVersionCode(),
        analysisPackage.getVersionCode()
      ),
      abiDiff = SnapshotDiffItem.DiffNode(
        PackageUtils.getAbi(basePackage).toShort(),
        PackageUtils.getAbi(analysisPackage).toShort()
      ),
      targetApiDiff = SnapshotDiffItem.DiffNode(
        basePackage.applicationInfo.targetSdkVersion.toShort(),
        analysisPackage.applicationInfo.targetSdkVersion.toShort()
      ),
      nativeLibsDiff = SnapshotDiffItem.DiffNode(
        PackageUtils.getNativeDirLibs(basePackage).toJson().orEmpty(),
        PackageUtils.getNativeDirLibs(analysisPackage).toJson().orEmpty()
      ),
      servicesDiff = SnapshotDiffItem.DiffNode(
        PackageUtils.getComponentStringList(
          basePackage,
          SERVICE,
          false
        ).toJson().orEmpty(),
        PackageUtils.getComponentStringList(
          analysisPackage,
          SERVICE,
          false
        ).toJson().orEmpty()
      ),
      activitiesDiff = SnapshotDiffItem.DiffNode(
        PackageUtils.getComponentStringList(
          basePackage,
          ACTIVITY,
          false
        ).toJson().orEmpty(),
        PackageUtils.getComponentStringList(
          analysisPackage,
          ACTIVITY,
          false
        ).toJson().orEmpty()
      ),
      receiversDiff = SnapshotDiffItem.DiffNode(
        PackageUtils.getComponentStringList(
          basePackage,
          RECEIVER,
          false
        ).toJson().orEmpty(),
        PackageUtils.getComponentStringList(
          analysisPackage,
          RECEIVER,
          false
        ).toJson().orEmpty()
      ),
      providersDiff = SnapshotDiffItem.DiffNode(
        PackageUtils.getComponentStringList(
          basePackage,
          PROVIDER,
          false
        ).toJson().orEmpty(),
        PackageUtils.getComponentStringList(
          analysisPackage,
          PROVIDER,
          false
        ).toJson().orEmpty()
      ),
      permissionsDiff = SnapshotDiffItem.DiffNode(
        basePackage.getPermissionsList().toJson().orEmpty(),
        analysisPackage.getPermissionsList().toJson().orEmpty()
      ),
      metadataDiff = SnapshotDiffItem.DiffNode(
        PackageUtils.getMetaDataItems(basePackage).toJson().orEmpty(),
        PackageUtils.getMetaDataItems(analysisPackage).toJson().orEmpty()
      ),
      packageSizeDiff = SnapshotDiffItem.DiffNode(
        basePackage.getPackageSize(true),
        analysisPackage.getPackageSize(true)
      )
    )

    val intent = Intent(this, SnapshotDetailActivity::class.java)
      .putExtras(bundleOf(EXTRA_ENTITY to diff))
    startActivity(intent)
  }

  private fun initProcessBarView() {
    processBarView = ProcessBarView(this@BaseAppDetailActivity).also {
      it.layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      it.setOnItemClickListener { isSelected, process ->
        if (isSelected) {
          viewModel.queriedProcess = process
        } else {
          viewModel.queriedProcess = null
        }
        detailFragmentManager.deliverFilterItems(viewModel.queriedProcess)
      }
    }
    binding.detailToolbarContainer.addView(processBarView)
    showProcessBarView()
  }

  private fun showProcessBarView() {
    if (viewModel.processToolIconVisibilityLiveData.value == false && detailFragmentManager.currentFragment !is PermissionAnalysisFragment) {
      processBarView?.isGone = true
    } else {
      processBarView?.isVisible = true
    }
  }

  private fun initAbiView(abi: Int, abiSet: Set<Int>) {
    val trueAbi = abi.mod(Constants.MULTI_ARCH)
    viewModel.is64Bit.postValue(trueAbi == Constants.ARMV8 || trueAbi == Constants.X86_64)

    if (abiSet.isNotEmpty() && !abiSet.contains(Constants.OVERLAY) && !abiSet.contains(Constants.ERROR)) {
      val abiLabelsList = mutableListOf<AbiLabelNode>()

      if (abi >= Constants.MULTI_ARCH) {
        abiLabelsList.add(AbiLabelNode(Constants.MULTI_ARCH, true))
      }

      abiSet.forEach {
        if (it != Constants.NO_LIBS) {
          val isActive = apkAnalyticsMode || it == abi % Constants.MULTI_ARCH
          abiLabelsList.add(AbiLabelNode(it, isActive))
        }
      }
      doOnMainThreadIdle {
        binding.detailsTitle.abiLabelsAdapter.setList(abiLabelsList)
      }
    }
  }
}
