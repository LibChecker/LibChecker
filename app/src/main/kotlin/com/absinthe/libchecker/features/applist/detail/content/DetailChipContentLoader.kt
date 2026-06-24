package com.absinthe.libchecker.features.applist.detail.content

import com.absinthe.libchecker.domain.app.detail.GetAppDetailDexChipsUseCase
import com.absinthe.libchecker.domain.app.detail.GetAppDetailMetadataChipsUseCase
import com.absinthe.libchecker.domain.app.detail.GetAppDetailSignatureChipsUseCase
import com.absinthe.libchecker.domain.app.detail.GetAppDetailStaticLibraryChipsUseCase
import com.absinthe.libchecker.features.applist.detail.DetailContentState
import com.absinthe.libchecker.features.applist.detail.DetailLoadJobsState
import com.absinthe.libchecker.features.applist.detail.DetailPackageState
import kotlinx.coroutines.CoroutineScope

class DetailChipContentLoader(
  private val getAppDetailDexChipsUseCase: GetAppDetailDexChipsUseCase,
  private val getAppDetailMetadataChipsUseCase: GetAppDetailMetadataChipsUseCase,
  private val getAppDetailSignatureChipsUseCase: GetAppDetailSignatureChipsUseCase,
  private val getAppDetailStaticLibraryChipsUseCase: GetAppDetailStaticLibraryChipsUseCase
) {
  fun initStaticData(
    scope: CoroutineScope,
    contentState: DetailContentState,
    loadJobsState: DetailLoadJobsState,
    packageState: DetailPackageState,
    sortBySizeMode: Boolean
  ) {
    loadJobsState.launchIfNeeded(
      key = DetailLoadJobsState.Key.STATIC_LIBS,
      scope = scope,
      hasData = contentState.staticLibItems.value != null
    ) {
      contentState.staticLibItems.emit(
        getAppDetailStaticLibraryChipsUseCase(
          packageInfo = packageState.packageInfo,
          sortBySizeMode = sortBySizeMode
        )
      )
    }
  }

  fun initMetaDataData(
    scope: CoroutineScope,
    contentState: DetailContentState,
    loadJobsState: DetailLoadJobsState,
    packageState: DetailPackageState
  ) {
    loadJobsState.launchIfNeeded(
      key = DetailLoadJobsState.Key.METADATA,
      scope = scope,
      hasData = contentState.metaDataItems.value != null
    ) {
      contentState.metaDataItems.emit(
        getAppDetailMetadataChipsUseCase(
          packageInfo = packageState.packageInfo,
          apkPreviewInfo = packageState.apkPreviewInfo,
          isApkPreview = packageState.isApkPreview
        )
      )
    }
  }

  fun initDexData(
    scope: CoroutineScope,
    contentState: DetailContentState,
    loadJobsState: DetailLoadJobsState,
    packageState: DetailPackageState,
    sortBySizeMode: Boolean
  ) {
    loadJobsState.launchIfNeeded(
      key = DetailLoadJobsState.Key.DEX,
      scope = scope,
      hasData = contentState.dexLibItems.value != null
    ) {
      val list = getAppDetailDexChipsUseCase(
        packageInfo = packageState.packageInfo,
        sortBySizeMode = sortBySizeMode
      )
      contentState.dexLibItems.emit(list)
    }
  }

  fun initSignatures(
    scope: CoroutineScope,
    contentState: DetailContentState,
    loadJobsState: DetailLoadJobsState,
    packageState: DetailPackageState
  ) {
    loadJobsState.launchIfNeeded(
      key = DetailLoadJobsState.Key.SIGNATURES,
      scope = scope,
      hasData = contentState.signaturesLibItems.value != null
    ) {
      contentState.signaturesLibItems.emit(
        getAppDetailSignatureChipsUseCase(
          packageState.packageInfo,
          packageState.isApk
        )
      )
    }
  }
}
