package com.absinthe.libchecker.di

import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.data.app.AndroidAppListExportMetadata
import com.absinthe.libchecker.data.app.AndroidAppListItemFactory
import com.absinthe.libchecker.data.app.GlobalAppListSettingsRepository
import com.absinthe.libchecker.data.app.LocalAppListRepository
import com.absinthe.libchecker.data.app.LocalInstalledAppRepository
import com.absinthe.libchecker.data.app.WorkerFeatureInitializationRepository
import com.absinthe.libchecker.domain.app.AppListItemFactory
import com.absinthe.libchecker.domain.app.AppListRepository
import com.absinthe.libchecker.domain.app.AppListSettingsRepository
import com.absinthe.libchecker.domain.app.CheckRequiredPackageAvailabilityUseCase
import com.absinthe.libchecker.domain.app.ClearApkCacheUseCase
import com.absinthe.libchecker.domain.app.FeatureInitializationRepository
import com.absinthe.libchecker.domain.app.GetRandomAppIconUseCase
import com.absinthe.libchecker.domain.app.GetRelatedAppListItemUseCase
import com.absinthe.libchecker.domain.app.InstalledAppRepository
import com.absinthe.libchecker.domain.app.list.export.AppListExportMetadata
import com.absinthe.libchecker.domain.app.list.export.BuildAppExportNativeLibrariesUseCase
import com.absinthe.libchecker.domain.app.list.export.ExportAppListToUriUseCase
import com.absinthe.libchecker.domain.app.list.export.ExportAppListUseCase
import com.absinthe.libchecker.domain.app.list.export.ExportInstalledAppsToUriUseCase
import com.absinthe.libchecker.domain.app.list.usecase.AppListItemsEquivalenceUseCase
import com.absinthe.libchecker.domain.app.list.usecase.BuildAppListItemViewStatesUseCase
import com.absinthe.libchecker.domain.app.list.usecase.BuildAppListUpdatePlanUseCase
import com.absinthe.libchecker.domain.app.list.usecase.FilterAppListItemsUseCase
import com.absinthe.libchecker.domain.app.list.usecase.GetAppListContentUseCase
import com.absinthe.libchecker.domain.app.list.usecase.GetAppListPackageStatesUseCase
import com.absinthe.libchecker.domain.app.list.usecase.InitializeAppListUseCase
import com.absinthe.libchecker.domain.app.list.usecase.InitializePendingAppFeaturesUseCase
import com.absinthe.libchecker.domain.app.list.usecase.ObserveAppListLoadingUseCase
import com.absinthe.libchecker.domain.app.search.HandleAppListSearchCommandUseCase
import com.absinthe.libchecker.domain.app.sync.SyncAppListChangesUseCase
import com.absinthe.libchecker.domain.home.presentation.HomeViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appListModule = module {
  single<AppListSettingsRepository> { GlobalAppListSettingsRepository() }
  single<InstalledAppRepository> { LocalInstalledAppRepository }
  single<AppListRepository> { LocalAppListRepository }
  single<AppListItemFactory> { AndroidAppListItemFactory(androidContext()) }
  single<AppListExportMetadata> { AndroidAppListExportMetadata(androidContext()) }
  single<FeatureInitializationRepository> { WorkerFeatureInitializationRepository() }

  factory { InitializeAppListUseCase(get(), get(), get()) }
  factory { ObserveAppListLoadingUseCase(get()) }
  factory { SyncAppListChangesUseCase(get(), get(), get()) }
  factory { ExportAppListUseCase(get(), get()) }
  factory { ExportAppListToUriUseCase(androidContext().contentResolver, get()) }
  factory { FilterAppListItemsUseCase(get()) }
  factory { GetAppListContentUseCase(BuildConfig.APPLICATION_ID, get(), get(), get(), get()) }
  factory { HandleAppListSearchCommandUseCase(get()) }
  factory { GetAppListPackageStatesUseCase(get()) }
  factory { BuildAppExportNativeLibrariesUseCase() }
  factory {
    ExportInstalledAppsToUriUseCase(androidContext(), androidContext().contentResolver, get(), get())
  }
  factory { AppListItemsEquivalenceUseCase() }
  factory { BuildAppListItemViewStatesUseCase(androidContext(), get()) }
  factory { BuildAppListUpdatePlanUseCase() }
  factory { ClearApkCacheUseCase(androidContext()) }
  factory { CheckRequiredPackageAvailabilityUseCase(get()) }
  factory { GetRandomAppIconUseCase(androidContext().packageManager, get()) }
  factory { GetRelatedAppListItemUseCase(get(), get()) }
  factory { InitializePendingAppFeaturesUseCase(get(), get()) }

  viewModel {
    HomeViewModel(
      installedAppRepository = get(),
      appListRepository = get(),
      initializeAppListUseCase = get(),
      syncAppListChangesUseCase = get(),
      exportAppListToUriUseCase = get(),
      getAppListContentUseCase = get(),
      buildAppListItemViewStatesUseCase = get(),
      buildAppListUpdatePlanUseCase = get(),
      handleAppListSearchCommandUseCase = get(),
      appListSettingsRepository = get(),
      clearApkCacheUseCase = get(),
      appListItemsEquivalenceUseCase = get(),
      observeAppListLoadingUseCase = get()
    )
  }
}
