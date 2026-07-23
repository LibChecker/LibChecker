package com.absinthe.libchecker.di

import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.data.app.GlobalAppDetailSettingsRepository
import com.absinthe.libchecker.data.app.RemoteLibraryDetailRepository
import com.absinthe.libchecker.domain.app.detail.action.AllowFileUriExposureUseCase
import com.absinthe.libchecker.domain.app.detail.action.ExportAppPackageShareFileUseCase
import com.absinthe.libchecker.domain.app.detail.action.ExtractNativeLibraryUseCase
import com.absinthe.libchecker.domain.app.detail.action.GetAlternativeLaunchItemsUseCase
import com.absinthe.libchecker.domain.app.detail.action.GetAppInfoActionsUseCase
import com.absinthe.libchecker.domain.app.detail.action.GetAppInfoPrimaryActionsUseCase
import com.absinthe.libchecker.domain.app.detail.action.GetAppInstallSourceDetailsUseCase
import com.absinthe.libchecker.domain.app.detail.action.GetAppManifestPropertiesUseCase
import com.absinthe.libchecker.domain.app.detail.action.GetElfDetailUseCase
import com.absinthe.libchecker.domain.app.detail.action.GetLibraryDetailDialogDataUseCase
import com.absinthe.libchecker.domain.app.detail.action.GetLibraryDetailUseCase
import com.absinthe.libchecker.domain.app.detail.action.GetOverlayDetailUseCase
import com.absinthe.libchecker.domain.app.detail.action.GetPermissionDetailUseCase
import com.absinthe.libchecker.domain.app.detail.action.GetPermissionProvidersUseCase
import com.absinthe.libchecker.domain.app.detail.action.GetXposedModuleInfoUseCase
import com.absinthe.libchecker.domain.app.detail.action.PrepareAppPackageShareActionUseCase
import com.absinthe.libchecker.domain.app.detail.action.PrepareAppPackageShareFileUseCase
import com.absinthe.libchecker.domain.app.detail.content.BuildNativeLibraryItemDisplayDataUseCase
import com.absinthe.libchecker.domain.app.detail.content.GetAppBundleItemsUseCase
import com.absinthe.libchecker.domain.app.detail.content.GetAppDetailAbilityChipsUseCase
import com.absinthe.libchecker.domain.app.detail.content.GetAppDetailComponentChipsUseCase
import com.absinthe.libchecker.domain.app.detail.content.GetAppDetailComponentsUseCase
import com.absinthe.libchecker.domain.app.detail.content.GetAppDetailDexChipsUseCase
import com.absinthe.libchecker.domain.app.detail.content.GetAppDetailMetadataChipsUseCase
import com.absinthe.libchecker.domain.app.detail.content.GetAppDetailNativeLibrariesUseCase
import com.absinthe.libchecker.domain.app.detail.content.GetAppDetailPermissionChipsUseCase
import com.absinthe.libchecker.domain.app.detail.content.GetAppDetailSignatureChipsUseCase
import com.absinthe.libchecker.domain.app.detail.content.GetAppDetailStaticLibraryChipsUseCase
import com.absinthe.libchecker.domain.app.detail.content.GetAppDetailStaticLibraryTabItemsUseCase
import com.absinthe.libchecker.domain.app.detail.feature.BuildAppDetailFeatureItemUseCase
import com.absinthe.libchecker.domain.app.detail.feature.GetAppDetailFeaturesUseCase
import com.absinthe.libchecker.domain.app.detail.packageinfo.GetAppDetailPackageSizeUseCase
import com.absinthe.libchecker.domain.app.detail.packageinfo.GetAppDetailPackageUseCase
import com.absinthe.libchecker.domain.app.detail.presentation.DetailActionLoader
import com.absinthe.libchecker.domain.app.detail.presentation.DetailFilterController
import com.absinthe.libchecker.domain.app.detail.presentation.DetailPackageLoader
import com.absinthe.libchecker.domain.app.detail.presentation.DetailPresentationLoader
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel
import com.absinthe.libchecker.domain.app.detail.presentation.content.DetailContentLoader
import com.absinthe.libchecker.domain.app.detail.resource.ResolveAppResourceValueUseCase
import com.absinthe.libchecker.domain.app.detail.statistics.AnalyzeAppStatisticRulesUseCase
import com.absinthe.libchecker.domain.app.packageinfo.GetArchivePackageInfoUseCase
import com.absinthe.libchecker.domain.app.packageinfo.GetInstalledAppComparisonPackageUseCase
import com.absinthe.libchecker.domain.app.packageinfo.PrepareApkAnalysisPackageUseCase
import com.absinthe.libchecker.domain.app.repository.AppDetailSettingsRepository
import com.absinthe.libchecker.domain.app.repository.LibraryDetailRepository
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appDetailModule = module {
  single<AppDetailSettingsRepository> { GlobalAppDetailSettingsRepository() }
  single<LibraryDetailRepository> { RemoteLibraryDetailRepository }
  single { AllowFileUriExposureUseCase() }
  factory { GetAlternativeLaunchItemsUseCase(androidContext().packageManager, get()) }
  factory { GetAppBundleItemsUseCase() }
  factory { GetAppDetailAbilityChipsUseCase(androidContext()) }
  factory { GetAppDetailComponentsUseCase(get()) }
  factory { GetAppDetailComponentChipsUseCase(get()) }
  factory { GetAppDetailDexChipsUseCase() }
  factory { GetAppDetailFeaturesUseCase(get(), get()) }
  factory {
    GetAppInfoActionsUseCase(BuildConfig.APPLICATION_ID, androidContext().packageManager, get(), get())
  }
  factory { GetAppInstallSourceDetailsUseCase(androidContext(), get()) }
  factory { GetAppInfoPrimaryActionsUseCase(BuildConfig.APPLICATION_ID) }
  factory { GetAppDetailMetadataChipsUseCase(androidContext().packageManager) }
  factory { GetAppDetailNativeLibrariesUseCase(get(), get()) }
  factory { GetAppDetailStaticLibraryChipsUseCase() }
  factory { GetAppDetailPackageUseCase(get()) }
  factory { GetAppDetailPackageSizeUseCase(get()) }
  factory { ExtractNativeLibraryUseCase(androidContext(), BuildConfig.APPLICATION_ID) }
  factory { GetAppDetailPermissionChipsUseCase() }
  factory { GetAppDetailSignatureChipsUseCase(androidContext(), get()) }
  factory { PrepareAppPackageShareFileUseCase(androidContext(), BuildConfig.APPLICATION_ID, get()) }
  factory { PrepareAppPackageShareActionUseCase(get()) }
  factory { ExportAppPackageShareFileUseCase(androidContext().contentResolver) }
  factory { ResolveAppResourceValueUseCase(androidContext().packageManager) }
  factory { BuildAppDetailFeatureItemUseCase() }
  factory { BuildNativeLibraryItemDisplayDataUseCase(androidContext()) }
  factory { GetPermissionProvidersUseCase(get()) }
  factory { GetAppManifestPropertiesUseCase(androidContext().packageManager) }
  factory { GetArchivePackageInfoUseCase() }
  factory { PrepareApkAnalysisPackageUseCase(androidContext().contentResolver, get()) }
  factory { GetElfDetailUseCase(get()) }
  factory { GetInstalledAppComparisonPackageUseCase(get()) }
  factory { GetLibraryDetailUseCase(get()) }
  factory { GetLibraryDetailDialogDataUseCase(get()) }
  factory { GetOverlayDetailUseCase(androidContext(), get()) }
  factory { GetPermissionDetailUseCase(androidContext().packageManager, get()) }
  factory { GetXposedModuleInfoUseCase(androidContext().packageManager, get()) }
  factory { GetAppDetailStaticLibraryTabItemsUseCase(get(), get()) }
  factory { AnalyzeAppStatisticRulesUseCase(get(), get()) }
  factory {
    DetailActionLoader(
      context = androidContext(),
      installedAppRepository = get(),
      getAlternativeLaunchItemsUseCase = get(),
      getAppBundleItemsUseCase = get(),
      getAppInfoActionsUseCase = get(),
      getAppInfoPrimaryActionsUseCase = get(),
      getAppInstallSourceDetailsUseCase = get(),
      getAppManifestPropertiesUseCase = get(),
      getElfDetailUseCase = get(),
      getLibraryDetailDialogDataUseCase = get(),
      getOverlayDetailUseCase = get(),
      getPermissionDetailUseCase = get(),
      getRelatedAppListItemUseCase = get(),
      getXposedModuleInfoUseCase = get(),
      extractNativeLibraryUseCase = get(),
      prepareAppPackageShareActionUseCase = get(),
      exportAppPackageShareFileUseCase = get()
    )
  }
  factory {
    DetailContentLoader(
      getAppDetailAbilityChipsUseCase = get(),
      getAppDetailComponentChipsUseCase = get(),
      getAppDetailDexChipsUseCase = get(),
      getAppDetailMetadataChipsUseCase = get(),
      getAppDetailNativeLibrariesUseCase = get(),
      getAppDetailPermissionChipsUseCase = get(),
      getAppDetailSignatureChipsUseCase = get(),
      getAppDetailStaticLibraryChipsUseCase = get(),
      getAppDetailStaticLibraryTabItemsUseCase = get(),
      appDetailSettingsRepository = get()
    )
  }
  factory {
    DetailFilterController(
      appDetailSettingsRepository = get()
    )
  }
  factory {
    DetailPresentationLoader(
      context = androidContext(),
      installedAppRepository = get(),
      getAppDetailPackageSize = get(),
      getAppDetailFeaturesUseCase = get(),
      buildAppDetailFeatureItemUseCase = get()
    )
  }
  factory {
    DetailPackageLoader(
      getAppDetailPackage = get(),
      prepareApkAnalysisPackageUseCase = get(),
      getInstalledAppComparisonPackageUseCase = get(),
      buildPackageComparisonSnapshotItemUseCase = get()
    )
  }
  viewModel {
    DetailViewModel(
      detailActionLoader = get(),
      detailContentLoader = get(),
      detailFilterController = get(),
      detailPresentationLoader = get(),
      detailPackageLoader = get(),
      analyzeAppStatisticRules = get()
    )
  }
}
