package com.absinthe.libchecker.di

import com.absinthe.libchecker.data.snapshot.GlobalSnapshotTrackChangeRepository
import com.absinthe.libchecker.domain.snapshot.track.presentation.TrackViewModel
import com.absinthe.libchecker.domain.snapshot.track.presentation.TrackWorkflow
import com.absinthe.libchecker.domain.snapshot.track.repository.SnapshotTrackChangeRepository
import com.absinthe.libchecker.domain.snapshot.track.usecase.CompareTrackedSnapshotListsUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val snapshotTrackModule = module {
  single<SnapshotTrackChangeRepository> { GlobalSnapshotTrackChangeRepository() }

  factory { CompareTrackedSnapshotListsUseCase(get(), get()) }
  factory { TrackWorkflow(androidContext().packageManager, get(), get(), get()) }

  viewModel { TrackViewModel(get()) }
}
