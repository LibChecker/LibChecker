package com.absinthe.libchecker.di

import com.absinthe.libchecker.domain.snapshot.timenode.usecase.BuildSnapshotTimeNodeItemsUseCase
import com.absinthe.libchecker.domain.snapshot.timenode.usecase.UpdateSnapshotAutoRemoveThresholdUseCase
import org.koin.dsl.module

val snapshotTimeNodeModule = module {
  factory { BuildSnapshotTimeNodeItemsUseCase() }
  factory { UpdateSnapshotAutoRemoveThresholdUseCase(get(), get()) }
}
