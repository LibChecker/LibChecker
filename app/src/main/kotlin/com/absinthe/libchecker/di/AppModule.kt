package com.absinthe.libchecker.di

import com.absinthe.libchecker.data.home.LocalRecentVisitsRepository
import com.absinthe.libchecker.database.LCDao
import com.absinthe.libchecker.database.LCDatabase
import com.absinthe.libchecker.database.LCRepository
import com.absinthe.libchecker.domain.home.presentation.RecentVisitsViewModel
import com.absinthe.libchecker.domain.home.recent.RecentVisitsRepository
import com.absinthe.libchecker.utils.SPUtils
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
  single<LCDatabase> { LCDatabase.getDatabase() }
  single<LCDao> { get<LCDatabase>().lcDao() }
  single<LCRepository> { LCRepository(get()) }
  single<RecentVisitsRepository> { LocalRecentVisitsRepository { SPUtils.sp } }
  viewModel { RecentVisitsViewModel(androidContext(), get(), get(), get(), get()) }
}
