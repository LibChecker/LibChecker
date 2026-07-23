package com.absinthe.libchecker.di

import com.absinthe.libchecker.domain.snapshot.detail.usecase.BuildSnapshotTitleDisplayDataUseCase
import com.absinthe.libchecker.domain.snapshot.detail.usecase.SnapshotDetailSectionBuilder
import com.absinthe.libchecker.domain.snapshot.display.FormatSnapshotTimestampUseCase
import com.absinthe.libchecker.domain.snapshot.list.capture.CaptureInstalledSnapshotUseCase
import com.absinthe.libchecker.domain.snapshot.list.presentation.SnapshotListWorkflow
import com.absinthe.libchecker.domain.snapshot.list.presentation.SnapshotViewModel
import com.absinthe.libchecker.domain.snapshot.list.usecase.BuildSnapshotCapturePlanUseCase
import com.absinthe.libchecker.domain.snapshot.list.usecase.BuildSnapshotItemDisplayDataUseCase
import com.absinthe.libchecker.domain.snapshot.list.usecase.BuildSnapshotListUpdatePlanUseCase
import com.absinthe.libchecker.domain.snapshot.list.usecase.BuildSnapshotSystemPropDisplayDataUseCase
import com.absinthe.libchecker.domain.snapshot.list.usecase.BuildSnapshotTimeNodeListDataUseCase
import com.absinthe.libchecker.domain.snapshot.list.usecase.DeleteSnapshotTimeStampUseCase
import com.absinthe.libchecker.domain.snapshot.list.usecase.GetSnapshotPackageIconSourcesUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val snapshotListModule = module {
  factory { CaptureInstalledSnapshotUseCase(androidContext().packageManager, get(), get(), get(), get(), get()) }
  factory { SnapshotDetailSectionBuilder(androidContext(), get()) }
  factory { BuildSnapshotTitleDisplayDataUseCase(androidContext()) }
  factory { BuildSnapshotCapturePlanUseCase(get()) }
  factory { BuildSnapshotItemDisplayDataUseCase(androidContext()) }
  factory { BuildSnapshotListUpdatePlanUseCase(get(), get(), get()) }
  factory { GetSnapshotPackageIconSourcesUseCase(get()) }
  factory { BuildSnapshotSystemPropDisplayDataUseCase(androidContext(), get()) }
  factory { BuildSnapshotTimeNodeListDataUseCase(get(), get<FormatSnapshotTimestampUseCase>()::invoke) }
  factory { DeleteSnapshotTimeStampUseCase(get(), get()) }
  factory {
    SnapshotListWorkflow(
      repository = get(),
      appListRepository = get(),
      compareSnapshotDiffs = get(),
      compareSnapshotItemWithInstalledApp = get(),
      snapshotDashboardCounter = get(),
      snapshotDetailSectionBuilder = get(),
      snapshotLibrary = get(),
      buildSnapshotCapturePlanUseCase = get(),
      getSnapshotPackageIconSourcesUseCase = get(),
      buildSnapshotListUpdatePlanUseCase = get(),
      buildSnapshotSystemPropDisplayDataUseCase = get(),
      buildSnapshotTimeNodeListDataUseCase = get(),
      deleteSnapshotTimeStampUseCase = get(),
      formatSnapshotTimestampUseCase = get(),
      snapshotSelection = get(),
      snapshotSettingsRepository = get(),
      updateSnapshotAutoRemoveThresholdUseCase = get(),
      snapshotTrackChangeRepository = get()
    )
  }

  viewModel {
    SnapshotViewModel(
      snapshotListWorkflow = get()
    )
  }
}
