package com.absinthe.libchecker.di

import com.absinthe.libchecker.domain.snapshot.timenode.usecase.UpdateSnapshotAutoRemoveThresholdUseCase
import org.koin.dsl.module

val snapshotTimeNodeModule = module {
  factory { UpdateSnapshotAutoRemoveThresholdUseCase(get(), get()) }
}
