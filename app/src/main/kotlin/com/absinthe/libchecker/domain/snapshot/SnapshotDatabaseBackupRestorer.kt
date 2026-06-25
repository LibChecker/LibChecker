package com.absinthe.libchecker.domain.snapshot

import java.io.File

interface SnapshotDatabaseBackupRestorer {

  suspend fun restore(backupFile: File): SnapshotDatabaseBackupRestoreResult
}

data class SnapshotDatabaseBackupRestoreResult(
  val success: Boolean,
  val message: String,
  val exitCode: Int
)
