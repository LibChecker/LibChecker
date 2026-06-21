package com.absinthe.libchecker.di

import com.absinthe.libchecker.data.app.AndroidAppListExportMetadata
import com.absinthe.libchecker.data.app.AndroidAppListItemFactory
import com.absinthe.libchecker.data.app.LocalAppListRepository
import com.absinthe.libchecker.data.app.LocalInstalledAppRepository
import com.absinthe.libchecker.data.snapshot.AndroidSnapshotItemFactory
import com.absinthe.libchecker.data.snapshot.LocalSnapshotRepository
import com.absinthe.libchecker.data.snapshot.ProtoSnapshotArchiveCodec
import com.absinthe.libchecker.database.LCRepository
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.domain.app.AppListExportMetadata
import com.absinthe.libchecker.domain.app.AppListItemFactory
import com.absinthe.libchecker.domain.app.AppListRepository
import com.absinthe.libchecker.domain.app.ExportAppListUseCase
import com.absinthe.libchecker.domain.app.InitializeAppListUseCase
import com.absinthe.libchecker.domain.app.InstalledAppRepository
import com.absinthe.libchecker.domain.app.SyncAppListChangesUseCase
import com.absinthe.libchecker.domain.snapshot.BuildSnapshotDetailItemsUseCase
import com.absinthe.libchecker.domain.snapshot.CompareSnapshotItemsUseCase
import com.absinthe.libchecker.domain.snapshot.CompareSnapshotListsUseCase
import com.absinthe.libchecker.domain.snapshot.SnapshotArchiveCodec
import com.absinthe.libchecker.domain.snapshot.SnapshotArchiveUseCase
import com.absinthe.libchecker.domain.snapshot.SnapshotItemFactory
import com.absinthe.libchecker.domain.snapshot.SnapshotLibraryUseCase
import com.absinthe.libchecker.domain.snapshot.SnapshotRepository
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
  single<AppListItemFactory> { AndroidAppListItemFactory(androidContext()) }
  single<AppListExportMetadata> { AndroidAppListExportMetadata(androidContext()) }
  single<SnapshotItemFactory> { AndroidSnapshotItemFactory() }
  single<SnapshotRepository> { LocalSnapshotRepository(get()) }
  single<SnapshotArchiveCodec> { ProtoSnapshotArchiveCodec() }
  factory { InitializeAppListUseCase(get(), get(), get()) }
  factory { SyncAppListChangesUseCase(get(), get(), get()) }
  factory { ComputeLibReferenceUseCase(get()) }
  factory { ExportAppListUseCase(get(), get()) }
  factory { CompareSnapshotItemsUseCase() }
  factory { CompareSnapshotListsUseCase(get()) }
  factory { BuildSnapshotDetailItemsUseCase() }
  factory { SnapshotArchiveUseCase(get(), get()) }
  factory { SnapshotLibraryUseCase(get()) }

  viewModel { ChartViewModel(get()) }
  viewModel { DetailViewModel(get()) }
  viewModel { HomeViewModel(get(), get(), get(), get(), get(), get()) }
  viewModel { LibReferenceViewModel(get()) }
  viewModel { SnapshotViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
  viewModel { TrackViewModel(get()) }
}
