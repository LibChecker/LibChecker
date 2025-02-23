package com.absinthe.libchecker.features.applist.detail.ui

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.toColorInt
import androidx.core.os.bundleOf
import androidx.core.text.buildSpannedString
import androidx.core.text.scale
import androidx.core.view.MenuProvider
import androidx.core.view.isGone
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
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.annotation.SIGNATURES
import com.absinthe.libchecker.annotation.STATIC
import com.absinthe.libchecker.constant.AbilityType
import com.absinthe.libchecker.constant.AndroidVersions
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.options.AdvancedOptions
import com.absinthe.libchecker.database.entity.Features
import com.absinthe.libchecker.databinding.ActivityAppDetailBinding
import com.absinthe.libchecker.features.applist.DetailFragmentManager
import com.absinthe.libchecker.features.applist.MODE_SORT_BY_LIB
import com.absinthe.libchecker.features.applist.MODE_SORT_BY_SIZE
import com.absinthe.libchecker.features.applist.detail.AppBarStateChangeListener
import com.absinthe.libchecker.features.applist.detail.DetailViewModel
import com.absinthe.libchecker.features.applist.detail.FeaturesDialog
import com.absinthe.libchecker.features.applist.detail.IDetailContainer
import com.absinthe.libchecker.features.applist.detail.bean.AppDetailToolbarItem
import com.absinthe.libchecker.features.applist.detail.bean.DetailExtraBean
import com.absinthe.libchecker.features.applist.detail.bean.FeatureItem
import com.absinthe.libchecker.features.applist.detail.ui.adapter.AppDetailToolbarAdapter
import com.absinthe.libchecker.features.applist.detail.ui.adapter.FeatureAdapter
import com.absinthe.libchecker.features.applist.detail.ui.adapter.ProcessBarAdapter
import com.absinthe.libchecker.features.applist.detail.ui.adapter.node.AbiLabelNode
import com.absinthe.libchecker.features.applist.detail.ui.impl.AbilityAnalysisFragment
import com.absinthe.libchecker.features.applist.detail.ui.impl.ComponentsAnalysisFragment
import com.absinthe.libchecker.features.applist.detail.ui.impl.MetaDataAnalysisFragment
import com.absinthe.libchecker.features.applist.detail.ui.impl.NativeAnalysisFragment
import com.absinthe.libchecker.features.applist.detail.ui.impl.PermissionAnalysisFragment
import com.absinthe.libchecker.features.applist.detail.ui.impl.SignaturesAnalysisFragment
import com.absinthe.libchecker.features.applist.detail.ui.impl.StaticAnalysisFragment
import com.absinthe.libchecker.features.applist.detail.ui.view.ProcessBarView
import com.absinthe.libchecker.features.snapshot.detail.bean.SnapshotDiffItem
import com.absinthe.libchecker.features.snapshot.detail.ui.EXTRA_ENTITY
import com.absinthe.libchecker.features.snapshot.detail.ui.SnapshotDetailActivity
import com.absinthe.libchecker.ui.app.CheckPackageOnResumingActivity
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
import com.absinthe.libchecker.utils.extensions.getCompileSdkVersionString
import com.absinthe.libchecker.utils.extensions.getPackageSize
import com.absinthe.libchecker.utils.extensions.getPermissionsList
import com.absinthe.libchecker.utils.extensions.getTargetApiString
import com.absinthe.libchecker.utils.extensions.getVersionCode
import com.absinthe.libchecker.utils.extensions.getVersionString
import com.absinthe.libchecker.utils.extensions.isKeyboardShowing
import com.absinthe.libchecker.utils.extensions.setLongClickCopiedToClipboard
import com.absinthe.libchecker.utils.extensions.sizeToString
import com.absinthe.libchecker.utils.extensions.unsafeLazy
import com.absinthe.libchecker.utils.harmony.ApplicationDelegate
import com.absinthe.libchecker.utils.toJson
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
  }

  @SuppressLint("UnsafeIntentLaunch")
  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    finish()
    startActivity(intent)
  }

  protected fun onPackageInfoAvailable(packageInfo: PackageInfo, extraBean: DetailExtraBean?) {
    viewModel.packageInfo = packageInfo
    lifecycleScope.launch {
      viewModel.packageInfoStateFlow.emit(packageInfo)
    }
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
            packageInfo.applicationInfo?.let { appInfo ->
              load(appIconLoader.loadIcon(appInfo))
            }
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
        val showAndroidVersion =
          (GlobalValues.advancedOptions and AdvancedOptions.SHOW_ANDROID_VERSION) > 0
        val ai = packageInfo.applicationInfo!!
        val versionInfo = buildSpannedString {
          if (!isHarmonyMode) {
            scale(0.8f) {
              append("Target: ")
            }
            append(packageInfo.getTargetApiString())
            if (showAndroidVersion) {
              append(" (${AndroidVersions.simpleVersions[ai.targetSdkVersion]})")
            }
            scale(0.8f) {
              append(" Min: ")
            }
            append(ai.minSdkVersion.toString())
            if (showAndroidVersion) {
              append(" (${AndroidVersions.simpleVersions[ai.minSdkVersion]})")
            }
            scale(0.8f) {
              append(" Compile: ")
            }
            append(packageInfo.getCompileSdkVersionString())
            if (showAndroidVersion) {
              append(" (${AndroidVersions.simpleVersions[packageInfo.getCompileSdkVersion()]})")
            }
            scale(0.8f) {
              append(" Size: ")
            }
            var baseApkSize = FileUtils.getFileSize(ai.sourceDir)
            val baseFormattedApkSize = baseApkSize.sizeToString(this@BaseAppDetailActivity, showBytes = false)
            val splitApkSizeList = PackageUtils.getSplitsSourceDir(packageInfo)
              ?.map {
                val size = FileUtils.getFileSize(it)
                baseApkSize += size
                size.sizeToString(this@BaseAppDetailActivity, showBytes = false)
              }
              ?.toMutableList()

            if (splitApkSizeList.isNullOrEmpty()) {
              append(baseFormattedApkSize)
            } else {
              splitApkSizeList.add(0, baseFormattedApkSize)
              val totalSize = baseApkSize.sizeToString(this@BaseAppDetailActivity, showBytes = false)
              append(
                splitApkSizeList.joinToString(separator = " + ", prefix = "(", postfix = " = $totalSize)")
              )
            }

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
            GlobalValues.libSortMode = if (GlobalValues.libSortMode == MODE_SORT_BY_LIB) {
              MODE_SORT_BY_SIZE
            } else {
              MODE_SORT_BY_LIB
            }
            detailFragmentManager.sortAll()
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
        // DEX,
        SIGNATURES
      )
    } else {
      mutableListOf(
        NATIVE,
        AbilityType.PAGE,
        AbilityType.SERVICE,
        AbilityType.WEB,
        AbilityType.DATA,
        // DEX,
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
        // getText(R.string.ref_category_dex),
        getText(R.string.ref_category_signatures)
      )
    } else {
      mutableListOf(
        getText(R.string.ref_category_native),
        getText(R.string.ability_page),
        getText(R.string.ability_service),
        getText(R.string.ability_web),
        getText(R.string.ability_data),
        // getText(R.string.ref_category_dex),
        getText(R.string.ref_category_signatures)
      )
    }

    if (packageInfo.applicationInfo?.sharedLibraryFiles?.isNotEmpty() == true) {
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

            // DEX -> DexAnalysisFragment.newInstance(packageInfo.packageName)

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
      it.itemsCountStateFlow.onEach { live ->
        if (detailFragmentManager.currentItemsCount != live.count && typeList[binding.tabLayout.selectedTabPosition] == live.locate) {
          binding.tsComponentCount.setText(live.count.toString())
          detailFragmentManager.currentItemsCount = live.count
        }
      }.launchIn(lifecycleScope)
      it.processToolIconVisibilityStateFlow.onEach { visible ->
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
      it.processMapStateFlow.onEach { map ->
        val list = map.map { mapItem ->
          ProcessBarAdapter.ProcessBarItem(
            mapItem.key,
            mapItem.value
          )
        }
        setupProcessBarView(list)
      }.launchIn(lifecycleScope)
      it.featuresFlow.onEach { feat ->
        initFeatureListView()

        when (feat.featureType) {
          Features.SPLIT_APKS -> {
            featureAdapter.addData(
              FeatureItem(R.drawable.ic_aab) {
                FeaturesDialog.showSplitApksDialog(this, packageInfo)
              }
            )
          }

          Features.KOTLIN_USED -> {
            featureAdapter.addData(
              FeatureItem(com.absinthe.lc.rulesbundle.R.drawable.ic_lib_kotlin) {
                FeaturesDialog.showKotlinDialog(this, feat.extras)
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
              FeatureItem(R.drawable.ic_reactivex, "#7F52FF".toColorInt()) {
                FeaturesDialog.showRxKotlinDialog(this, feat.version)
              }
            )
          }

          Features.RX_ANDROID -> {
            featureAdapter.addData(
              FeatureItem(R.drawable.ic_reactivex, "#3DDC84".toColorInt()) {
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

          Features.KMP -> {
            featureAdapter.addData(
              FeatureItem(R.drawable.ic_jetbrain_kmp) {
                FeaturesDialog.showKMPDialog(this, feat.version)
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

          Features.Ext.ELF_PAGE_SIZE_16KB -> {
            featureAdapter.addData(
              FeatureItem(R.drawable.ic_16kb_align) {
                FeaturesDialog.show16KBAlignDialog(this)
              }
            )
          }

          Features.Ext.ELF_PAGE_SIZE_16KB_COMPAT -> {
            featureAdapter.addData(
              FeatureItem(R.drawable.ic_16kb_compat) {
                FeaturesDialog.show16KBCompatDialog(this)
              }
            )
          }
        }
      }.launchIn(lifecycleScope)
      it.abiBundleStateFlow.onEach { bundle ->
        if (bundle != null) {
          initAbiView(bundle.abi, bundle.abiSet)

          val action: Runnable = object : Runnable {
            override fun run() {
              if (featureListView == null) {
                initFeatureListView()
              }
              if (featureListView?.parent != null) {
                return
              }
              val oldContainerHeight = binding.headerContentLayout.height
              val newContainerHeight = oldContainerHeight + 40.dp
              val params = binding.headerContentLayout.layoutParams

              binding.headerContentLayout.addView(featureListView)
              ValueAnimator.ofInt(oldContainerHeight, newContainerHeight).also { anim ->
                anim.addUpdateListener { valueAnimator ->
                  val height = valueAnimator.animatedValue as Int

                  if (valueAnimator.animatedFraction == 1f || featureAdapter.data.isEmpty()) {
                    params.height = ViewGroup.LayoutParams.WRAP_CONTENT
                  } else {
                    params.height = height
                  }
                  binding.headerContentLayout.layoutParams = params
                }
                anim.duration = 250
                anim.start()
              }
            }
          }
          binding.detailsTitle.postDelayed(action, 500)
        }
      }.launchIn(lifecycleScope)
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
    detailFragmentManager.deliverFilterItemsByText(newText, lifecycleScope)
    return false
  }

  override fun collapseAppBar() {
    binding.headerLayout.setExpanded(false, true)
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
      GlobalValues.processMode = !GlobalValues.processMode

      toggleProcessBarViewVisibility()
      if (!GlobalValues.processMode) {
        doOnMainThreadIdle {
          viewModel.queriedProcess = null
          detailFragmentManager.deliverFilterItems(null, null, lifecycleScope)
        }
      }
    }
  }

  private fun navigateToSnapshotDetailPage(basePackage: PackageInfo, analysisPackage: PackageInfo) {
    val diff = SnapshotDiffItem(
      packageName = basePackage.packageName,
      updateTime = basePackage.lastUpdateTime,
      labelDiff = SnapshotDiffItem.DiffNode(
        basePackage.getAppName().toString(),
        analysisPackage.getAppName().toString()
      ),
      versionNameDiff = SnapshotDiffItem.DiffNode(
        basePackage.versionName.toString(),
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
        basePackage.applicationInfo?.targetSdkVersion?.toShort() ?: 0,
        analysisPackage.applicationInfo?.targetSdkVersion?.toShort()
      ),
      compileSdkDiff = SnapshotDiffItem.DiffNode(
        basePackage.getCompileSdkVersion().toShort(),
        analysisPackage.getCompileSdkVersion().toShort()
      ),
      minSdkDiff = SnapshotDiffItem.DiffNode(
        basePackage.applicationInfo?.minSdkVersion?.toShort() ?: 0,
        analysisPackage.applicationInfo?.minSdkVersion?.toShort()
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
        detailFragmentManager.deliverFilterItems(null, viewModel.queriedProcess, lifecycleScope)
      }
    }
    binding.detailToolbarContainer.addView(processBarView)
  }

  private fun setupProcessBarView(list: List<ProcessBarAdapter.ProcessBarItem>) {
    if (list.isEmpty()) {
      if (processBarView?.parent != null) {
        (processBarView?.parent as? ViewGroup)?.removeView(processBarView)
        processBarView = null
      }
    } else {
      if (processBarView == null) {
        initProcessBarView()
      }
      toggleProcessBarViewVisibility()
      processBarView?.setData(list)
    }
  }

  private fun toggleProcessBarViewVisibility() {
    processBarView?.isGone =
      !GlobalValues.processMode &&
      detailFragmentManager.currentFragment?.hasNonGrantedPermissions() == false
  }

  private fun initAbiView(abi: Int, abiSet: Collection<Int>) {
    val trueAbi = abi.mod(Constants.MULTI_ARCH)
    lifecycleScope.launch {
      viewModel.is64Bit.emit(PackageUtils.isAbi64Bit(trueAbi))
    }

    if (abiSet.isNotEmpty() && !abiSet.contains(Constants.OVERLAY) && !abiSet.contains(Constants.ERROR)) {
      val abiLabelsList = mutableListOf<AbiLabelNode>()

      if (abi >= Constants.MULTI_ARCH) {
        abiLabelsList.add(
          AbiLabelNode(Constants.MULTI_ARCH, true) {
            FeaturesDialog.showMultiArchDialog(this)
          }
        )
      }

      abiSet.forEach {
        if (it != Constants.NO_LIBS) {
          val isActive = apkAnalyticsMode || it == abi % Constants.MULTI_ARCH
          abiLabelsList.add(AbiLabelNode(it, isActive))
        }
      }
      binding.detailsTitle.setAbiLabels(abiLabelsList)
    }
  }
}
