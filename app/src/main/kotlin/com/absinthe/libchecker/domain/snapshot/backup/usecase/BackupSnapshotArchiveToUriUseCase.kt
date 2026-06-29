package com.absinthe.libchecker.domain.snapshot.backup.usecase

import android.content.ContentResolver
import android.net.Uri
import com.absinthe.libchecker.domain.snapshot.backup.archive.SnapshotArchiveUseCase

class BackupSnapshotArchiveToUriUseCase(
  private val contentResolver: ContentResolver,
  private val snapshotArchive: SnapshotArchiveUseCase
) {

  suspend operator fun invoke(uri: Uri): SnapshotArchiveBackupResult {
    val outputStream = contentResolver.openOutputStream(uri, WRITE_TRUNCATE_MODE)
      ?: return SnapshotArchiveBackupResult.Failed

    val itemCount = runCatching {
      snapshotArchive.backup(outputStream)
    }.onFailure {
      deleteDocument(uri)
    }.getOrThrow()

    return if (itemCount > 0) {
      SnapshotArchiveBackupResult.Success(itemCount)
    } else {
      deleteDocument(uri)
      SnapshotArchiveBackupResult.Empty
    }
  }

  private fun deleteDocument(uri: Uri) {
    runCatching {
      contentResolver.delete(uri, null, null)
    }
  }

  private companion object {
    const val WRITE_TRUNCATE_MODE = "wt"
  }
}

sealed interface SnapshotArchiveBackupResult {
  data class Success(val itemCount: Int) : SnapshotArchiveBackupResult
  data object Empty : SnapshotArchiveBackupResult
  data object Failed : SnapshotArchiveBackupResult
}
