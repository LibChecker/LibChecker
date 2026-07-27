package com.absinthe.libchecker.domain.snapshot.backup.usecase

import com.absinthe.libchecker.domain.snapshot.SnapshotRepository
import com.absinthe.libchecker.domain.snapshot.backup.model.SnapshotBackupTarget
import com.absinthe.libchecker.domain.snapshot.backup.repository.SnapshotDatabaseFileRepository

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
      SnapshotBackupTarget.Archive(buildSnapshotBackupFileName("lcss"))
    }
  }

  private companion object {
    private const val LARGE_DATABASE_THRESHOLD_BYTES = 100L * 1024 * 1024
  }
}
