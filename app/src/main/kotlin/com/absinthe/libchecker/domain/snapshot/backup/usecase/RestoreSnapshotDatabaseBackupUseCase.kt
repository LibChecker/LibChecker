package com.absinthe.libchecker.domain.snapshot.backup.usecase

import android.content.ContentResolver
import android.net.Uri
import com.absinthe.libchecker.domain.snapshot.backup.repository.SnapshotDatabaseBackupRestoreResult
import com.absinthe.libchecker.domain.snapshot.backup.repository.SnapshotDatabaseBackupRestorer
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.buffer
import okio.sink
import okio.source

class RestoreSnapshotDatabaseBackupUseCase(
  private val contentResolver: ContentResolver,
  private val databaseBackupRestorer: SnapshotDatabaseBackupRestorer,
  private val onSuccessfulRestore: () -> Unit
) {

  suspend operator fun invoke(
    uri: Uri,
    cacheDir: File
  ): SnapshotDatabaseBackupRestoreResult? {
    val restoreFile = File(cacheDir, RESTORE_FILE_NAME)
    if (!copyRestoreFile(uri, restoreFile)) return null
    val result = databaseBackupRestorer.restore(restoreFile)
    if (result.success) {
      restoreFile.delete()
      onSuccessfulRestore()
    }
    return result
  }

  private suspend fun copyRestoreFile(uri: Uri, restoreFile: File): Boolean = withContext(Dispatchers.IO) {
    contentResolver.openInputStream(uri)?.let { inputStream ->
      inputStream.source().buffer().use { source ->
        restoreFile.outputStream().sink().buffer().use { sink ->
          source.readAll(sink)
        }
      }
      true
    } ?: false
  }

  private companion object {
    private const val RESTORE_FILE_NAME = "restore.sqlite3"
  }
}
