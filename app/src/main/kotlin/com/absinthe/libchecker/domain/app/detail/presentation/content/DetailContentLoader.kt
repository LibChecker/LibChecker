package com.absinthe.libchecker.domain.app.detail.presentation.content

import com.absinthe.libchecker.database.entity.Features
import com.absinthe.libchecker.domain.app.detail.content.GetAppDetailAbilityChipsUseCase
import com.absinthe.libchecker.domain.app.detail.content.GetAppDetailComponentChipsUseCase
import com.absinthe.libchecker.domain.app.detail.content.GetAppDetailDexChipsUseCase
import com.absinthe.libchecker.domain.app.detail.content.GetAppDetailMetadataChipsUseCase
import com.absinthe.libchecker.domain.app.detail.content.GetAppDetailNativeLibrariesUseCase
import com.absinthe.libchecker.domain.app.detail.content.GetAppDetailPermissionChipsUseCase
import com.absinthe.libchecker.domain.app.detail.content.GetAppDetailSignatureChipsUseCase
import com.absinthe.libchecker.domain.app.detail.content.GetAppDetailStaticLibraryChipsUseCase
import com.absinthe.libchecker.domain.app.detail.content.GetAppDetailStaticLibraryTabItemsUseCase
import com.absinthe.libchecker.domain.app.detail.model.LibStringItemChip
import com.absinthe.libchecker.domain.app.detail.presentation.DetailContentState
import com.absinthe.libchecker.domain.app.detail.presentation.DetailFeatureState
import com.absinthe.libchecker.domain.app.detail.presentation.DetailLoadJobsState
import com.absinthe.libchecker.domain.app.detail.presentation.DetailPackageState
import com.absinthe.libchecker.domain.app.detail.trace.TRACE_DETAIL_NATIVE_INIT_USE_CASE
import com.absinthe.libchecker.domain.app.detail.trace.traceDetailSection
import com.absinthe.libchecker.domain.app.detail.ui.MODE_SORT_BY_SIZE
import com.absinthe.libchecker.domain.app.model.VersionedFeature
import com.absinthe.libchecker.domain.app.repository.AppDetailSettingsRepository
import com.absinthe.libchecker.utils.UiUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

class DetailContentLoader(
  private val getAppDetailAbilityChipsUseCase: GetAppDetailAbilityChipsUseCase,
  private val getAppDetailComponentChipsUseCase: GetAppDetailComponentChipsUseCase,
  private val getAppDetailDexChipsUseCase: GetAppDetailDexChipsUseCase,
  private val getAppDetailMetadataChipsUseCase: GetAppDetailMetadataChipsUseCase,
  private val getAppDetailNativeLibrariesUseCase: GetAppDetailNativeLibrariesUseCase,
  private val getAppDetailPermissionChipsUseCase: GetAppDetailPermissionChipsUseCase,
  private val getAppDetailSignatureChipsUseCase: GetAppDetailSignatureChipsUseCase,
  private val getAppDetailStaticLibraryChipsUseCase: GetAppDetailStaticLibraryChipsUseCase,
  private val getAppDetailStaticLibraryTabItemsUseCase: GetAppDetailStaticLibraryTabItemsUseCase,
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
    packageState: DetailPackageState,
    tab: String
  ) {
    val sortBySizeMode = isSortBySizeMode()
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

  fun initStaticData(
    scope: CoroutineScope,
    packageState: DetailPackageState
  ) {
    val sortBySizeMode = isSortBySizeMode()
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

  suspend fun loadStaticLibraryTabItems(
    packageState: DetailPackageState,
    packageName: String
  ): List<LibStringItemChip> {
    return getAppDetailStaticLibraryTabItemsUseCase(
      packageInfo = packageState.packageInfo,
      packageName = packageName,
      sortBySizeMode = isSortBySizeMode()
    )
  }

  suspend fun emitStaticLibItems(items: List<LibStringItemChip>) {
    contentState.emitStaticLibItems(items)
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
    val sortBySizeMode = isSortBySizeMode()
    loadJobsState.launchIfNeeded(
      key = DetailLoadJobsState.Key.DEX,
      scope = scope,
      hasData = contentState.dexLibItems.value != null
    ) {
      contentState.dexLibItems.emit(
        getAppDetailDexChipsUseCase(
          packageInfo = packageState.packageInfo,
          sortBySizeMode = sortBySizeMode
        )
      )
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
        getAppDetailSignatureChipsUseCase(packageState.packageInfo, packageState.isApk)
      )
    }
  }

  fun initComponentsData(
    scope: CoroutineScope,
    packageState: DetailPackageState
  ) {
    loadJobsState.launchIfNeeded(
      key = DetailLoadJobsState.Key.COMPONENTS,
      scope = scope,
      hasData = contentState.hasComponentsData()
    ) {
      try {
        val components = getAppDetailComponentChipsUseCase(
          packageState.packageInfo,
          packageState.isApk
        )
        contentState.emitComponents(components) { UiUtils.getRandomColor() }
      } catch (e: Exception) {
        Timber.e(e)
      }
    }
  }

  fun initComponentsDataInPreview(
    scope: CoroutineScope,
    packageState: DetailPackageState
  ) = scope.launch(Dispatchers.IO) {
    val previewInfo = packageState.apkPreviewInfo ?: return@launch
    contentState.emitComponentItems(getAppDetailComponentChipsUseCase(previewInfo))
  }

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
