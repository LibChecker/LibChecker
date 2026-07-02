package com.absinthe.libchecker.di

import com.absinthe.libchecker.LibCheckerApp
import com.absinthe.libchecker.constant.OnceTag
import com.absinthe.libchecker.data.snapshot.LocalSnapshotDatabaseBackupExporter
import com.absinthe.libchecker.data.snapshot.LocalSnapshotDatabaseBackupRestorer
import com.absinthe.libchecker.data.snapshot.LocalSnapshotDatabaseFileRepository
import com.absinthe.libchecker.data.snapshot.ProtoSnapshotArchiveCodec
import com.absinthe.libchecker.database.LCDatabase
import com.absinthe.libchecker.database.backup.RoomBackup
import com.absinthe.libchecker.domain.snapshot.backup.archive.SnapshotArchiveCodec
import com.absinthe.libchecker.domain.snapshot.backup.archive.SnapshotArchiveUseCase
import com.absinthe.libchecker.domain.snapshot.backup.presentation.SnapshotBackupViewModel
import com.absinthe.libchecker.domain.snapshot.backup.repository.SnapshotDatabaseFileRepository
import com.absinthe.libchecker.domain.snapshot.backup.usecase.BackupSnapshotArchiveToUriUseCase
import com.absinthe.libchecker.domain.snapshot.backup.usecase.BuildSnapshotRestorePlanUseCase
import com.absinthe.libchecker.domain.snapshot.backup.usecase.CreateSnapshotDatabaseBackupUseCase
import com.absinthe.libchecker.domain.snapshot.backup.usecase.GetSnapshotBackupTargetUseCase
import com.absinthe.libchecker.domain.snapshot.backup.usecase.PrepareRoomBackupRestoreFileUseCase
import com.absinthe.libchecker.domain.snapshot.backup.usecase.RestoreSnapshotArchiveFromUriUseCase
import com.absinthe.libchecker.domain.snapshot.backup.usecase.RestoreSnapshotDatabaseBackupUseCase
import com.jakewharton.processphoenix.ProcessPhoenix
import jonathanfinerty.once.Once
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

val snapshotBackupModule = module {
  single<SnapshotDatabaseFileRepository> { LocalSnapshotDatabaseFileRepository() }
  single<SnapshotArchiveCodec> { ProtoSnapshotArchiveCodec() }

  factory { SnapshotArchiveUseCase(get(), get()) }
  factory { BuildSnapshotRestorePlanUseCase(androidContext().contentResolver) }
  factory { GetSnapshotBackupTargetUseCase(get(), get()) }
  factory { BackupSnapshotArchiveToUriUseCase(androidContext().contentResolver, get()) }
  factory { (roomBackup: RoomBackup) ->
    CreateSnapshotDatabaseBackupUseCase(
      databaseBackupExporter = LocalSnapshotDatabaseBackupExporter(
        roomBackup = roomBackup,
        databaseName = LCDatabase.DATABASE_NAME,
        database = { LCDatabase.getDatabase() }
      )
    )
  }
  factory { PrepareRoomBackupRestoreFileUseCase(androidContext().contentResolver) }
  factory { (roomBackup: RoomBackup) ->
    RestoreSnapshotDatabaseBackupUseCase(
      prepareRoomBackupRestoreFile = get(),
      databaseBackupRestorer = LocalSnapshotDatabaseBackupRestorer(
        roomBackup = roomBackup,
        databaseName = LCDatabase.DATABASE_NAME,
        database = { LCDatabase.getDatabase() }
      ),
      onSuccessfulRestore = {
        Once.clearDone(OnceTag.FIRST_LAUNCH)
        ProcessPhoenix.triggerRebirth(LibCheckerApp.app)
      }
    )
  }
  factory { RestoreSnapshotArchiveFromUriUseCase(androidContext().contentResolver, get()) }

  viewModel {
    SnapshotBackupViewModel(
      backupSnapshotArchiveToUriUseCase = get(),
      restoreSnapshotArchiveFromUriUseCase = get(),
      getSnapshotBackupTargetUseCase = get(),
      buildSnapshotRestorePlanUseCase = get(),
      createSnapshotDatabaseBackupUseCaseFactory = { roomBackup ->
        get {
          parametersOf(roomBackup)
        }
      },
      restoreSnapshotDatabaseBackupUseCaseFactory = { roomBackup ->
        get {
          parametersOf(roomBackup)
        }
      },
      formatSnapshotTimestampUseCase = get(),
      snapshotSelection = get()
    )
  }
}
