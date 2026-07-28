package com.absinthe.libchecker.domain.snapshot.backup.usecase

import com.absinthe.libchecker.domain.snapshot.backup.repository.SnapshotDatabaseBackupExportResult
import com.absinthe.libchecker.domain.snapshot.backup.repository.SnapshotDatabaseBackupExporter

class CreateSnapshotDatabaseBackupUseCase(
  private val databaseBackupExporter: SnapshotDatabaseBackupExporter
) {

  operator fun invoke(
    onComplete: (SnapshotDatabaseBackupExportResult) -> Unit
  ) {
    databaseBackupExporter.export(
      fileName = buildSnapshotBackupFileName("sqlite3"),
      onComplete = onComplete
    )
  }
}
