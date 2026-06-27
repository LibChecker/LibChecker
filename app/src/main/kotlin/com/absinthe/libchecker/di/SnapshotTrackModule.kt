package com.absinthe.libchecker.di

import com.absinthe.libchecker.data.snapshot.GlobalSnapshotTrackChangeRepository
import com.absinthe.libchecker.domain.snapshot.track.presentation.TrackViewModel
import com.absinthe.libchecker.domain.snapshot.track.repository.SnapshotTrackChangeRepository
import com.absinthe.libchecker.domain.snapshot.track.usecase.CompareTrackedSnapshotListsUseCase
import com.absinthe.libchecker.domain.snapshot.track.usecase.GetTrackListItemsUseCase
import com.absinthe.libchecker.domain.snapshot.track.usecase.SetPackageTrackedUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val snapshotTrackModule = module {
  single<SnapshotTrackChangeRepository> { GlobalSnapshotTrackChangeRepository() }

  factory { CompareTrackedSnapshotListsUseCase(get(), get()) }
  factory { GetTrackListItemsUseCase(androidContext().packageManager, get(), get()) }
  factory { SetPackageTrackedUseCase(get(), get()) }

  viewModel { TrackViewModel(get(), get()) }
}
