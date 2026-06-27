package com.absinthe.libchecker.features.applist.detail.ui

import android.content.pm.PackageInfo
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.options.AdvancedOptions
import com.absinthe.libchecker.databinding.ActivityAppDetailBinding
import com.absinthe.libchecker.domain.app.AppDetailSettingsRepository
import com.absinthe.libchecker.domain.app.AppListSettingsRepository
import com.absinthe.libchecker.domain.app.VersionedFeature
import com.absinthe.libchecker.domain.app.detail.content.BuildAppDetailContentInitPlanUseCase
import com.absinthe.libchecker.domain.app.detail.content.BuildAppDetailTabTypesUseCase
import com.absinthe.libchecker.domain.app.detail.model.DetailExtraBean
import com.absinthe.libchecker.domain.app.detail.ui.DetailFragmentManager
import com.absinthe.libchecker.features.applist.detail.DetailViewModel
import com.absinthe.libchecker.features.applist.detail.IDetailContainer
import com.absinthe.libchecker.ui.app.CheckPackageOnResumingActivity
import com.absinthe.libchecker.utils.extensions.addBackStateHandler
import com.absinthe.libchecker.utils.extensions.applySystemBarsPadding
import com.absinthe.libchecker.utils.extensions.doOnMainThreadIdle
import com.absinthe.libchecker.utils.extensions.getVersionCode
import com.absinthe.libchecker.utils.extensions.isKeyboardShowing
import com.absinthe.libchecker.utils.extensions.unsafeLazy
import com.absinthe.libchecker.utils.harmony.ApplicationDelegate
import kotlinx.coroutines.launch
import ohos.bundle.IBundleManager
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

