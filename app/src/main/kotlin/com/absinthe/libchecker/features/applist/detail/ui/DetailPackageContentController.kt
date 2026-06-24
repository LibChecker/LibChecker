package com.absinthe.libchecker.features.applist.detail.ui

import android.content.pm.PackageInfo
import com.absinthe.libchecker.database.entity.Features
import com.absinthe.libchecker.domain.app.VersionedFeature
import com.absinthe.libchecker.features.applist.detail.DetailViewModel
import com.absinthe.libchecker.features.applist.detail.bean.DetailExtraBean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetailPackageContentController(
  private val viewModel: DetailViewModel,
  private val coroutineScope: CoroutineScope,
  private val tabSpecBuilder: DetailTabSpecBuilder,
  private val tabController: DetailTabController,
  private val featureListController: DetailFeatureListController,
  private val currentUiGeneration: () -> Int,
  private val staticLibraryTitle: () -> CharSequence,
  private val onStaticLibsAvailable: () -> Unit,
  private val onPostPackageInfoAvailable: () -> Unit
) {

  fun bind(
    packageInfo: PackageInfo,
    extraBean: DetailExtraBean?,
    packageName: String,
    isHarmonyMode: Boolean,
    uiGeneration: Int
  ) {
    val tabSpec = tabSpecBuilder.build(
      isHarmonyMode = isHarmonyMode,
      isApkPreview = viewModel.isApkPreview
    )
    tabController.setup(
      packageName = packageName,
      isHarmonyMode = isHarmonyMode,
      tabSpec = tabSpec
    )

    insertStaticLibraryTabIfAvailable(packageInfo, packageName, uiGeneration)
    initFeatureItems(packageInfo, extraBean)
    initAnalysisContent(packageName, isHarmonyMode)
    schedulePostPackageInfoAvailable()
  }

  private fun insertStaticLibraryTabIfAvailable(
    packageInfo: PackageInfo,
    packageName: String,
    uiGeneration: Int
  ) {
    coroutineScope.launch {
      if (viewModel.shouldShowStaticLibraryTab(packageInfo, packageName)) {
        if (uiGeneration != currentUiGeneration()) {
          return@launch
        }
        if (tabController.insertStaticLibraryTab(staticLibraryTitle())) {
          onStaticLibsAvailable()
        }
      }
    }
  }

  private fun initFeatureItems(packageInfo: PackageInfo, extraBean: DetailExtraBean?) {
    if (featureListController.isInitialized) {
      return
    }

    if (viewModel.isApkPreview) {
      viewModel.emitFeature(VersionedFeature(Features.Ext.APPLICATION_PROP))
    } else {
      viewModel.initFeatures(packageInfo, extraBean?.features ?: -1)
    }
  }

  private fun initAnalysisContent(packageName: String, isHarmonyMode: Boolean) {
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
  }

  private fun schedulePostPackageInfoAvailable() {
    // Keep the legacy hook after the current UI-thread work queue.
    coroutineScope.launch(Dispatchers.IO) {
      withContext(Dispatchers.Main) {
        delay(1L)
        onPostPackageInfoAvailable()
      }
    }
  }
}
