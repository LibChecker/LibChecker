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
import androidx.annotation.StringRes
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
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
import com.absinthe.libchecker.features.applist.detail.bean.DetailExtraBean
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
  protected val typeList: List<Int>
    get() = tabController.types

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
  private val tabController by unsafeLazy {
    DetailTabController(
      activity = this,
      viewPager = binding.viewpager,
      tabLayout = binding.tabLayout,
      onTabSelected = ::onDetailTabSelected,
      onProcessTooltipTextChanged = ::updateProcessToolbarTooltip
    )
  }
  private val toolbarController by unsafeLazy {
    DetailToolbarController(
      toolbarView = binding.rvToolbar,
      appBarLayout = binding.headerLayout,
      onSortClick = ::toggleSortMode,
      onQuickLaunchClick = ::showCurrentAppInfoDialog,
      onProcessClick = ::toggleProcessMode
    )
  }

  private var isHarmonyMode = false
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
    tabController.reset()
    toolbarController.release()
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

      toolbarController.setupBaseActions(
        showHarmonyToggle = extraBean?.variant == Constants.VARIANT_HAP,
        onHarmonyToggle = {
          isHarmonyMode = !isHarmonyMode
          onPackageInfoAvailable(packageInfo, extraBean)
        }
      )
      if (apkAnalyticsMode && !viewModel.isApkPreview) {
        lifecycleScope.launch {
          if (!viewModel.isInstalledAppComparisonAvailable(packageName)) {
            return@launch
          }
          if (uiGeneration != packageUiGeneration) {
            return@launch
          }
          toolbarController.addCompareAction {
            lifecycleScope.launch {
              val basePackage = viewModel.loadInstalledAppComparisonPackage(packageName)
              if (basePackage == null) {
                Toasty.showLong(this@BaseAppDetailActivity, getString(R.string.toast_cant_open_app))
                return@launch
              }
              navigateToSnapshotDetailPage(basePackage, viewModel.packageInfo)
            }
          }
        }
      }
    }

    val tabSpec = tabSpecBuilder.build(
      isHarmonyMode = isHarmonyMode,
      isApkPreview = viewModel.isApkPreview
    )
    tabController.setup(
      packageName = packageName,
      isHarmonyMode = isHarmonyMode,
      tabSpec = tabSpec
    )

    if (sharedLibraryFiles?.isNotEmpty() == true) {
      lifecycleScope.launch {
        if (viewModel.hasInstalledStaticLibraries(packageName)) {
          if (uiGeneration != packageUiGeneration) {
            return@launch
          }
          if (tabController.insertStaticLibraryTab(getText(R.string.ref_category_static))) {
            onStaticLibsAvailable()
          }
        }
      }
    }

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
        if (detailFragmentManager.currentItemsCount != live.count && tabController.selectedType == live.locate) {
          updateCurrentItemsCount(live.count)
        }
      }.launchIn(lifecycleScope)
      it.filterState.processToolIconVisibilityStateFlow.onEach { visible ->
        toolbarController.setProcessActionVisible(visible)
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
    tabController.reset()
    featureListController.reset()
    toolbarController.reset()
  }

  private fun onDetailTabSelected(type: Int) {
    val count = viewModel.filterState.itemsCountList[type]
    if (detailFragmentManager.currentItemsCount != count) {
      updateCurrentItemsCount(count)
    }
    detailFragmentManager.selectedPosition = type
  }

  private fun updateCurrentItemsCount(count: Int) {
    binding.tsComponentCount.setText(count.toString())
    detailFragmentManager.currentItemsCount = count
  }

  private fun updateProcessToolbarTooltip(@StringRes tooltipTextRes: Int) {
    toolbarController.updateProcessTooltip(tooltipTextRes)
  }

  private fun showCurrentAppInfoDialog() {
    if (viewModel.isPackageInfoAvailable()) {
      showAppInfoDialog(viewModel.packageInfo.packageName)
    }
  }

  private fun toggleProcessMode() {
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

  private fun toggleSortMode() {
    val sortMode = if (appDetailSettingsRepository.sortMode == MODE_SORT_BY_LIB) {
      MODE_SORT_BY_SIZE
    } else {
      MODE_SORT_BY_LIB
    }
    appDetailSettingsRepository.setSortMode(sortMode)
    detailFragmentManager.sortAll(lifecycleScope)
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
