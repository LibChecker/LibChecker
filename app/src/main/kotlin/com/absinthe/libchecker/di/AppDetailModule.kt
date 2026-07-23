package com.absinthe.libchecker.di

import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.data.app.GlobalAppDetailSettingsRepository
import com.absinthe.libchecker.data.app.RemoteLibraryDetailRepository
import com.absinthe.libchecker.domain.app.detail.action.AllowFileUriExposureUseCase
import com.absinthe.libchecker.domain.app.detail.action.DetailAppInfoResolver
import com.absinthe.libchecker.domain.app.detail.action.DetailItemResolver
import com.absinthe.libchecker.domain.app.detail.action.ExportAppPackageShareFileUseCase
import com.absinthe.libchecker.domain.app.detail.action.ExtractNativeLibraryUseCase
import com.absinthe.libchecker.domain.app.detail.action.PrepareAppPackageShareActionUseCase
import com.absinthe.libchecker.domain.app.detail.action.PrepareAppPackageShareFileUseCase
import com.absinthe.libchecker.domain.app.detail.content.DetailContentResolver
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
  factory { DetailAppInfoResolver(androidContext(), BuildConfig.APPLICATION_ID, get(), get(), get()) }
  factory { DetailItemResolver(androidContext().packageManager, get(), get()) }
  factory { DetailContentResolver(androidContext(), get()) }
  factory { GetAppDetailFeaturesUseCase(get(), get()) }
  factory { GetAppDetailPackageUseCase(get()) }
  factory { GetAppDetailPackageSizeUseCase() }
  factory { ExtractNativeLibraryUseCase(androidContext(), BuildConfig.APPLICATION_ID) }
  factory { PrepareAppPackageShareFileUseCase(androidContext(), BuildConfig.APPLICATION_ID, get()) }
  factory { PrepareAppPackageShareActionUseCase(get()) }
  factory { ExportAppPackageShareFileUseCase(androidContext().contentResolver) }
  factory { ResolveAppResourceValueUseCase(androidContext().packageManager) }
  factory { BuildAppDetailFeatureItemUseCase() }
  factory { GetArchivePackageInfoUseCase() }
  factory { PrepareApkAnalysisPackageUseCase(androidContext().contentResolver, get()) }
  factory { GetInstalledAppComparisonPackageUseCase(get()) }
  factory { AnalyzeAppStatisticRulesUseCase(get(), get()) }
  factory {
    DetailActionLoader(
      context = androidContext(),
      installedAppRepository = get(),
      appInfoResolver = get(),
      itemResolver = get(),
      extractNativeLibraryUseCase = get(),
      prepareAppPackageShareActionUseCase = get(),
      exportAppPackageShareFileUseCase = get()
    )
  }
  factory {
    DetailContentLoader(
      contentResolver = get(),
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
