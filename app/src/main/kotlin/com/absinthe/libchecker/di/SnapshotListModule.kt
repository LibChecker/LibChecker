package com.absinthe.libchecker.di

import com.absinthe.libchecker.domain.snapshot.list.presentation.SnapshotViewModel
import com.absinthe.libchecker.domain.snapshot.list.usecase.BuildSnapshotCapturePlanUseCase
import com.absinthe.libchecker.domain.snapshot.list.usecase.BuildSnapshotListUpdatePlanUseCase
import com.absinthe.libchecker.domain.snapshot.list.usecase.BuildSnapshotSystemPropDisplayDataUseCase
import com.absinthe.libchecker.domain.snapshot.list.usecase.DeleteSnapshotTimeStampUseCase
import com.absinthe.libchecker.domain.snapshot.list.usecase.GetApexPackageNamesUseCase
import com.absinthe.libchecker.domain.snapshot.list.usecase.GetSnapshotPackageIconSourcesUseCase
import com.absinthe.libchecker.domain.snapshot.list.usecase.GetSnapshotSystemPropDiffsUseCase
import com.absinthe.libchecker.domain.snapshot.list.usecase.UpdateSnapshotDiffItemsUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val snapshotListModule = module {
  factory { GetApexPackageNamesUseCase(get()) }
  factory { BuildSnapshotCapturePlanUseCase(get()) }
  factory { BuildSnapshotListUpdatePlanUseCase(get(), get(), get()) }
  factory { GetSnapshotPackageIconSourcesUseCase(get()) }
  factory { GetSnapshotSystemPropDiffsUseCase(get()) }
  factory { BuildSnapshotSystemPropDisplayDataUseCase(androidContext(), get()) }
  factory { DeleteSnapshotTimeStampUseCase(get(), get()) }
  factory { UpdateSnapshotDiffItemsUseCase() }

  viewModel {
    SnapshotViewModel(
      repository = get(),
      appListRepository = get(),
      compareSnapshotDiffs = get(),
      compareSnapshotItemWithInstalledApp = get(),
      getSnapshotDashboardCount = get(),
      snapshotDetailSectionBuilder = get(),
      snapshotLibrary = get(),
      buildSnapshotCapturePlanUseCase = get(),
      getSnapshotPackageIconSourcesUseCase = get(),
      buildSnapshotListUpdatePlanUseCase = get(),
      buildSnapshotSystemPropDisplayDataUseCase = get(),
      buildSnapshotTimeNodeItemsUseCase = get(),
      deleteSnapshotTimeStampUseCase = get(),
      formatSnapshotTimestampUseCase = get(),
      snapshotSelectionUseCase = get(),
      snapshotSettingsRepository = get(),
      updateSnapshotAutoRemoveThresholdUseCase = get(),
      updateSnapshotDiffItemsUseCase = get(),
      snapshotTrackChangeRepository = get()
    )
  }
}
