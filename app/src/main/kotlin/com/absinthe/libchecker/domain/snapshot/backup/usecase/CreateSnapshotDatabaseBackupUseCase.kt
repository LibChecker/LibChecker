package com.absinthe.libchecker.domain.snapshot.backup.usecase

import com.absinthe.libchecker.domain.snapshot.backup.repository.SnapshotDatabaseBackupExportResult
import com.absinthe.libchecker.domain.snapshot.backup.repository.SnapshotDatabaseBackupExporter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CreateSnapshotDatabaseBackupUseCase(
  private val databaseBackupExporter: SnapshotDatabaseBackupExporter
) {

  operator fun invoke(
    onComplete: (SnapshotDatabaseBackupExportResult) -> Unit
  ) {
    databaseBackupExporter.export(
      fileName = buildFileName(),
      onComplete = onComplete
    )
  }

  private fun buildFileName(): String {
    val formatted = SimpleDateFormat(
      "yyyy-MM-dd-HH-mm-ss",
      Locale.getDefault()
    ).format(Date())
    return "LibChecker-Snapshot-Backups-$formatted.sqlite3"
  }
}
