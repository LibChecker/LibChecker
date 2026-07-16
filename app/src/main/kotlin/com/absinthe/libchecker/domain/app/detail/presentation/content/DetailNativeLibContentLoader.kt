package com.absinthe.libchecker.domain.app.detail.presentation.content

import com.absinthe.libchecker.database.entity.Features
import com.absinthe.libchecker.domain.app.detail.content.GetAppDetailNativeLibrariesUseCase
import com.absinthe.libchecker.domain.app.detail.presentation.DetailContentState
import com.absinthe.libchecker.domain.app.detail.presentation.DetailFeatureState
import com.absinthe.libchecker.domain.app.detail.presentation.DetailLoadJobsState
import com.absinthe.libchecker.domain.app.detail.presentation.DetailPackageState
import com.absinthe.libchecker.domain.app.detail.trace.TRACE_DETAIL_NATIVE_INIT_USE_CASE
import com.absinthe.libchecker.domain.app.detail.trace.traceDetailSection
import com.absinthe.libchecker.domain.app.model.VersionedFeature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DetailNativeLibContentLoader(
  private val getAppDetailNativeLibrariesUseCase: GetAppDetailNativeLibrariesUseCase
) {
  fun initSoAnalysisData(
    scope: CoroutineScope,
    contentState: DetailContentState,
    featureState: DetailFeatureState,
    loadJobsState: DetailLoadJobsState,
    packageState: DetailPackageState
  ) {
    loadJobsState.launchIfNeeded(
      key = DetailLoadJobsState.Key.NATIVE_LIBS,
      scope = scope,
      hasData = contentState.nativeLibItems.value != null
    ) {
      val abiBundle = featureState.abiBundleStateFlow.value
        ?: featureState.abiBundleStateFlow.filterNotNull().first()
      val nativeLibraries = traceDetailSection(TRACE_DETAIL_NATIVE_INIT_USE_CASE) {
        getAppDetailNativeLibrariesUseCase(
          packageInfo = packageState.packageInfo,
          apkPreviewInfo = packageState.apkPreviewInfo,
          isApk = packageState.isApk,
          isApkPreview = packageState.isApkPreview,
          abi = abiBundle.abi
        )
      }

      contentState.emitNativeLibTabs(nativeLibraries.itemsByAbi)

      if (nativeLibraries.selectedAbiSupports16KbPageSize) {
        featureState.emitFeature(VersionedFeature(Features.Ext.ELF_PAGE_SIZE_16KB))
      }
    }
  }

  fun loadSoAnalysisData(
    scope: CoroutineScope,
    contentState: DetailContentState,
    packageState: DetailPackageState,
    tab: String,
    sortBySizeMode: Boolean
  ) {
    contentState.nativeLibItemsFor(tab)?.let {
      scope.launch(Dispatchers.IO) {
        contentState.nativeLibItems.emit(
          getAppDetailNativeLibrariesUseCase.buildChipList(
            packageInfo = packageState.packageInfo,
            apkPreviewInfo = packageState.apkPreviewInfo,
            isApkPreview = packageState.isApkPreview,
            tab = tab,
            items = it,
            sortBySize = sortBySizeMode
          )
        )
      }
    }
  }
}
