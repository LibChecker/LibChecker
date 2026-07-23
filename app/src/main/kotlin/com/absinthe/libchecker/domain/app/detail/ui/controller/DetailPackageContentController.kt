package com.absinthe.libchecker.domain.app.detail.ui.controller

import android.content.pm.PackageInfo
import com.absinthe.libchecker.database.entity.Features
import com.absinthe.libchecker.domain.app.detail.model.DetailExtraBean
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel
import com.absinthe.libchecker.domain.app.detail.ui.DetailTabSpecBuilder
import com.absinthe.libchecker.domain.app.model.VersionedFeature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DetailPackageContentController(
  private val viewModel: DetailViewModel,
  private val coroutineScope: CoroutineScope,
  private val tabSpecBuilder: DetailTabSpecBuilder,
  private val tabController: DetailTabController,
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

    insertStaticLibraryTabIfAvailable(packageName, uiGeneration)
    initFeatureItems(packageInfo, extraBean?.features ?: -1, uiGeneration)
    initAnalysisContent(packageName, isHarmonyMode)
    // Detect Live Update notification
    viewModel.initPermissionData()
    schedulePostPackageInfoAvailable()
  }

  private fun insertStaticLibraryTabIfAvailable(
    packageName: String,
    uiGeneration: Int
  ) {
    coroutineScope.launch {
      val staticLibItems = viewModel.loadStaticLibraryTabItems(packageName)
      if (staticLibItems.isEmpty() || uiGeneration != currentUiGeneration()) {
        return@launch
      }
      viewModel.emitStaticLibItems(staticLibItems)
      if (tabController.insertStaticLibraryTab(staticLibraryTitle())) {
        onStaticLibsAvailable()
      }
    }
  }

  private fun initFeatureItems(
    packageInfo: PackageInfo,
    featureMask: Int,
    uiGeneration: Int
  ) {
    if (viewModel.isApkPreview) {
      viewModel.emitFeature(VersionedFeature(Features.Ext.APPLICATION_PROP))
    } else {
      viewModel.setFeatureLoading(true)
      if (uiGeneration == currentUiGeneration()) {
        viewModel.initFeatures(packageInfo, featureMask)
      }
    }
  }

  private fun initAnalysisContent(
    packageName: String,
    isHarmonyMode: Boolean
  ) {
    when {
      isHarmonyMode -> viewModel.initAbilities(packageName)
      viewModel.isApkPreview -> viewModel.initComponentsDataInPreview()
    }
  }

  private fun schedulePostPackageInfoAvailable() {
    // Keep the legacy hook after the current UI-thread work queue.
    coroutineScope.launch {
      delay(1L)
      onPostPackageInfoAvailable()
    }
  }
}
