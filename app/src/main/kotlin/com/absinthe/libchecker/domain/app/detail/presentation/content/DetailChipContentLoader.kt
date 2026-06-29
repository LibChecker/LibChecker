package com.absinthe.libchecker.domain.app.detail.presentation.content

import com.absinthe.libchecker.domain.app.detail.content.GetAppDetailDexChipsUseCase
import com.absinthe.libchecker.domain.app.detail.content.GetAppDetailMetadataChipsUseCase
import com.absinthe.libchecker.domain.app.detail.content.GetAppDetailSignatureChipsUseCase
import com.absinthe.libchecker.domain.app.detail.content.GetAppDetailStaticLibraryChipsUseCase
import com.absinthe.libchecker.domain.app.detail.presentation.DetailContentState
import com.absinthe.libchecker.domain.app.detail.presentation.DetailLoadJobsState
import com.absinthe.libchecker.domain.app.detail.presentation.DetailPackageState
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
