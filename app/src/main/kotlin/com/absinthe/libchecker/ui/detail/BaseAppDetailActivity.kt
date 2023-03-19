package com.absinthe.libchecker.ui.detail

import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.format.Formatter
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.core.text.HtmlCompat
import androidx.core.text.buildSpannedString
import androidx.core.text.scale
import androidx.core.view.MenuProvider
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
import com.absinthe.libchecker.app.SystemServices
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
import com.absinthe.libchecker.ui.base.BaseAlertDialogBuilder
import com.absinthe.libchecker.model.AppDetailToolbarItem
import com.absinthe.libchecker.model.DetailExtraBean
import com.absinthe.libchecker.model.FeatureItem
import com.absinthe.libchecker.model.SnapshotDiffItem
import com.absinthe.libchecker.compat.VersionCompat
import com.absinthe.libchecker.constant.AbilityType
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.database.entity.Features
import com.absinthe.libchecker.databinding.ActivityAppDetailBinding
import com.absinthe.libchecker.recyclerview.adapter.detail.AppDetailToolbarAdapter
import com.absinthe.libchecker.recyclerview.adapter.detail.FeatureAdapter
import com.absinthe.libchecker.recyclerview.adapter.detail.ProcessBarAdapter
import com.absinthe.libchecker.recyclerview.adapter.detail.node.AbiLabelNode
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
import com.absinthe.libchecker.ui.fragment.detail.impl.SignaturesAnalysisFragment
import com.absinthe.libchecker.ui.fragment.detail.impl.StaticAnalysisFragment
import com.absinthe.libchecker.utils.FileUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.PackageUtils.getFeatures
import com.absinthe.libchecker.utils.PackageUtils.getPermissionsList
import com.absinthe.libchecker.utils.PackageUtils.isOverlay
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.utils.extensions.doOnMainThreadIdle
import com.absinthe.libchecker.utils.extensions.dp
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
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.analytics.EventProperties
import java.io.File
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.zhanghai.android.appiconloader.AppIconLoader
import ohos.bundle.IBundleManager
import rikka.core.util.ClipboardUtils
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
  private var easterEggTabA = -1
  private var easterEggTabB = -1
  private var easterEggCount = 0

  override fun onCreate(savedInstanceState: Bundle?) {
    binding = ActivityAppDetailBinding.inflate(layoutInflater)
    super.onCreate(savedInstanceState)
    addMenuProvider(this)
    setSupportActionBar(getToolbar())
    supportActionBar?.apply {
      setDisplayHomeAsUpEnabled(true)
      setDisplayShowHomeEnabled(true)
    }
    onBackPressedDispatcher.addCallback(this, true) {
      val closeBtn = findViewById<View>(androidx.appcompat.R.id.search_close_btn)
      if (closeBtn != null) {
        binding.toolbar.collapseActionView()
      } else {
        finish()
      }
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
          it.title = packageInfo.applicationInfo?.loadLabel(packageManager)?.toString()
            ?: getString(R.string.detail_label)
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

        val trueAbi = abi.mod(Constants.MULTI_ARCH)
        viewModel.is64Bit.postValue(trueAbi == Constants.ARMV8 || trueAbi == Constants.X86_64)

        val versionInfo = buildSpannedString {
          if (!isHarmonyMode) {
            scale(0.8f) {
              append("Target: ")
            }
            append(PackageUtils.getTargetApiString(packageInfo))
            scale(0.8f) {
              append(" Min: ")
            }
            append(PackageUtils.getMinSdkVersion(packageInfo).toString())
            scale(0.8f) {
              append(" Compile: ")
            }
            append(PackageUtils.getCompileSdkVersion(packageInfo))
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

        if (abiSet.isNotEmpty() && !abiSet.contains(Constants.OVERLAY) && !abiSet.contains(Constants.ERROR)) {
          val abiLabelsList = mutableListOf<AbiLabelNode>()

          if (abi >= Constants.MULTI_ARCH) {
            abiLabelsList.add(AbiLabelNode(Constants.MULTI_ARCH, true))
          }

          abiSet.forEach {
            if (it != Constants.NO_LIBS) {
              abiLabelsList.add(
                AbiLabelNode(
                  it,
                  apkAnalyticsMode || it == abi % Constants.MULTI_ARCH
                )
              )
            }
          }
          detailsTitle.abiLabelsAdapter.setList(abiLabelsList)
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
            var features = extraBean?.features ?: -1
            if (features == -1) {
              features = packageInfo.getFeatures()
              Repositories.lcRepository.updateFeatures(packageInfo.packageName, features)
            }

            withContext(Dispatchers.Main) {
              initFeatures(packageInfo, features)
            }
          }
        }
      } catch (e: Exception) {
        Timber.e(e)
        Toasty.showLong(this@BaseAppDetailActivity, e.toString())
        finish()
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
        if (detailFragmentManager.currentFragment?.isComponentFragment() == true) {
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
    viewModel.processMapLiveData.observe(this) {
      if (processBarView == null) {
        initProcessBarView()
      }
      processBarView?.setData(
        it.map { mapItem ->
          ProcessBarAdapter.ProcessBarItem(
            mapItem.key,
            mapItem.value
          )
        }
      )
      showProcessBarView()
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

      // if (!isListReady) {
      //   isVisible = false
      // }
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
    binding.headerContentLayout.addView(featureListView)
    return true
  }

  private suspend fun initFeatures(packageInfo: PackageInfo, features: Int) {
    initFeatureListView()

    if ((features and Features.SPLIT_APKS) > 0) {
      featureAdapter.addData(
        FeatureItem(R.drawable.ic_aab) {
          AppBundleBottomSheetDialogFragment().apply {
            arguments = bundleOf(
              EXTRA_PACKAGE_NAME to packageInfo.packageName
            )
            show(supportFragmentManager, AppBundleBottomSheetDialogFragment::class.java.name)
          }
        }
      )
    }
    if ((features and Features.KOTLIN_USED) > 0) {
      val dialog = BaseAlertDialogBuilder(this)
        .setIcon(R.drawable.ic_kotlin_logo)
        .setTitle(R.string.kotlin_string)
        .setMessage(R.string.kotlin_details)
        .setPositiveButton(android.R.string.ok, null)
      featureAdapter.addData(
        FeatureItem(R.drawable.ic_kotlin_logo) {
          dialog.show()
        }
      )
      withContext(Dispatchers.IO) {
        PackageUtils.getKotlinPluginVersion(packageInfo)?.let { version ->
          withContext(Dispatchers.Main) {
            dialog.setTitle(
              HtmlCompat.fromHtml(
                "${getString(R.string.kotlin_string)} <b>$version</b>",
                HtmlCompat.FROM_HTML_MODE_COMPACT
              )
            )
          }
        }
      }
    }
    if ((features and Features.RX_JAVA) > 0) {
      val dialog = BaseAlertDialogBuilder(this@BaseAppDetailActivity)
        .setIcon(R.drawable.ic_reactivex)
        .setTitle(R.string.rxjava)
        .setMessage(R.string.rx_detail)
        .setPositiveButton(android.R.string.ok, null)
      featureAdapter.addData(
        FeatureItem(R.drawable.ic_reactivex) {
          dialog.show()
        }
      )
      withContext(Dispatchers.IO) {
        PackageUtils.getRxJavaVersion(packageInfo)?.let { version ->
          withContext(Dispatchers.Main) {
            dialog.setTitle(
              HtmlCompat.fromHtml(
                "${getString(R.string.rxjava)} <b>$version</b>",
                HtmlCompat.FROM_HTML_MODE_COMPACT
              )
            )
          }
        }
      }
    }
    if ((features and Features.RX_KOTLIN) > 0) {
      val dialog = BaseAlertDialogBuilder(this@BaseAppDetailActivity)
        .setIcon(R.drawable.ic_rxkotlin)
        .setTitle(R.string.rxkotlin)
        .setMessage(R.string.rx_kotlin_detail)
        .setPositiveButton(android.R.string.ok, null)
      featureAdapter.addData(
        FeatureItem(R.drawable.ic_rxkotlin) {
          dialog.show()
        }
      )
      withContext(Dispatchers.IO) {
        PackageUtils.getRxKotlinVersion(packageInfo)?.let { version ->
          withContext(Dispatchers.Main) {
            dialog.setTitle(
              HtmlCompat.fromHtml(
                "${getString(R.string.rxkotlin)} <b>$version</b>",
                HtmlCompat.FROM_HTML_MODE_COMPACT
              )
            )
          }
        }
      }
    }
    if ((features and Features.RX_ANDROID) > 0) {
      val dialog = BaseAlertDialogBuilder(this@BaseAppDetailActivity)
        .setIcon(R.drawable.ic_rxandroid)
        .setTitle(R.string.rxandroid)
        .setMessage(R.string.rx_android_detail)
        .setPositiveButton(android.R.string.ok, null)
      featureAdapter.addData(
        FeatureItem(R.drawable.ic_rxandroid) {
          dialog.show()
        }
      )
      withContext(Dispatchers.IO) {
        PackageUtils.getRxAndroidVersion(packageInfo)?.let { version ->
          withContext(Dispatchers.Main) {
            dialog.setTitle(
              HtmlCompat.fromHtml(
                "${getString(R.string.rxandroid)} <b>$version</b>",
                HtmlCompat.FROM_HTML_MODE_COMPACT
              )
            )
          }
        }
      }
    }
    if ((features and Features.AGP) > 0) {
      val dialog = BaseAlertDialogBuilder(this@BaseAppDetailActivity)
        .setIcon(R.drawable.ic_gradle)
        .setTitle(R.string.agp)
        .setMessage(R.string.agp_details)
        .setPositiveButton(android.R.string.ok, null)
      featureAdapter.addData(
        FeatureItem(R.drawable.ic_gradle) {
          dialog.show()
        }
      )
      withContext(Dispatchers.IO) {
        PackageUtils.getAGPVersion(packageInfo)?.let { version ->
          withContext(Dispatchers.Main) {
            dialog.setTitle(
              HtmlCompat.fromHtml(
                "${getString(R.string.agp)} <b>$version</b>",
                HtmlCompat.FROM_HTML_MODE_COMPACT
              )
            )
          }
        }
      }
    }

    if ((features and Features.XPOSED_MODULE) > 0) {
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

    if ((features and Features.PLAY_SIGNING) > 0) {
      featureAdapter.addData(
        FeatureItem(com.absinthe.lc.rulesbundle.R.drawable.ic_lib_play_store) {
          BaseAlertDialogBuilder(this@BaseAppDetailActivity)
            .setIcon(com.absinthe.lc.rulesbundle.R.drawable.ic_lib_play_store)
            .setTitle(R.string.play_app_signing)
            .setMessage(R.string.play_app_signing_details)
            .setPositiveButton(android.R.string.ok, null)
            .show()
        }
      )
    }

    if ((features and Features.PWA) > 0) {
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

    if ((features and Features.JETPACK_COMPOSE) > 0) {
      val dialog = BaseAlertDialogBuilder(this@BaseAppDetailActivity)
        .setIcon(com.absinthe.lc.rulesbundle.R.drawable.ic_lib_jetpack_compose)
        .setTitle(R.string.jetpack_compose)
        .setMessage(R.string.jetpack_compose_details)
        .setPositiveButton(android.R.string.ok, null)
      featureAdapter.addData(
        FeatureItem(com.absinthe.lc.rulesbundle.R.drawable.ic_lib_jetpack_compose) {
          dialog.show()
        }
      )
      withContext(Dispatchers.IO) {
        PackageUtils.getJetpackComposeVersion(packageInfo)?.let { version ->
          withContext(Dispatchers.Main) {
            dialog.setTitle(
              HtmlCompat.fromHtml(
                "${getString(R.string.jetpack_compose)} <b>$version</b>",
                HtmlCompat.FROM_HTML_MODE_COMPACT
              )
            )
          }
        }
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
        processBarView!!.setData(
          viewModel.processesMap.map { mapItem ->
            ProcessBarAdapter.ProcessBarItem(
              mapItem.key,
              mapItem.value
            )
          }
        )
        processBarView?.isVisible = true
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
        basePackage.applicationInfo.loadLabel(SystemServices.packageManager).toString(),
        analysisPackage.applicationInfo.loadLabel(SystemServices.packageManager).toString()
      ),
      versionNameDiff = SnapshotDiffItem.DiffNode(
        basePackage.versionName,
        analysisPackage.versionName
      ),
      versionCodeDiff = SnapshotDiffItem.DiffNode(
        PackageUtils.getVersionCode(basePackage),
        PackageUtils.getVersionCode(analysisPackage)
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
        PackageUtils.getPackageSize(basePackage, true),
        PackageUtils.getPackageSize(analysisPackage, true)
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
}
