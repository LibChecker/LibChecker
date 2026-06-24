package com.absinthe.libchecker.features.applist.detail.ui

import android.content.Intent
import android.content.pm.PackageInfo
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.SIGNATURES
import com.absinthe.libchecker.annotation.STATIC
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.options.AdvancedOptions
import com.absinthe.libchecker.database.entity.Features
import com.absinthe.libchecker.databinding.ActivityAppDetailBinding
import com.absinthe.libchecker.domain.app.AppDetailSettingsRepository
import com.absinthe.libchecker.domain.app.AppListSettingsRepository
import com.absinthe.libchecker.domain.app.VersionedFeature
import com.absinthe.libchecker.features.applist.DetailFragmentManager
import com.absinthe.libchecker.features.applist.MODE_SORT_BY_LIB
import com.absinthe.libchecker.features.applist.MODE_SORT_BY_SIZE
import com.absinthe.libchecker.features.applist.detail.AppBarStateChangeListener
import com.absinthe.libchecker.features.applist.detail.DetailViewModel
import com.absinthe.libchecker.features.applist.detail.IDetailContainer
import com.absinthe.libchecker.features.applist.detail.bean.AppDetailToolbarItem
import com.absinthe.libchecker.features.applist.detail.bean.DetailExtraBean
import com.absinthe.libchecker.features.applist.detail.ui.adapter.AppDetailToolbarAdapter
import com.absinthe.libchecker.features.applist.detail.ui.impl.AbilityAnalysisFragment
import com.absinthe.libchecker.features.applist.detail.ui.impl.ComponentsAnalysisFragment
import com.absinthe.libchecker.features.applist.detail.ui.impl.MetaDataAnalysisFragment
import com.absinthe.libchecker.features.applist.detail.ui.impl.NativeAnalysisFragment
import com.absinthe.libchecker.features.applist.detail.ui.impl.PermissionAnalysisFragment
import com.absinthe.libchecker.features.applist.detail.ui.impl.SignaturesAnalysisFragment
import com.absinthe.libchecker.features.applist.detail.ui.impl.StaticAnalysisFragment
import com.absinthe.libchecker.features.snapshot.detail.ui.EXTRA_ENTITY
import com.absinthe.libchecker.features.snapshot.detail.ui.SnapshotDetailActivity
import com.absinthe.libchecker.ui.app.CheckPackageOnResumingActivity
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.extensions.addBackStateHandler
import com.absinthe.libchecker.utils.extensions.applySystemBarsPadding
import com.absinthe.libchecker.utils.extensions.doOnMainThreadIdle
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getVersionCode
import com.absinthe.libchecker.utils.extensions.isKeyboardShowing
import com.absinthe.libchecker.utils.extensions.unsafeLazy
import com.absinthe.libchecker.utils.harmony.ApplicationDelegate
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
import ohos.bundle.IBundleManager
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

