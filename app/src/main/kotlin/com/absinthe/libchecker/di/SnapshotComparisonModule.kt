package com.absinthe.libchecker.di

import com.absinthe.libchecker.domain.snapshot.comparison.archive.BuildArchiveSnapshotItemUseCase
import com.absinthe.libchecker.domain.snapshot.comparison.presentation.SnapshotComparisonViewModel
import com.absinthe.libchecker.domain.snapshot.comparison.presentation.SnapshotComparisonWorkflow
import com.absinthe.libchecker.domain.snapshot.comparison.usecase.BuildPackageComparisonSnapshotItemUseCase
import com.absinthe.libchecker.domain.snapshot.comparison.usecase.CompareSnapshotDiffsUseCase
import com.absinthe.libchecker.domain.snapshot.comparison.usecase.CompareSnapshotItemWithInstalledAppUseCase
import com.absinthe.libchecker.domain.snapshot.comparison.usecase.CompareSnapshotItemsUseCase
import com.absinthe.libchecker.domain.snapshot.comparison.usecase.CompareSnapshotListsUseCase
import com.absinthe.libchecker.domain.snapshot.comparison.usecase.CompareSnapshotWithInstalledAppsUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val snapshotComparisonModule = module {
  factory { BuildArchiveSnapshotItemUseCase(androidContext()) }
  factory { SnapshotComparisonWorkflow(get(), get()) }
  factory { BuildPackageComparisonSnapshotItemUseCase(androidContext().packageManager) }
  factory { CompareSnapshotItemsUseCase() }
  factory { CompareSnapshotListsUseCase(get()) }
  factory { CompareSnapshotWithInstalledAppsUseCase(androidContext().packageManager, get(), get(), get(), get()) }
  factory { CompareSnapshotDiffsUseCase(get(), get(), get(), get()) }
  factory { CompareSnapshotItemWithInstalledAppUseCase(androidContext().packageManager, get(), get(), get(), get()) }

  viewModel {
    SnapshotComparisonViewModel(
      compareSnapshotDiffs = get(),
      snapshotDashboardCounter = get(),
      snapshotLibrary = get(),
      formatSnapshotTimestampUseCase = get(),
      comparisonWorkflow = get()
    )
  }
}
