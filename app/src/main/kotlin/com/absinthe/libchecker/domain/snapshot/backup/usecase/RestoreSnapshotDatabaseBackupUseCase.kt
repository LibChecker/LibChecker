package com.absinthe.libchecker.domain.snapshot.backup.usecase

import android.net.Uri
import com.absinthe.libchecker.domain.snapshot.backup.repository.SnapshotDatabaseBackupRestoreResult
import com.absinthe.libchecker.domain.snapshot.backup.repository.SnapshotDatabaseBackupRestorer
import java.io.File

class RestoreSnapshotDatabaseBackupUseCase(
  private val prepareRoomBackupRestoreFile: PrepareRoomBackupRestoreFileUseCase,
  private val databaseBackupRestorer: SnapshotDatabaseBackupRestorer,
  private val onSuccessfulRestore: () -> Unit
) {

  suspend operator fun invoke(
    uri: Uri,
    cacheDir: File
  ): SnapshotDatabaseBackupRestoreResult? {
    val restoreFile = File(cacheDir, RESTORE_FILE_NAME)
    val preparedRestoreFile = prepareRoomBackupRestoreFile(uri, restoreFile) ?: return null
    val result = databaseBackupRestorer.restore(preparedRestoreFile)
    if (result.success) {
      preparedRestoreFile.delete()
      onSuccessfulRestore()
    }
    return result
  }

  private companion object {
    private const val RESTORE_FILE_NAME = "restore.sqlite3"
  }
}
