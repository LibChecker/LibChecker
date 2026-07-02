package com.absinthe.libchecker.domain.snapshot.backup.usecase

import com.absinthe.libchecker.domain.snapshot.SnapshotRepository
import com.absinthe.libchecker.domain.snapshot.backup.model.SnapshotBackupTarget
import com.absinthe.libchecker.domain.snapshot.backup.repository.SnapshotDatabaseFileRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GetSnapshotBackupTargetUseCase(
  private val databaseFileRepository: SnapshotDatabaseFileRepository,
  private val snapshotRepository: SnapshotRepository
) {

  suspend operator fun invoke(): SnapshotBackupTarget {
    if (snapshotRepository.getTimeStamps().isEmpty()) {
      return SnapshotBackupTarget.Empty
    }

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
