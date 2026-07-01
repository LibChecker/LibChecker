package com.absinthe.libchecker.domain.app.detail.ui.controller

import android.content.pm.PackageInfo
import com.absinthe.libchecker.domain.app.detail.content.AppDetailAnalysisInitAction
import com.absinthe.libchecker.domain.app.detail.content.AppDetailFeatureInitAction
import com.absinthe.libchecker.domain.app.detail.content.BuildAppDetailContentInitPlanUseCase
import com.absinthe.libchecker.domain.app.detail.model.DetailExtraBean
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel
import com.absinthe.libchecker.domain.app.detail.ui.DetailTabSpec
import com.absinthe.libchecker.domain.app.detail.ui.DetailTabSpecBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DetailPackageContentController(
  private val viewModel: DetailViewModel,
  private val coroutineScope: CoroutineScope,
  private val buildAppDetailContentInitPlan: BuildAppDetailContentInitPlanUseCase,
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
    val contentInitPlan = buildAppDetailContentInitPlan(
      isHarmonyMode = isHarmonyMode,
      isApkPreview = viewModel.isApkPreview,
      featureMask = extraBean?.features ?: -1
    )
    tabController.setup(
      packageName = packageName,
      isHarmonyMode = isHarmonyMode,
      tabSpec = tabSpec
    )

    insertStaticLibraryTabIfAvailable(packageName, uiGeneration)
    initFeatureItems(packageInfo, contentInitPlan.featureAction, uiGeneration)
    initAnalysisContent(packageName, contentInitPlan.analysisAction)
    if (contentInitPlan.shouldInitPermissions) {
      // Detect Live Update notification
      viewModel.initPermissionData()
    }
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
    featureAction: AppDetailFeatureInitAction,
    uiGeneration: Int
  ) {
    when (featureAction) {
      is AppDetailFeatureInitAction.Emit -> viewModel.emitFeature(featureAction.feature)

      is AppDetailFeatureInitAction.LoadPackageFeatures -> {
        viewModel.setFeatureLoading(true)
        if (uiGeneration == currentUiGeneration()) {
          viewModel.initFeatures(packageInfo, featureAction.featureMask)
        }
      }
    }
  }

  private fun initAnalysisContent(
    packageName: String,
    analysisAction: AppDetailAnalysisInitAction
  ) {
    when (analysisAction) {
      AppDetailAnalysisInitAction.Abilities -> viewModel.initAbilities(packageName)
      AppDetailAnalysisInitAction.Components -> Unit
      AppDetailAnalysisInitAction.PreviewComponents -> viewModel.initComponentsDataInPreview()
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