abstract class BaseAppDetailActivity :
  CheckPackageOnResumingActivity<ActivityAppDetailBinding>(),
  IDetailContainer,
  SearchView.OnQueryTextListener,
  MenuProvider {

  protected val viewModel: DetailViewModel by viewModel()
  private val appDetailSettingsRepository: AppDetailSettingsRepository by inject()
  private val appListSettingsRepository: AppListSettingsRepository by inject()
  protected var typeList = mutableListOf<Int>()

  override var detailFragmentManager: DetailFragmentManager = DetailFragmentManager()

  abstract val apkAnalyticsMode: Boolean
  abstract fun getToolbar(): Toolbar

  override fun onApplyContentWindowInsets() {
    binding.root.applySystemBarsPadding(left = true, top = true, right = true)
  }

  private val bundleManager by unsafeLazy { ApplicationDelegate(this).iBundleManager }
  private val featureItemBuilder by unsafeLazy {
    DetailFeatureItemBuilder(
      activity = this,
      packageInfo = { viewModel.packageInfo },
      isApkPreview = { viewModel.isApkPreview },
      apkPreviewInfo = { viewModel.apkPreviewInfo },
      apkAnalyticsMode = { apkAnalyticsMode },
      appIcons = { viewModel.featureState.appIcons },
      appIconDrawables = ::prepareAppIconDrawables
    )
  }
  private val featureListController by unsafeLazy {
    DetailFeatureListController(binding.headerContentLayout)
  }
  private val processBarController by unsafeLazy {
    DetailProcessBarController(
      container = binding.detailToolbarContainer,
      processMode = { appDetailSettingsRepository.processMode },
      hasNonGrantedPermissions = { detailFragmentManager.currentFragment?.hasNonGrantedPermissions() },
      onProcessFilterChanged = { process ->
        viewModel.filterState.queriedProcess = process
        detailFragmentManager.deliverFilterItems(null, process, lifecycleScope)
      }
    )
  }
  private val abiLabelBinder by unsafeLazy {
    DetailAbiLabelBinder(
      activity = this,
      detailsTitleView = binding.detailsTitle,
      tintAbiLabels = { isDisplayOptionEnabled(AdvancedOptions.TINT_ABI_LABEL) }
    )
  }
  private val headerTitleBinder by unsafeLazy {
    DetailHeaderTitleBinder(
      detailsTitleView = binding.detailsTitle,
      onAppInfoClick = ::showAppInfoDialog
    )
  }
  private val headerExtraInfoBinder by unsafeLazy {
    DetailHeaderExtraInfoBinder(binding.detailsTitle)
  }
  private val tabSpecBuilder by unsafeLazy { DetailTabSpecBuilder(this) }
  private val toolbarAdapter by unsafeLazy { AppDetailToolbarAdapter() }

  private var isHarmonyMode = false
  private var isToolbarCollapsed = false
  private var tabLayoutMediator: TabLayoutMediator? = null
  private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null
  private var packageUiGeneration = 0

  override fun onCreate(savedInstanceState: Bundle?) {
    binding = ActivityAppDetailBinding.inflate(layoutInflater)
    super.onCreate(savedInstanceState)
    binding.viewpager.isSaveEnabled = false
    addMenuProvider(this, this, Lifecycle.State.CREATED)
    setSupportActionBar(getToolbar())
    binding.toolbar.isBackInvokedCallbackEnabled = false
    supportActionBar?.apply {
      setDisplayHomeAsUpEnabled(true)
      setDisplayShowHomeEnabled(true)
    }
    onBackPressedDispatcher.addBackStateHandler(
      lifecycleOwner = this,
      enabledState = { !isKeyboardShowing() && binding.toolbar.hasExpandedActionView() },
      handler = { binding.toolbar.collapseActionView() }
    )
    initObserver()
  }

  override fun onDestroy() {
    tabLayoutMediator?.detach()
    tabLayoutMediator = null
    pageChangeCallback?.let { binding.viewpager.unregisterOnPageChangeCallback(it) }
    pageChangeCallback = null
    super.onDestroy()
  }

  protected fun onPackageInfoAvailable(packageInfo: PackageInfo, extraBean: DetailExtraBean?) {
    val uiGeneration = ++packageUiGeneration
    resetUiState()
    viewModel.reset()
    val ai = packageInfo.applicationInfo
    val apkPreviewInfo = viewModel.apkPreviewInfo

    viewModel.initPackageInfo(packageInfo)
    if (apkPreviewInfo != null) {
      viewModel.initAbiInfo(apkPreviewInfo)
    } else {
      viewModel.initAbiInfo(packageInfo, apkAnalyticsMode)
    }

    val headerTitleData = viewModel.buildAppDetailHeaderTitleData(packageInfo, apkAnalyticsMode)
    val packageName = headerTitleData.packageName
    val sharedLibraryFiles = ai?.sharedLibraryFiles

    binding.apply {
      try {
        supportActionBar?.title = null
        collapsingToolbar.also {
          it.setOnApplyWindowInsetsListener(null)
          it.title = headerTitleData.title
        }
        headerLayout.addOnOffsetChangedListener(object : AppBarStateChangeListener() {
          override fun onStateChanged(appBarLayout: AppBarLayout, state: State) {
            collapsingToolbar.isTitleEnabled = state == State.COLLAPSED
          }
        })
        headerTitleBinder.bind(headerTitleData, ai)

        lifecycleScope.launch(Dispatchers.IO) {
          val showAndroidVersion = isDisplayOptionEnabled(AdvancedOptions.SHOW_ANDROID_VERSION)
          val versionInfo = if (!isHarmonyMode) {
            val headerExtraInfo = viewModel.buildAppDetailHeaderExtraInfo(
              packageInfo = packageInfo,
              showAndroidVersion = showAndroidVersion
            )
            headerExtraInfoBinder.format(headerExtraInfo)
          } else {
            if (extraBean?.variant == Constants.VARIANT_HAP) {
              headerExtraInfoBinder.formatHarmony(
                bundleManager?.getBundleInfo(
                  packageName,
                  IBundleManager.GET_BUNDLE_DEFAULT
                )
              )
            } else {
              ""
            }
          }

          withContext(Dispatchers.Main) {
            headerExtraInfoBinder.bind(versionInfo)
          }
        }
      } catch (e: Exception) {
        Timber.e(e)
        Toasty.showLong(this@BaseAppDetailActivity, e.toString())
        finish()
        return
      }

      toolbarAdapter.addData(
        AppDetailToolbarItem(R.drawable.ic_lib_sort, R.string.menu_sort) {
          val sortMode = if (appDetailSettingsRepository.sortMode == MODE_SORT_BY_LIB) {
            MODE_SORT_BY_SIZE
          } else {
            MODE_SORT_BY_LIB
          }
          appDetailSettingsRepository.setSortMode(sortMode)
          detailFragmentManager.sortAll(lifecycleScope)
        }
      )
      if (extraBean?.variant == Constants.VARIANT_HAP) {
        toolbarAdapter.addData(
          AppDetailToolbarItem(R.drawable.ic_harmonyos_logo, R.string.ability) {
            isHarmonyMode = !isHarmonyMode
            onPackageInfoAvailable(packageInfo, extraBean)
          }
        )
      }
      if (apkAnalyticsMode && !viewModel.isApkPreview) {
        lifecycleScope.launch {
          if (!viewModel.isInstalledAppComparisonAvailable(packageName)) {
            return@launch
          }
          if (uiGeneration != packageUiGeneration) {
            return@launch
          }
          toolbarAdapter.addData(
            AppDetailToolbarItem(R.drawable.ic_compare, R.string.compare_with_current) {
              lifecycleScope.launch {
                val basePackage = viewModel.loadInstalledAppComparisonPackage(packageName)
                if (basePackage == null) {
                  Toasty.showLong(this@BaseAppDetailActivity, getString(R.string.toast_cant_open_app))
                  return@launch
                }
                navigateToSnapshotDetailPage(basePackage, viewModel.packageInfo)
              }
            }
          )
        }
      }

      rvToolbar.apply {
        if (adapter != toolbarAdapter) {
          adapter = toolbarAdapter
          layoutManager =
            LinearLayoutManager(this@BaseAppDetailActivity, RecyclerView.HORIZONTAL, false)
          itemAnimator = null
        }
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

    val tabSpec = tabSpecBuilder.build(
      isHarmonyMode = isHarmonyMode,
      isApkPreview = viewModel.isApkPreview
    )
    typeList = tabSpec.types
    val tabTitles = tabSpec.titles

    if (sharedLibraryFiles?.isNotEmpty() == true) {
      lifecycleScope.launch {
        if (viewModel.hasInstalledStaticLibraries(packageName)) {
          if (uiGeneration != packageUiGeneration || STATIC in typeList) {
            return@launch
          }
          typeList.add(1, STATIC)
          tabTitles.add(1, getText(R.string.ref_category_static))
          binding.viewpager.adapter?.notifyItemInserted(1)
          onStaticLibsAvailable()
        }
      }
    }

    binding.viewpager.apply {
      adapter = object : FragmentStateAdapter(this@BaseAppDetailActivity) {
        override fun getItemCount(): Int {
          return typeList.size
        }

        override fun createFragment(position: Int): Fragment {
          return when (val type = typeList.getOrElse(position) { NATIVE }) {
            NATIVE -> NativeAnalysisFragment.newInstance(packageName)

            STATIC -> StaticAnalysisFragment.newInstance(packageName)

            PERMISSION -> PermissionAnalysisFragment.newInstance(packageName)

            METADATA -> MetaDataAnalysisFragment.newInstance(packageName)

            // DEX -> DexAnalysisFragment.newInstance(packageName)

            SIGNATURES -> SignaturesAnalysisFragment.newInstance(packageName)

            else -> if (!isHarmonyMode) {
              ComponentsAnalysisFragment.newInstance(type)
            } else {
              AbilityAnalysisFragment.newInstance(type)
            }
          }
        }

        override fun getItemId(position: Int): Long {
          return typeList.getOrElse(position) { NATIVE }.toLong()
        }

        override fun containsItem(itemId: Long): Boolean {
          return typeList.any { it.toLong() == itemId }
        }
      }
      pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
          super.onPageSelected(position)
          val tooltipTextRes = if (typeList.getOrNull(position) == NATIVE) {
            R.string.menu_split
          } else {
            R.string.menu_process
          }
          if (toolbarProcessItem.tooltipTextRes != tooltipTextRes) {
            toolbarProcessItem.tooltipTextRes = tooltipTextRes
            toolbarAdapter.data.indexOf(toolbarProcessItem).takeIf { it >= 0 }?.let {
              toolbarAdapter.notifyItemChanged(it)
            }
          }
        }
      }.also { registerOnPageChangeCallback(it) }
    }
    binding.tabLayout.apply {
      removeAllTabs()
      tabTitles.forEach {
        addTab(newTab().apply { text = it })
      }
      addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
        override fun onTabSelected(tab: TabLayout.Tab) {
          val type = typeList.getOrNull(tab.position) ?: return
          val count = viewModel.filterState.itemsCountList[type]
          if (detailFragmentManager.currentItemsCount != count) {
            binding.tsComponentCount.setText(count.toString())
            detailFragmentManager.currentItemsCount = count
          }
          detailFragmentManager.selectedPosition = type
        }

        override fun onTabUnselected(tab: TabLayout.Tab?) {}

        override fun onTabReselected(tab: TabLayout.Tab?) {}
      })
    }

    tabLayoutMediator =
      TabLayoutMediator(binding.tabLayout, binding.viewpager) { tab, position ->
        tab.text = tabTitles.getOrNull(position)
      }.also { it.attach() }

    if (!featureListController.isInitialized) {
      if (viewModel.isApkPreview) {
        viewModel.emitFeature(VersionedFeature(Features.Ext.APPLICATION_PROP))
      } else {
        viewModel.initFeatures(packageInfo, extraBean?.features ?: -1)
      }
    }

    if (!isHarmonyMode) {
      if (viewModel.isApkPreview) {
        viewModel.initComponentsDataInPreview()
      } else {
        viewModel.initComponentsData()
      }
    } else {
      viewModel.initAbilities(packageName)
    }

    // Detect Live Update notification
    viewModel.initPermissionData()

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

  private fun showAppInfoDialog(packageName: String) {
    AppInfoBottomSheetDialogFragment().apply {
      arguments = Bundle().apply {
        putString(EXTRA_PACKAGE_NAME, packageName)
      }
      show(supportFragmentManager, AppInfoBottomSheetDialogFragment::class.java.name)
    }
  }

  override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
    if (menuItem.itemId == android.R.id.home) {
      finish()
    }
    return true
  }

  override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
    menuInflater.inflate(R.menu.app_detail_menu, menu)

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
    viewModel.filterState.queriedText = newText
    detailFragmentManager.deliverFilterItemsByText(newText, lifecycleScope)
    return false
  }

  override fun collapseAppBar() {
    binding.headerLayout.setExpanded(false, true)
  }

  private fun initObserver() {
    viewModel.also {
      it.filterState.itemsCountStateFlow.onEach { live ->
        val position = binding.tabLayout.selectedTabPosition
        if (position >= 0 && detailFragmentManager.currentItemsCount != live.count && typeList[position] == live.locate) {
          binding.tsComponentCount.setText(live.count.toString())
          detailFragmentManager.currentItemsCount = live.count
        }
      }.launchIn(lifecycleScope)
      it.filterState.processToolIconVisibilityStateFlow.onEach { visible ->
        if (visible) {
          if (!toolbarAdapter.data.contains(toolbarProcessItem)) {
            toolbarAdapter.addData(toolbarProcessItem)
          }
        } else {
          if (toolbarAdapter.data.contains(toolbarProcessItem)) {
            toolbarAdapter.remove(toolbarProcessItem)
          }
        }
      }.launchIn(lifecycleScope)
      it.filterState.processMapStateFlow.onEach { map ->
        processBarController.setData(map)
      }.launchIn(lifecycleScope)
      it.featureState.featuresFlow.onEach { feat ->
        addFeatureItem(feat)
      }.launchIn(lifecycleScope)
      it.featureState.abiBundleStateFlow.onEach { bundle ->
        if (bundle != null) {
          initAbiView(bundle.abi, bundle.abiSet)

          doOnMainThreadIdle {
            featureListController.attachWithAnimation()
          }
        }
      }.launchIn(lifecycleScope)
    }
  }

  private fun addFeatureItem(feature: VersionedFeature) {
    val featureItem = featureItemBuilder.build(feature, featureListController.itemCount) ?: return
    featureListController.addItem(featureItem)
  }

  private fun resetUiState() {
    tabLayoutMediator?.detach()
    tabLayoutMediator = null
    pageChangeCallback?.let { binding.viewpager.unregisterOnPageChangeCallback(it) }
    pageChangeCallback = null
    binding.viewpager.adapter = null
    binding.tabLayout.clearOnTabSelectedListeners()
    featureListController.reset()
    toolbarAdapter.setList(emptyList())
  }

  private val toolbarQuicklyLaunchItem by unsafeLazy {
    AppDetailToolbarItem(R.drawable.ic_launch, R.string.further_operation) {
      if (viewModel.isPackageInfoAvailable()) {
        showAppInfoDialog(viewModel.packageInfo.packageName)
      }
    }
  }
  private val toolbarProcessItem by unsafeLazy {
    AppDetailToolbarItem(R.drawable.ic_processes, R.string.menu_process) {
      val processMode = !appDetailSettingsRepository.processMode
      appDetailSettingsRepository.setProcessMode(processMode)
      detailFragmentManager.deliverProcessMode(processMode)

      processBarController.refreshVisibility()
      if (!processMode) {
        doOnMainThreadIdle {
          viewModel.filterState.queriedProcess = null
          detailFragmentManager.deliverFilterItems(null, null, lifecycleScope)
        }
      }
    }
  }

  private fun navigateToSnapshotDetailPage(basePackage: PackageInfo, analysisPackage: PackageInfo) = lifecycleScope.launch(Dispatchers.Main) {
    val dialog = UiUtils.createLoadingDialog(this@BaseAppDetailActivity)
    dialog.show()
    val diff = viewModel.buildPackageComparisonSnapshotItem(basePackage, analysisPackage)
    dialog.dismiss()

    val intent = Intent(this@BaseAppDetailActivity, SnapshotDetailActivity::class.java)
      .putExtras(
        Bundle().apply {
          putSerializable(EXTRA_ENTITY, diff)
        }
      )
    startActivity(intent)
  }

  private fun initAbiView(abi: Int, abiSet: Collection<Int>) {
    val abiLabelData = viewModel.buildAppDetailAbiLabelData(
      abi = abi,
      abiSet = abiSet,
      apkAnalyticsMode = apkAnalyticsMode
    )
    lifecycleScope.launch {
      viewModel.featureState.set64Bit(abiLabelData.is64Bit)
    }

    abiLabelBinder.bind(abiLabelData)
  }

  private fun isDisplayOptionEnabled(option: Int): Boolean {
    return (appListSettingsRepository.displayOptions and option) > 0
  }

  @RequiresApi(Build.VERSION_CODES.TIRAMISU)
  private fun prepareAppIconDrawables(): List<Drawable> {
    val firstIcon = viewModel.featureState.appIcons[0]
    val drawables = viewModel.featureState.appIcons.map { it.drawable }.toMutableList()

    val processedDrawable = when {
      firstIcon.isMonochrome && firstIcon.drawable is AdaptiveIconDrawable -> {
        createMonochromeIconWithBackground(firstIcon.drawable)
      }

      else -> firstIcon.drawable
    }

    processedDrawable?.let {
      drawables[0] = it
    } ?: drawables.removeAt(0)

    return drawables
  }

  @RequiresApi(Build.VERSION_CODES.TIRAMISU)
  private fun createMonochromeIconWithBackground(adaptiveIcon: AdaptiveIconDrawable): Drawable? {
    return adaptiveIcon.monochrome?.apply {
      setTint(getColorByAttr(androidx.appcompat.R.attr.colorPrimary))
    }?.let { foreground ->
      UiUtils.addCircleBackground(
        this,
        foreground,
        getColorByAttr(com.google.android.material.R.attr.colorSecondaryContainer)
      )
    }
  }
}