abstract class BaseAppDetailActivity :
  CheckPackageOnResumingActivity<ActivityAppDetailBinding>(),
  IDetailContainer {

  protected val viewModel: DetailViewModel by viewModel()
  private val appDetailSettingsRepository: AppDetailSettingsRepository by inject()
  private val appListSettingsRepository: AppListSettingsRepository by inject()
  private val buildAppDetailContentInitPlan: BuildAppDetailContentInitPlanUseCase by inject()
  private val buildAppDetailTabTypes: BuildAppDetailTabTypesUseCase by inject()
  protected val typeList: List<Int>
    get() = tabController.types

  override var detailFragmentManager: DetailFragmentManager = DetailFragmentManager()

  abstract val apkAnalyticsMode: Boolean
  abstract fun getToolbar(): Toolbar

  override fun onApplyContentWindowInsets() {
    binding.root.applySystemBarsPadding(left = true, top = true, right = true)
  }

  private val bundleManager by unsafeLazy { ApplicationDelegate(this).iBundleManager }
  private val appIconDrawableBuilder by unsafeLazy {
    DetailAppIconDrawableBuilder(this)
  }
  private val featureItemBuilder by unsafeLazy {
    DetailFeatureItemBuilder(
      activity = this,
      viewModel = viewModel,
      packageInfo = { viewModel.packageInfo },
      apkPreviewInfo = { viewModel.apkPreviewInfo },
      apkAnalyticsMode = { apkAnalyticsMode },
      appIcons = { viewModel.featureState.appIcons },
      appIconDrawables = appIconDrawableBuilder::build
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
        listInteractionController.onProcessFilterChanged(process)
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
  private val headerController by unsafeLazy {
    DetailHeaderController(
      activity = this,
      supportActionBar = { supportActionBar },
      collapsingToolbar = binding.collapsingToolbar,
      headerLayout = binding.headerLayout,
      headerTitleBinder = headerTitleBinder,
      headerExtraInfoBinder = headerExtraInfoBinder,
      viewModel = viewModel,
      coroutineScope = lifecycleScope,
      isDisplayOptionEnabled = ::isDisplayOptionEnabled,
      harmonyBundleInfo = { packageName ->
        bundleManager?.getBundleInfo(
          packageName,
          IBundleManager.GET_BUNDLE_DEFAULT
        )
      }
    )
  }
  private val tabSpecBuilder by unsafeLazy {
    DetailTabSpecBuilder(this, buildAppDetailTabTypes)
  }
  private val tabController: DetailTabController by unsafeLazy {
    DetailTabController(
      activity = this,
      viewPager = binding.viewpager,
      tabLayout = binding.tabLayout,
      onTabSelected = { type -> listInteractionController.onDetailTabSelected(type) },
      onProcessTooltipTextChanged = ::updateProcessToolbarTooltip
    )
  }
  private val listInteractionController: DetailListInteractionController by unsafeLazy {
    DetailListInteractionController(
      viewModel = viewModel,
      detailFragmentManager = detailFragmentManager,
      appDetailSettingsRepository = appDetailSettingsRepository,
      selectedType = { tabController.selectedType },
      processBarController = processBarController,
      coroutineScope = lifecycleScope,
      onCurrentItemsCountChanged = { count ->
        binding.tsComponentCount.setText(count.toString())
      }
    )
  }
  private val packageContentController by unsafeLazy {
    DetailPackageContentController(
      viewModel = viewModel,
      coroutineScope = lifecycleScope,
      buildAppDetailContentInitPlan = buildAppDetailContentInitPlan,
      tabSpecBuilder = tabSpecBuilder,
      tabController = tabController,
      featureListController = featureListController,
      currentUiGeneration = { packageUiGeneration },
      staticLibraryTitle = { getText(R.string.ref_category_static) },
      onStaticLibsAvailable = ::onStaticLibsAvailable,
      onPostPackageInfoAvailable = ::onPostPackageInfoAvailable
    )
  }
  private val toolbarController by unsafeLazy {
    DetailToolbarController(
      toolbarView = binding.rvToolbar,
      appBarLayout = binding.headerLayout,
      onSortClick = { listInteractionController.toggleSortMode() },
      onQuickLaunchClick = ::showCurrentAppInfoDialog,
      onProcessClick = { listInteractionController.toggleProcessMode() }
    )
  }
  private val menuController by unsafeLazy {
    DetailMenuController(
      context = this,
      toolbar = binding.toolbar,
      onNavigateUp = ::finish,
      onQueryTextChanged = { text -> listInteractionController.onSearchTextChanged(text) }
    )
  }
  private val packageComparisonController by unsafeLazy {
    DetailPackageComparisonController(
      activity = this,
      viewModel = viewModel,
      toolbarController = toolbarController,
      coroutineScope = lifecycleScope,
      currentUiGeneration = { packageUiGeneration }
    )
  }
  private val stateObserverController by unsafeLazy {
    DetailStateObserverController(
      viewModel = viewModel,
      coroutineScope = lifecycleScope,
      onItemsCountChanged = { live -> listInteractionController.onItemsCountChanged(live) },
      onProcessToolIconVisibilityChanged = toolbarController::setProcessActionVisible,
      onProcessMapChanged = processBarController::setData,
      onFeatureAdded = ::addFeatureItem,
      onAbiBundleChanged = ::onAbiBundleChanged
    )
  }

  private var isHarmonyMode = false
  private var packageUiGeneration = 0

  override fun onCreate(savedInstanceState: Bundle?) {
    binding = ActivityAppDetailBinding.inflate(layoutInflater)
    super.onCreate(savedInstanceState)
    binding.viewpager.isSaveEnabled = false
    addMenuProvider(menuController, this, Lifecycle.State.CREATED)
    setSupportActionBar(getToolbar())
    binding.toolbar.isBackInvokedCallbackEnabled = false
    supportActionBar?.apply {
      setDisplayHomeAsUpEnabled(true)
      setDisplayShowHomeEnabled(true)
    }
    onBackPressedDispatcher.addBackStateHandler(
      lifecycleOwner = this,
      enabledState = { !isKeyboardShowing() && menuController.hasExpandedActionView() },
      handler = { menuController.collapseActionView() }
    )
    stateObserverController.observe()
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
    val apkPreviewInfo = viewModel.apkPreviewInfo

    viewModel.initPackageInfo(packageInfo)
    if (apkPreviewInfo != null) {
      viewModel.initAbiInfo(apkPreviewInfo)
    } else {
      viewModel.initAbiInfo(packageInfo, apkAnalyticsMode)
    }

    val headerResult = headerController.bind(
      packageInfo = packageInfo,
      extraBean = extraBean,
      isHarmonyMode = isHarmonyMode,
      apkAnalyticsMode = apkAnalyticsMode
    ) ?: return
    val packageName = headerResult.packageName

    toolbarController.setupBaseActions(
      showHarmonyToggle = extraBean?.variant == Constants.VARIANT_HAP,
      onHarmonyToggle = {
        isHarmonyMode = !isHarmonyMode
        onPackageInfoAvailable(packageInfo, extraBean)
      }
    )
    if (apkAnalyticsMode && !viewModel.isApkPreview) {
      packageComparisonController.setupIfAvailable(packageName, uiGeneration)
    }

    packageContentController.bind(
      packageInfo = packageInfo,
      extraBean = extraBean,
      packageName = packageName,
      isHarmonyMode = isHarmonyMode,
      uiGeneration = uiGeneration
    )
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

  override fun collapseAppBar() {
    binding.headerLayout.setExpanded(false, true)
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

  private fun updateProcessToolbarTooltip(@StringRes tooltipTextRes: Int) {
    toolbarController.updateProcessTooltip(tooltipTextRes)
  }

  private fun onAbiBundleChanged(abi: Int, abiSet: Collection<Int>) {
    initAbiView(abi, abiSet)

    doOnMainThreadIdle {
      featureListController.attachWithAnimation()
    }
  }

  private fun showCurrentAppInfoDialog() {
    if (viewModel.isPackageInfoAvailable()) {
      showAppInfoDialog(viewModel.packageInfo.packageName)
    }
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
}
