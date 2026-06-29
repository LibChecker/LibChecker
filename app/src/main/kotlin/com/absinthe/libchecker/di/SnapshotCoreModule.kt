package com.absinthe.libchecker.di

import com.absinthe.libchecker.data.snapshot.AndroidSnapshotItemFactory
import com.absinthe.libchecker.data.snapshot.GlobalSnapshotSelectionRepository
import com.absinthe.libchecker.data.snapshot.GlobalSnapshotSettingsRepository
import com.absinthe.libchecker.data.snapshot.LocalSnapshotRepository
import com.absinthe.libchecker.data.snapshot.OnceSnapshotCaptureStateRepository
import com.absinthe.libchecker.domain.snapshot.SnapshotItemFactory
import com.absinthe.libchecker.domain.snapshot.SnapshotRepository
import com.absinthe.libchecker.domain.snapshot.SnapshotSettingsRepository
import com.absinthe.libchecker.domain.snapshot.display.SnapshotDashboardCounter
import com.absinthe.libchecker.domain.snapshot.library.SnapshotLibrary
import com.absinthe.libchecker.domain.snapshot.list.capture.SnapshotCaptureStateRepository
import com.absinthe.libchecker.domain.snapshot.selection.SnapshotSelection
import com.absinthe.libchecker.domain.snapshot.selection.SnapshotSelectionRepository
import org.koin.dsl.module

val snapshotCoreModule = module {
  single<SnapshotItemFactory> { AndroidSnapshotItemFactory() }
  single<SnapshotCaptureStateRepository> { OnceSnapshotCaptureStateRepository() }
  single<SnapshotSelectionRepository> { GlobalSnapshotSelectionRepository() }
  single<SnapshotSettingsRepository> { GlobalSnapshotSettingsRepository() }
  single<SnapshotRepository> { LocalSnapshotRepository(get(), get()) }

  factory { SnapshotDashboardCounter(get(), get()) }
  factory { SnapshotLibrary(get()) }
  factory { SnapshotSelection(get()) }
}
