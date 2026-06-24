package com.absinthe.libchecker.features.applist.detail

import com.absinthe.libchecker.database.entity.Features
import com.absinthe.libchecker.domain.app.AppDetailSettingsRepository
import com.absinthe.libchecker.domain.app.GetAppDetailAbilityChipsUseCase
import com.absinthe.libchecker.domain.app.GetAppDetailDexChipsUseCase
import com.absinthe.libchecker.domain.app.GetAppDetailMetadataChipsUseCase
import com.absinthe.libchecker.domain.app.GetAppDetailPermissionChipsUseCase
import com.absinthe.libchecker.domain.app.GetAppDetailSignatureChipsUseCase
import com.absinthe.libchecker.domain.app.GetAppDetailStaticLibraryChipsUseCase
import com.absinthe.libchecker.domain.app.VersionedFeature
import com.absinthe.libchecker.features.applist.MODE_SORT_BY_SIZE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class DetailContentLoader(
  private val getAppDetailAbilityChipsUseCase: GetAppDetailAbilityChipsUseCase,
  private val getAppDetailDexChipsUseCase: GetAppDetailDexChipsUseCase,
  private val getAppDetailMetadataChipsUseCase: GetAppDetailMetadataChipsUseCase,
  private val getAppDetailPermissionChipsUseCase: GetAppDetailPermissionChipsUseCase,
  private val getAppDetailSignatureChipsUseCase: GetAppDetailSignatureChipsUseCase,
  private val getAppDetailStaticLibraryChipsUseCase: GetAppDetailStaticLibraryChipsUseCase,
  private val detailComponentContentLoader: DetailComponentContentLoader,
  private val detailNativeLibContentLoader: DetailNativeLibContentLoader,
  private val appDetailSettingsRepository: AppDetailSettingsRepository
) {
  val contentState = DetailContentState()
  private val loadJobsState = DetailLoadJobsState()

  private fun cancelAll() {
    loadJobsState.cancelAll()
  }

  fun reset() {
    cancelAll()
    contentState.reset()
  }

  fun initSoAnalysisData(
    scope: CoroutineScope,
    featureState: DetailFeatureState,
    packageState: DetailPackageState
  ) {
    detailNativeLibContentLoader.initSoAnalysisData(
      scope = scope,
      contentState = contentState,
      featureState = featureState,
      loadJobsState = loadJobsState,
      packageState = packageState
    )
  }

  fun loadSoAnalysisData(
    scope: CoroutineScope,
    packageState: DetailPackageState,
    tab: String
  ) {
    detailNativeLibContentLoader.loadSoAnalysisData(
      scope = scope,
      contentState = contentState,
      packageState = packageState,
      tab = tab,
      sortBySizeMode = isSortBySizeMode()
    )
  }

  fun initStaticData(
    scope: CoroutineScope,
    packageState: DetailPackageState
  ) {
    loadJobsState.launchIfNeeded(
      key = DetailLoadJobsState.Key.STATIC_LIBS,
      scope = scope,
      hasData = contentState.staticLibItems.value != null
    ) {
      contentState.staticLibItems.emit(
        getAppDetailStaticLibraryChipsUseCase(
          packageInfo = packageState.packageInfo,
          sortBySizeMode = isSortBySizeMode()
        )
      )
    }
  }

  fun initMetaDataData(
    scope: CoroutineScope,
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

  fun initPermissionData(
    scope: CoroutineScope,
    featureState: DetailFeatureState,
    packageState: DetailPackageState
  ) {
    loadJobsState.launchIfNeeded(
      key = DetailLoadJobsState.Key.PERMISSIONS,
      scope = scope,
      hasData = contentState.permissionsItems.value != null
    ) {
      val permissions = getAppDetailPermissionChipsUseCase(
        packageInfo = packageState.packageInfo,
        apkPreviewInfo = packageState.apkPreviewInfo,
        isApk = packageState.isApk,
        isApkPreview = packageState.isApkPreview
      )
      contentState.permissionsItems.emit(permissions.items)

      if (permissions.hasLiveUpdateNotification) {
        featureState.emitFeature(VersionedFeature(Features.LIVE_UPDATE_NOTIFICATION))
      }
    }
  }

  fun initDexData(
    scope: CoroutineScope,
    packageState: DetailPackageState
  ) {
    loadJobsState.launchIfNeeded(
      key = DetailLoadJobsState.Key.DEX,
      scope = scope,
      hasData = contentState.dexLibItems.value != null
    ) {
      val list = getAppDetailDexChipsUseCase(
        packageInfo = packageState.packageInfo,
        sortBySizeMode = isSortBySizeMode()
      )
      contentState.dexLibItems.emit(list)
    }
  }

  fun cancelInitDexDataJob() {
    loadJobsState.cancel(DetailLoadJobsState.Key.DEX)
  }

  fun initSignatures(
    scope: CoroutineScope,
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

  fun initComponentsData(
    scope: CoroutineScope,
    packageState: DetailPackageState
  ) {
    detailComponentContentLoader.initComponentsData(
      scope = scope,
      contentState = contentState,
      loadJobsState = loadJobsState,
      packageState = packageState
    )
  }

  fun initComponentsDataInPreview(
    scope: CoroutineScope,
    packageState: DetailPackageState
  ) = detailComponentContentLoader.initComponentsDataInPreview(
    scope = scope,
    contentState = contentState,
    packageState = packageState
  )

  fun initAbilities(
    scope: CoroutineScope,
    packageName: String
  ) = scope.launch(Dispatchers.IO) {
    contentState.resetAbilities()

    runCatching {
      getAppDetailAbilityChipsUseCase(packageName)
    }.onSuccess { abilityChips ->
      contentState.emitAbilities(abilityChips)
    }.onFailure {
      Timber.e(it)
    }
  }

  private fun isSortBySizeMode(): Boolean {
    return appDetailSettingsRepository.sortMode == MODE_SORT_BY_SIZE
  }
}
