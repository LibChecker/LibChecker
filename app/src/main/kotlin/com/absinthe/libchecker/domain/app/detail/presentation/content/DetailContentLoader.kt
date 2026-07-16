package com.absinthe.libchecker.domain.app.detail.presentation.content

import com.absinthe.libchecker.domain.app.detail.content.GetAppDetailAbilityChipsUseCase
import com.absinthe.libchecker.domain.app.detail.content.GetAppDetailStaticLibraryTabItemsUseCase
import com.absinthe.libchecker.domain.app.detail.model.LibStringItemChip
import com.absinthe.libchecker.domain.app.detail.presentation.DetailContentState
import com.absinthe.libchecker.domain.app.detail.presentation.DetailFeatureState
import com.absinthe.libchecker.domain.app.detail.presentation.DetailLoadJobsState
import com.absinthe.libchecker.domain.app.detail.presentation.DetailPackageState
import com.absinthe.libchecker.domain.app.detail.ui.MODE_SORT_BY_SIZE
import com.absinthe.libchecker.domain.app.repository.AppDetailSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class DetailContentLoader(
  private val getAppDetailAbilityChipsUseCase: GetAppDetailAbilityChipsUseCase,
  private val getAppDetailStaticLibraryTabItemsUseCase: GetAppDetailStaticLibraryTabItemsUseCase,
  private val detailChipContentLoader: DetailChipContentLoader,
  private val detailComponentContentLoader: DetailComponentContentLoader,
  private val detailNativeLibContentLoader: DetailNativeLibContentLoader,
  private val detailPermissionContentLoader: DetailPermissionContentLoader,
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
    detailChipContentLoader.initStaticData(
      scope = scope,
      contentState = contentState,
      loadJobsState = loadJobsState,
      packageState = packageState,
      sortBySizeMode = isSortBySizeMode()
    )
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
    detailChipContentLoader.initMetaDataData(
      scope = scope,
      contentState = contentState,
      loadJobsState = loadJobsState,
      packageState = packageState
    )
  }

  fun initPermissionData(
    scope: CoroutineScope,
    featureState: DetailFeatureState,
    packageState: DetailPackageState
  ) {
    detailPermissionContentLoader.initPermissionData(
      scope = scope,
      contentState = contentState,
      featureState = featureState,
      loadJobsState = loadJobsState,
      packageState = packageState
    )
  }

  fun initDexData(
    scope: CoroutineScope,
    packageState: DetailPackageState
  ) {
    detailChipContentLoader.initDexData(
      scope = scope,
      contentState = contentState,
      loadJobsState = loadJobsState,
      packageState = packageState,
      sortBySizeMode = isSortBySizeMode()
    )
  }

  fun cancelInitDexDataJob() {
    loadJobsState.cancel(DetailLoadJobsState.Key.DEX)
  }

  fun initSignatures(
    scope: CoroutineScope,
    packageState: DetailPackageState
  ) {
    detailChipContentLoader.initSignatures(
      scope = scope,
      contentState = contentState,
      loadJobsState = loadJobsState,
      packageState = packageState
    )
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
