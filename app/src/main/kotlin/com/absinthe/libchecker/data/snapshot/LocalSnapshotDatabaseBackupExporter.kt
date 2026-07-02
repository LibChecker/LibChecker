package com.absinthe.libchecker.data.snapshot

import androidx.room3.RoomDatabase
import com.absinthe.libchecker.database.backup.RoomBackup
import com.absinthe.libchecker.domain.snapshot.backup.repository.SnapshotDatabaseBackupExportResult
import com.absinthe.libchecker.domain.snapshot.backup.repository.SnapshotDatabaseBackupExporter

class LocalSnapshotDatabaseBackupExporter(
  private val roomBackup: RoomBackup,
  private val databaseName: String,
  private val database: () -> RoomDatabase
) : SnapshotDatabaseBackupExporter {

  override fun export(
    fileName: String,
    onComplete: (SnapshotDatabaseBackupExportResult) -> Unit
  ) {
    roomBackup
      .database(database(), databaseName)
      .enableLogDebug(true)
      .backupLocation(RoomBackup.BACKUP_FILE_LOCATION_CUSTOM_DIALOG)
      .customBackupFileName(fileName)
      .maxFileCount(5)
      .onCompleteListener { success, message, exitCode ->
        onComplete(
          SnapshotDatabaseBackupExportResult(
            success = success,
            message = message,
            exitCode = exitCode
          )
        )
      }
      .backup()
  }
}
