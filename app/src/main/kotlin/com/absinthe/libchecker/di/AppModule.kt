package com.absinthe.libchecker.di

import com.absinthe.libchecker.data.app.AndroidAppListExportMetadata
import com.absinthe.libchecker.data.app.AndroidAppListItemFactory
import com.absinthe.libchecker.data.app.LocalAppListRepository
import com.absinthe.libchecker.data.app.LocalInstalledAppRepository
import com.absinthe.libchecker.data.app.RemoteLibraryDetailRepository
import com.absinthe.libchecker.data.snapshot.AndroidSnapshotItemFactory
import com.absinthe.libchecker.data.snapshot.LocalSnapshotRepository
import com.absinthe.libchecker.data.snapshot.ProtoSnapshotArchiveCodec
import com.absinthe.libchecker.database.LCRepository
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.domain.app.AppListExportMetadata
import com.absinthe.libchecker.domain.app.AppListItemFactory
import com.absinthe.libchecker.domain.app.AppListRepository
import com.absinthe.libchecker.domain.app.BuildAppListItemViewStatesUseCase
import com.absinthe.libchecker.domain.app.ExportAppListUseCase
import com.absinthe.libchecker.domain.app.FilterAppListItemsUseCase
import com.absinthe.libchecker.domain.app.GetApkPreviewInfoUseCase
import com.absinthe.libchecker.domain.app.GetAppBundleItemsUseCase
import com.absinthe.libchecker.domain.app.GetAppDetailAbiUseCase
import com.absinthe.libchecker.domain.app.GetAppDetailComponentChipsUseCase
import com.absinthe.libchecker.domain.app.GetAppDetailComponentsUseCase
import com.absinthe.libchecker.domain.app.GetAppDetailFeaturesUseCase
import com.absinthe.libchecker.domain.app.GetAppDetailMetadataChipsUseCase
import com.absinthe.libchecker.domain.app.GetAppDetailNativeLibrariesUseCase
import com.absinthe.libchecker.domain.app.GetAppDetailPackageSizeUseCase
import com.absinthe.libchecker.domain.app.GetAppDetailPackageUseCase
import com.absinthe.libchecker.domain.app.GetAppDetailPermissionChipsUseCase
import com.absinthe.libchecker.domain.app.GetAppDetailSignatureChipsUseCase
import com.absinthe.libchecker.domain.app.GetAppDetailStaticLibraryChipsUseCase
import com.absinthe.libchecker.domain.app.GetAppListPackageStatesUseCase
import com.absinthe.libchecker.domain.app.GetAppManifestPropertiesUseCase
import com.absinthe.libchecker.domain.app.GetArchivePackageInfoUseCase
import com.absinthe.libchecker.domain.app.GetInstalledAppComparisonPackageUseCase
import com.absinthe.libchecker.domain.app.GetLibraryDetailUseCase
import com.absinthe.libchecker.domain.app.HasInstalledStaticLibrariesUseCase
import com.absinthe.libchecker.domain.app.InitializeAppListUseCase
import com.absinthe.libchecker.domain.app.InstalledAppRepository
import com.absinthe.libchecker.domain.app.LibraryDetailRepository
import com.absinthe.libchecker.domain.app.SortAppDetailItemsUseCase
import com.absinthe.libchecker.domain.app.SyncAppListChangesUseCase
import com.absinthe.libchecker.domain.snapshot.BuildArchiveSnapshotItemUseCase
import com.absinthe.libchecker.domain.snapshot.BuildPackageComparisonSnapshotItemUseCase
import com.absinthe.libchecker.domain.snapshot.BuildSnapshotComparisonListsUseCase
import com.absinthe.libchecker.domain.snapshot.BuildSnapshotDetailItemsUseCase
import com.absinthe.libchecker.domain.snapshot.BuildSnapshotPairDiffUseCase
import com.absinthe.libchecker.domain.snapshot.CompareSnapshotItemWithInstalledAppUseCase
import com.absinthe.libchecker.domain.snapshot.CompareSnapshotItemsUseCase
import com.absinthe.libchecker.domain.snapshot.CompareSnapshotListsUseCase
import com.absinthe.libchecker.domain.snapshot.CompareSnapshotWithInstalledAppsUseCase
import com.absinthe.libchecker.domain.snapshot.GetSnapshotDashboardCountUseCase
import com.absinthe.libchecker.domain.snapshot.SnapshotArchiveCodec
import com.absinthe.libchecker.domain.snapshot.SnapshotArchiveUseCase
import com.absinthe.libchecker.domain.snapshot.SnapshotItemFactory
import com.absinthe.libchecker.domain.snapshot.SnapshotLibraryUseCase
import com.absinthe.libchecker.domain.snapshot.SnapshotRepository
import com.absinthe.libchecker.domain.snapshot.UpdateSnapshotTopAppsUseCase
import com.absinthe.libchecker.domain.statistics.ComputeLibReferenceUseCase
import com.absinthe.libchecker.features.album.track.TrackViewModel
import com.absinthe.libchecker.features.applist.detail.DetailViewModel
import com.absinthe.libchecker.features.chart.ChartViewModel
import com.absinthe.libchecker.features.home.HomeViewModel
import com.absinthe.libchecker.features.snapshot.SnapshotViewModel
import com.absinthe.libchecker.features.statistics.LibReferenceViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
  single<LCRepository> { Repositories.lcRepository }
  single<InstalledAppRepository> { LocalInstalledAppRepository }
  single<AppListRepository> { LocalAppListRepository }
  single<LibraryDetailRepository> { RemoteLibraryDetailRepository }
  single<AppListItemFactory> { AndroidAppListItemFactory(androidContext()) }
  single<AppListExportMetadata> { AndroidAppListExportMetadata(androidContext()) }
  single<SnapshotItemFactory> { AndroidSnapshotItemFactory() }
  single<SnapshotRepository> { LocalSnapshotRepository(get()) }
  single<SnapshotArchiveCodec> { ProtoSnapshotArchiveCodec() }
  factory { InitializeAppListUseCase(get(), get(), get()) }
  factory { SyncAppListChangesUseCase(get(), get(), get()) }
  factory { ComputeLibReferenceUseCase(get()) }
  factory { ExportAppListUseCase(get(), get()) }
  factory { FilterAppListItemsUseCase(get()) }
  factory { GetAppBundleItemsUseCase() }
  factory { GetAppDetailAbiUseCase() }
  factory { GetAppDetailComponentsUseCase() }
  factory { GetAppDetailComponentChipsUseCase(get()) }
  factory { GetAppDetailFeaturesUseCase(get()) }
  factory { GetAppDetailMetadataChipsUseCase() }
  factory { GetAppDetailNativeLibrariesUseCase() }
  factory { GetAppDetailStaticLibraryChipsUseCase() }
  factory { GetAppDetailPackageUseCase(get()) }
  factory { GetAppDetailPackageSizeUseCase(get()) }
  factory { GetAppDetailPermissionChipsUseCase() }
  factory { GetAppDetailSignatureChipsUseCase(androidContext()) }
  factory { GetApkPreviewInfoUseCase() }
  factory { GetAppListPackageStatesUseCase(get()) }
  factory { BuildAppListItemViewStatesUseCase(androidContext(), get()) }
  factory { GetAppManifestPropertiesUseCase() }
  factory { GetArchivePackageInfoUseCase() }
  factory { GetInstalledAppComparisonPackageUseCase(get()) }
  factory { GetLibraryDetailUseCase(get()) }
  factory { HasInstalledStaticLibrariesUseCase(get()) }
  factory { SortAppDetailItemsUseCase() }
  factory { BuildArchiveSnapshotItemUseCase(androidContext()) }
  factory { BuildPackageComparisonSnapshotItemUseCase(androidContext().packageManager) }
  factory { BuildSnapshotComparisonListsUseCase(get()) }
  factory { BuildSnapshotPairDiffUseCase() }
  factory { CompareSnapshotItemsUseCase() }
  factory { CompareSnapshotListsUseCase(get()) }
  factory { CompareSnapshotWithInstalledAppsUseCase(get(), get(), get(), get()) }
  factory { CompareSnapshotItemWithInstalledAppUseCase(get(), get(), get(), get()) }
  factory { GetSnapshotDashboardCountUseCase(get(), get()) }
  factory { UpdateSnapshotTopAppsUseCase(get(), get()) }
  factory { BuildSnapshotDetailItemsUseCase() }
  factory { SnapshotArchiveUseCase(get(), get()) }
  factory { SnapshotLibraryUseCase(get()) }

  viewModel {
    ChartViewModel(
      appListRepository = get(),
      buildAppListItemViewStatesUseCase = get()
    )
  }
  viewModel {
    DetailViewModel(
      appListRepository = get(),
      getAppDetailPackage = get(),
      getAppBundleItemsUseCase = get(),
      getAppDetailAbiUseCase = get(),
      getAppDetailComponentChipsUseCase = get(),
      getAppDetailFeaturesUseCase = get(),
      getAppDetailMetadataChipsUseCase = get(),
      getAppDetailNativeLibrariesUseCase = get(),
      getAppDetailStaticLibraryChipsUseCase = get(),
      getAppDetailPackageSizeUseCase = get(),
      getApkPreviewInfoUseCase = get(),
      getAppDetailPermissionChipsUseCase = get(),
      getAppDetailSignatureChipsUseCase = get(),
      getAppManifestPropertiesUseCase = get(),
      getArchivePackageInfoUseCase = get(),
      getInstalledAppComparisonPackageUseCase = get(),
      hasInstalledStaticLibrariesUseCase = get(),
      getLibraryDetailUseCase = get(),
      sortAppDetailItemsUseCase = get(),
      buildPackageComparisonSnapshotItemUseCase = get()
    )
  }
  viewModel {
    HomeViewModel(
      installedAppRepository = get(),
      appListRepository = get(),
      initializeAppListUseCase = get(),
      syncAppListChangesUseCase = get(),
      computeLibReferenceUseCase = get(),
      exportAppListUseCase = get(),
      filterAppListItemsUseCase = get(),
      buildAppListItemViewStatesUseCase = get()
    )
  }
  viewModel {
    LibReferenceViewModel(
      appListRepository = get(),
      buildAppListItemViewStatesUseCase = get()
    )
  }
  viewModel { SnapshotViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
  viewModel { TrackViewModel(get()) }
}
