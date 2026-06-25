package com.absinthe.libchecker.domain.snapshot

import android.net.Uri
import java.io.File

class RestoreSnapshotDatabaseBackupUseCase(
  private val prepareRoomBackupRestoreFile: PrepareRoomBackupRestoreFileUseCase,
  private val databaseBackupRestorer: SnapshotDatabaseBackupRestorer,
  private val onSuccessfulRestore: () -> Unit
) {

  suspend operator fun invoke(
    uri: Uri,
    restoreFile: File
  ): SnapshotDatabaseBackupRestoreResult? {
    val preparedRestoreFile = prepareRoomBackupRestoreFile(uri, restoreFile) ?: return null
    val result = databaseBackupRestorer.restore(preparedRestoreFile)
    if (result.success) {
      preparedRestoreFile.delete()
      onSuccessfulRestore()
    }
    return result
  }
}
