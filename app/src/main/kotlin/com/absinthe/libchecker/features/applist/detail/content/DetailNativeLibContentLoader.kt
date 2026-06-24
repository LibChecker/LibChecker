package com.absinthe.libchecker.features.applist.detail.content

import com.absinthe.libchecker.database.entity.Features
import com.absinthe.libchecker.domain.app.GetAppDetailNativeLibrariesUseCase
import com.absinthe.libchecker.domain.app.VersionedFeature
import com.absinthe.libchecker.features.applist.detail.DetailContentState
import com.absinthe.libchecker.features.applist.detail.DetailFeatureState
import com.absinthe.libchecker.features.applist.detail.DetailLoadJobsState
import com.absinthe.libchecker.features.applist.detail.DetailPackageState
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
      val nativeLibraries = getAppDetailNativeLibrariesUseCase(
        packageInfo = packageState.packageInfo,
        apkPreviewInfo = packageState.apkPreviewInfo,
        isApk = packageState.isApk,
        isApkPreview = packageState.isApkPreview,
        abi = abiBundle.abi
      )

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
            items = it,
            sortBySize = sortBySizeMode
          )
        )
      }
    }
  }
}
