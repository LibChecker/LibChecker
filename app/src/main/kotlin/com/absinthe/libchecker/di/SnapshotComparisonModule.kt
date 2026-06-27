package com.absinthe.libchecker.di

import com.absinthe.libchecker.domain.snapshot.comparison.archive.PrepareSnapshotComparisonArchivesUseCase
import com.absinthe.libchecker.domain.snapshot.comparison.presentation.SnapshotComparisonViewModel
import com.absinthe.libchecker.domain.snapshot.comparison.usecase.BuildPackageComparisonSnapshotItemUseCase
import com.absinthe.libchecker.domain.snapshot.comparison.usecase.BuildSnapshotComparisonPlanUseCase
import com.absinthe.libchecker.domain.snapshot.comparison.usecase.BuildSnapshotPairDiffUseCase
import com.absinthe.libchecker.domain.snapshot.comparison.usecase.CompareSnapshotDiffsUseCase
import com.absinthe.libchecker.domain.snapshot.comparison.usecase.CompareSnapshotItemWithInstalledAppUseCase
import com.absinthe.libchecker.domain.snapshot.comparison.usecase.CompareSnapshotItemsUseCase
import com.absinthe.libchecker.domain.snapshot.comparison.usecase.CompareSnapshotListsUseCase
import com.absinthe.libchecker.domain.snapshot.comparison.usecase.CompareSnapshotWithInstalledAppsUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val snapshotComparisonModule = module {
  factory { PrepareSnapshotComparisonArchivesUseCase(get()) }
  factory { BuildPackageComparisonSnapshotItemUseCase(androidContext().packageManager) }
  factory { BuildSnapshotComparisonPlanUseCase(get()) }
  factory { BuildSnapshotPairDiffUseCase() }
  factory { CompareSnapshotItemsUseCase() }
  factory { CompareSnapshotListsUseCase(get()) }
  factory { CompareSnapshotWithInstalledAppsUseCase(androidContext().packageManager, get(), get(), get(), get()) }
  factory { CompareSnapshotDiffsUseCase(get(), get(), get(), get()) }
  factory { CompareSnapshotItemWithInstalledAppUseCase(androidContext().packageManager, get(), get(), get(), get()) }

  viewModel {
    SnapshotComparisonViewModel(
      compareSnapshotDiffs = get(),
      getSnapshotDashboardCount = get(),
      snapshotLibrary = get(),
      buildSnapshotPairDiffUseCase = get(),
      buildSnapshotComparisonPlanUseCase = get(),
      formatSnapshotTimestampUseCase = get(),
      prepareSnapshotComparisonArchivesUseCase = get()
    )
  }
}
