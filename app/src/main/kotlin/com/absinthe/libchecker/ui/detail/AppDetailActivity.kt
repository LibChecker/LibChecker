package com.absinthe.libchecker.ui.detail

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageInfoHidden
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.text.style.StrikethroughSpan
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import coil.load
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.ALL
import com.absinthe.libchecker.annotation.DEX
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.annotation.STATIC
import com.absinthe.libchecker.bean.AppDetailToolbarItem
import com.absinthe.libchecker.bean.DetailExtraBean
import com.absinthe.libchecker.bean.FeatureItem
import com.absinthe.libchecker.constant.AbilityType
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.databinding.ActivityAppDetailBinding
import com.absinthe.libchecker.recyclerview.adapter.detail.AppDetailToolbarAdapter
import com.absinthe.libchecker.recyclerview.adapter.detail.FeatureAdapter
import com.absinthe.libchecker.ui.fragment.detail.AppBundleBottomSheetDialogFragment
import com.absinthe.libchecker.ui.fragment.detail.AppInfoBottomSheetDialogFragment
import com.absinthe.libchecker.ui.fragment.detail.MODE_SORT_BY_LIB
import com.absinthe.libchecker.ui.fragment.detail.MODE_SORT_BY_SIZE
import com.absinthe.libchecker.ui.fragment.detail.impl.AbilityAnalysisFragment
import com.absinthe.libchecker.ui.fragment.detail.impl.ComponentsAnalysisFragment
import com.absinthe.libchecker.ui.fragment.detail.impl.DexAnalysisFragment
import com.absinthe.libchecker.ui.fragment.detail.impl.MetaDataAnalysisFragment
import com.absinthe.libchecker.ui.fragment.detail.impl.NativeAnalysisFragment
import com.absinthe.libchecker.ui.fragment.detail.impl.PermissionAnalysisFragment
import com.absinthe.libchecker.ui.fragment.detail.impl.StaticAnalysisFragment
import com.absinthe.libchecker.ui.main.EXTRA_REF_NAME
import com.absinthe.libchecker.ui.main.EXTRA_REF_TYPE
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.PackageUtils.isPWA
import com.absinthe.libchecker.utils.PackageUtils.isPlayAppSigning
import com.absinthe.libchecker.utils.PackageUtils.isXposedModule
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.utils.extensions.getDrawable
import com.absinthe.libchecker.utils.extensions.isOrientationPortrait
import com.absinthe.libchecker.utils.extensions.setLongClickCopiedToClipboard
import com.absinthe.libchecker.utils.extensions.unsafeLazy
import com.absinthe.libchecker.utils.harmony.ApplicationDelegate
import com.absinthe.libchecker.view.detail.AppBarStateChangeListener
import com.absinthe.libchecker.view.detail.CenterAlignImageSpan
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dev.rikka.tools.refine.Refine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.zhanghai.android.appiconloader.AppIconLoader
import ohos.bundle.IBundleManager
import rikka.core.util.ClipboardUtils
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.Arrays
import kotlin.math.abs

@SuppressLint("InlinedApi")
const val EXTRA_PACKAGE_NAME = Intent.EXTRA_PACKAGE_NAME
const val EXTRA_DETAIL_BEAN = "EXTRA_DETAIL_BEAN"

class AppDetailActivity : BaseAppDetailActivity<ActivityAppDetailBinding>(), IDetailContainer {

  private val pkgName by unsafeLazy { intent.getStringExtra(EXTRA_PACKAGE_NAME) }
  private val refName by unsafeLazy { intent.getStringExtra(EXTRA_REF_NAME) }
  private val refType by unsafeLazy { intent.getIntExtra(EXTRA_REF_TYPE, ALL) }
  private val extraBean by unsafeLazy { intent.getParcelableExtra(EXTRA_DETAIL_BEAN) as? DetailExtraBean }
  private val bundleManager by unsafeLazy { ApplicationDelegate(this).iBundleManager }
  private val featureAdapter by unsafeLazy { FeatureAdapter() }
  private val toolbarAdapter by unsafeLazy { AppDetailToolbarAdapter() }
  private val toolbarQuicklyLaunchItem by unsafeLazy {
    AppDetailToolbarItem(
      R.drawable.ic_launch,
      R.string.further_operation
    ) {
      AppInfoBottomSheetDialogFragment().apply {
        arguments = bundleOf(
          EXTRA_PACKAGE_NAME to pkgName
        )
        show(supportFragmentManager, tag)
      }
    }
  }

  private var isHarmonyMode = false
  private var isToolbarCollapsed = false
  private var featureListView: RecyclerView? = null
  private var typeList = mutableListOf<Int>()

  override fun requirePackageName() = pkgName
  override fun getToolbar() = binding.toolbar

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    isPackageReady = true
    initView()
    resolveReferenceExtras()

