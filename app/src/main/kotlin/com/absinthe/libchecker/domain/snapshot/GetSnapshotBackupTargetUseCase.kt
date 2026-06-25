package com.absinthe.libchecker.domain.snapshot

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GetSnapshotBackupTargetUseCase(
  private val databaseFileRepository: SnapshotDatabaseFileRepository
) {

  operator fun invoke(): SnapshotBackupTarget {
    return if (databaseFileRepository.getDatabaseSizeBytes() > LARGE_DATABASE_THRESHOLD_BYTES) {
      SnapshotBackupTarget.Database
    } else {
      SnapshotBackupTarget.Archive(buildArchiveFileName())
    }
  }

  private fun buildArchiveFileName(): String {
    val formatted = SimpleDateFormat(
      "yyyy-MM-dd-HH-mm-ss",
      Locale.getDefault()
    ).format(Date())
    return "LibChecker-Snapshot-Backups-$formatted.lcss"
  }

  private companion object {
    private const val LARGE_DATABASE_THRESHOLD_BYTES = 100L * 1024 * 1024
  }
}

sealed interface SnapshotBackupTarget {
  data class Archive(val fileName: String) : SnapshotBackupTarget
  data object Database : SnapshotBackupTarget
}
