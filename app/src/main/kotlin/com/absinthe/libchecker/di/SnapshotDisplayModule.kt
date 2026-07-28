package com.absinthe.libchecker.di

import com.absinthe.libchecker.domain.snapshot.display.FormatSnapshotTimestampUseCase
import org.koin.dsl.module

val snapshotDisplayModule = module {
  factory { FormatSnapshotTimestampUseCase() }
}