    PackageUtils.getPackageInfo(pkgName!!).apply {
      val installed = applicationInfo.flags.and(ApplicationInfo.FLAG_INSTALLED) != 0
      val multiArch = applicationInfo.flags.and(ApplicationInfo.FLAG_MULTIARCH) != 0
      val enable = applicationInfo.enabled
      val shared = Arrays.toString(applicationInfo.sharedLibraryFiles)

      Timber.d("installed: $installed, multiArch: $multiArch, enable: $enable, shared: $shared")
    }
  }

  override fun onStart() {
    super.onStart()
    registerPackageBroadcast()
  }

  override fun onStop() {
    super.onStop()
    unregisterPackageBroadcast()
  }

  private fun initView() {
    pkgName?.let { packageName ->
      val packageInfo = runCatching {
        PackageUtils.getPackageInfo(
          packageName,
          PackageManager.GET_META_DATA
        )
      }.getOrNull() ?: return
      viewModel.packageName = packageName
      binding.apply {
        try {
          supportActionBar?.title = null
          collapsingToolbar.also {
            it.setOnApplyWindowInsetsListener(null)
            it.title = try {
              packageInfo.applicationInfo.loadLabel(packageManager).toString()
            } catch (e: PackageManager.NameNotFoundException) {
              getString(R.string.detail_label)
            }
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
                this@AppDetailActivity
              )
              load(appIconLoader.loadIcon(packageInfo.applicationInfo))
              setOnClickListener {
                if (AntiShakeUtils.isInvalidClick(it)) {
                  return@setOnClickListener
                }
                AppInfoBottomSheetDialogFragment().apply {
                  arguments = bundleOf(
                    EXTRA_PACKAGE_NAME to pkgName
                  )
                  show(supportFragmentManager, tag)
                }
              }
              setOnLongClickListener {
                (drawable as? BitmapDrawable)?.bitmap?.let { bitmap ->
                  val iconFile = File(externalCacheDir, Constants.TEMP_ICON)
                  if (!iconFile.exists()) {
                    iconFile.createNewFile()
                  }
                  FileOutputStream(iconFile).use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                    it.flush()
                  }
                  val uri = FileProvider.getUriForFile(
                    this@AppDetailActivity,
                    BuildConfig.APPLICATION_ID + ".fileprovider",
                    iconFile
                  )
                  if (ClipboardUtils.put(
                      this@AppDetailActivity,
                      ClipData.newUri(contentResolver, Constants.TEMP_ICON, uri)
                    )
                  ) {
                    Toasty.showShort(this@AppDetailActivity, R.string.toast_copied_to_clipboard)
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
              text = packageName
              setLongClickCopiedToClipboard(text)
            }
            versionInfoView.apply {
              text = PackageUtils.getVersionString(packageInfo)
              setLongClickCopiedToClipboard(text)
            }
          }

          val extraInfo = SpannableStringBuilder()
          val file = File(packageInfo.applicationInfo.sourceDir)
          val overlay = Refine.unsafeCast<PackageInfoHidden>(packageInfo).isOverlayPackage
          var abiSet = PackageUtils.getAbiSet(
            file,
            packageInfo.applicationInfo,
            isApk = false,
            overlay = overlay,
            ignoreArch = true
          ).toSet()
          val abi = PackageUtils.getAbi(packageInfo, isApk = false, abiSet = abiSet)
          abiSet = abiSet.sortedByDescending { it == abi }.toSet()

          extraInfo.apply {
            if (abi >= Constants.MULTI_ARCH) {
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
            appendLine()
          }
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
              PackageUtils.getAbiString(
                this@AppDetailActivity,
                it,
                false
              ).let { str ->
                spanString = SpannableString("  $str")
              }
              PackageUtils.getAbiBadgeResource(it)
                .getDrawable(this@AppDetailActivity)?.mutate()?.let { drawable ->
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
          detailsTitle.extraInfoView.text = extraInfo

          if (featureListView == null) {
            extraBean?.let {
              if (it.isSplitApk) {
                initFeatureListView()
                featureAdapter.addData(
                  FeatureItem(R.drawable.ic_aab) {
                    AppBundleBottomSheetDialogFragment().apply {
                      arguments = bundleOf(
                        EXTRA_PACKAGE_NAME to pkgName
                      )
                      show(supportFragmentManager, tag)
                    }
                  }
                )
              }
              if (it.isKotlinUsed == null) {
                lifecycleScope.launch(Dispatchers.IO) {
                  val isKotlinUsed = PackageUtils.isKotlinUsed(packageInfo)
                  Repositories.lcRepository.updateKotlinUsage(packageName, isKotlinUsed)

                  if (isKotlinUsed) {
                    withContext(Dispatchers.Main) {
                      showKotlinUsedLabel()
                    }
                  }
                }
              } else if (it.isKotlinUsed) {
                showKotlinUsedLabel()
              }

              initMoreFeatures(packageInfo)
            }
          }
        } catch (e: Exception) {
          Timber.e(e)
          Toasty.showLong(this@AppDetailActivity, e.toString())
          finish()
        }

        val toolbarItems = mutableListOf(
          AppDetailToolbarItem(
            R.drawable.ic_lib_sort,
            R.string.menu_sort
          ) {
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
        if (GlobalValues.debugMode) {
          toolbarItems.add(
            AppDetailToolbarItem(
              R.drawable.ic_processes,
              R.string.menu_sort
            ) {
              Toasty.showLong(this@AppDetailActivity, viewModel.processesSet.toString())
            }
          )
        }
        if (extraBean?.variant == Constants.VARIANT_HAP) {
          toolbarItems.add(
            AppDetailToolbarItem(
              R.drawable.ic_harmonyos_logo,
              R.string.ability
            ) {
              isHarmonyMode = !isHarmonyMode
              initView()
            }
          )
        }
        toolbarAdapter.setList(toolbarItems)
        rvToolbar.apply {
          adapter = toolbarAdapter
          layoutManager =
            LinearLayoutManager(this@AppDetailActivity, RecyclerView.HORIZONTAL, false)
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
            val libs = PackageUtils.getStaticLibs(PackageUtils.getPackageInfo(packageName))
            if (libs.isNotEmpty()) {
              withContext(Dispatchers.Main) {
                typeList.add(1, STATIC)
                tabTitles.add(1, getText(R.string.ref_category_static))
                binding.tabLayout.addTab(
                  binding.tabLayout.newTab()
                    .also { it.text = getText(R.string.ref_category_static) },
                  1
                )
                resolveReferenceExtras()
              }
            }
          } catch (e: PackageManager.NameNotFoundException) {
            Timber.e(e)
          }
        }
      }

      binding.viewpager.apply {
        adapter = object : FragmentStateAdapter(this@AppDetailActivity) {
          override fun getItemCount(): Int {
            return typeList.size
          }

          override fun createFragment(position: Int): Fragment {
            return when (val type = typeList[position]) {
              NATIVE -> NativeAnalysisFragment.newInstance(packageName)
              STATIC -> StaticAnalysisFragment.newInstance(packageName)
              PERMISSION -> PermissionAnalysisFragment.newInstance(packageName)
              METADATA -> MetaDataAnalysisFragment.newInstance(packageName)
              DEX -> DexAnalysisFragment.newInstance(packageName)
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

      if (!isHarmonyMode) {
        viewModel.initComponentsData(packageName)
      } else {
        viewModel.initAbilities(packageName)
      }
    } ?: finish()
  }

  private fun resolveReferenceExtras() {
    if (pkgName == null || refName == null || refType == ALL) {
      return
    }
    navigateToReferenceComponentPosition(pkgName!!, refName!!)
  }

  private fun navigateToReferenceComponentPosition(packageName: String, refName: String) {
    val position = typeList.indexOf(refType)
    binding.viewpager.currentItem = position
    binding.tabLayout.post {
      val targetTab = binding.tabLayout.getTabAt(position)
      if (targetTab?.isSelected == false) {
        targetTab.select()
      }
    }

    val componentName = if (refType == PERMISSION) {
      refName
    } else {
      refName.removePrefix(packageName)
    }
    detailFragmentManager.navigateToComponent(refType, componentName)
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

  private fun showKotlinUsedLabel() {
    initFeatureListView()
    featureAdapter.addData(
      FeatureItem(R.drawable.ic_kotlin_logo) {
        MaterialAlertDialogBuilder(this)
          .setIcon(R.drawable.ic_kotlin_logo)
          .setTitle(R.string.kotlin_string)
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
    }
    binding.headerContentLayout.addView(featureListView)
    return true
  }

  private fun initMoreFeatures(packageInfo: PackageInfo) {
    lifecycleScope.launch(Dispatchers.IO) {
      PackageUtils.getAGPVersion(packageInfo)?.let {
        withContext(Dispatchers.Main) {
          initFeatureListView()
          featureAdapter.addData(
            FeatureItem(R.drawable.ic_gradle) {
              MaterialAlertDialogBuilder(this@AppDetailActivity)
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
              MaterialAlertDialogBuilder(this@AppDetailActivity)
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
              MaterialAlertDialogBuilder(this@AppDetailActivity)
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
              MaterialAlertDialogBuilder(this@AppDetailActivity)
                .setIcon(R.drawable.ic_pwa)
                .setTitle(R.string.pwa)
                .setMessage(R.string.pwa_details)
                .setPositiveButton(android.R.string.ok, null)
                .show()
            }
          )
        }
      }
    }
  }
}
