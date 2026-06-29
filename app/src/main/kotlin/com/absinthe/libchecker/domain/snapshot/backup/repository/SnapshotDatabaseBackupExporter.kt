package com.absinthe.libchecker.domain.snapshot.backup.repository

interface SnapshotDatabaseBackupExporter {

  fun export(
    fileName: String,
    onComplete: (SnapshotDatabaseBackupExportResult) -> Unit
  )
}

data class SnapshotDatabaseBackupExportResult(
  val success: Boolean,
  val message: String,
  val exitCode: Int
)
