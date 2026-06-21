package com.absinthe.libchecker.di

import com.absinthe.libchecker.data.app.AndroidAppListExportMetadata
import com.absinthe.libchecker.data.app.AndroidAppListItemFactory
import com.absinthe.libchecker.data.app.LocalAppListRepository
import com.absinthe.libchecker.data.app.LocalInstalledAppRepository
import com.absinthe.libchecker.data.snapshot.AndroidSnapshotItemFactory
import com.absinthe.libchecker.database.LCRepository
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.domain.app.AppListExportMetadata
import com.absinthe.libchecker.domain.app.AppListItemFactory
import com.absinthe.libchecker.domain.app.AppListRepository
import com.absinthe.libchecker.domain.app.ExportAppListUseCase
import com.absinthe.libchecker.domain.app.InitializeAppListUseCase
import com.absinthe.libchecker.domain.app.InstalledAppRepository
import com.absinthe.libchecker.domain.app.SyncAppListChangesUseCase
import com.absinthe.libchecker.domain.snapshot.SnapshotItemFactory
import com.absinthe.libchecker.domain.statistics.ComputeLibReferenceUseCase
import com.absinthe.libchecker.features.home.HomeViewModel
import com.absinthe.libchecker.features.snapshot.SnapshotViewModel
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
  factory { InitializeAppListUseCase(get(), get(), get()) }
  factory { SyncAppListChangesUseCase(get(), get(), get()) }
  factory { ComputeLibReferenceUseCase(get()) }
  factory { ExportAppListUseCase(get(), get()) }

  viewModel { HomeViewModel(get(), get(), get(), get(), get(), get()) }
  viewModel { SnapshotViewModel(get(), get()) }
}
