package com.absinthe.libchecker.di

import com.absinthe.libchecker.domain.snapshot.display.BuildSnapshotAbiDisplayDataUseCase
import com.absinthe.libchecker.domain.snapshot.display.BuildSnapshotUpdateTimeDisplayDataUseCase
import com.absinthe.libchecker.domain.snapshot.display.FormatSnapshotTimestampUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val snapshotDisplayModule = module {
  factory { BuildSnapshotAbiDisplayDataUseCase(androidContext()) }
  factory { BuildSnapshotUpdateTimeDisplayDataUseCase() }
  factory { FormatSnapshotTimestampUseCase() }
}
