package com.absinthe.libchecker.di

import com.absinthe.libchecker.data.app.AndroidAppListItemFactory
import com.absinthe.libchecker.data.app.LocalAppListRepository
import com.absinthe.libchecker.data.app.LocalInstalledAppRepository
import com.absinthe.libchecker.domain.app.AppListItemFactory
import com.absinthe.libchecker.domain.app.AppListRepository
import com.absinthe.libchecker.domain.app.InitializeAppListUseCase
import com.absinthe.libchecker.domain.app.InstalledAppRepository
import com.absinthe.libchecker.features.home.HomeViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
  single<InstalledAppRepository> { LocalInstalledAppRepository }
  single<AppListRepository> { LocalAppListRepository }
  single<AppListItemFactory> { AndroidAppListItemFactory(androidContext()) }
  factory { InitializeAppListUseCase(get(), get(), get()) }

  viewModel { HomeViewModel(get(), get(), get(), get()) }
}
